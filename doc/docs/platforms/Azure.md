# Getting started with Azure AKS

This document explains how to get started with installing Theia.Cloud on Azure.
It assumes that you have a subscription with Owner/Admin privileges.

## Install required tools

### Azure CLI

The Azure CLI is a command line tool to modify your Azure environment, e.g. creating clusters, docker registries, etc.
See <https://learn.microsoft.com/en-us/cli/azure/install-azure-cli> to install the Azure CLI on your system.

[Login to Azure CLI](https://learn.microsoft.com/en-us/cli/azure/get-started-with-azure-cli) with

```bash
az login
```

Set the subscription you want to use.
See <https://learn.microsoft.com/en-us/cli/azure/manage-azure-subscriptions-azure-cli> on managing Azure subscriptions.

### Kubectl

Kubectl is used to manage Kubernetes clusters.
See the official Kubernetes documentation on how to install it on your system: <https://kubernetes.io/docs/tasks/tools/#kubectl>

Alternatively, you can use the Azure CLI to install Kubectl for you with [az aks install-cli](https://learn.microsoft.com/en-us/cli/azure/aks?view=azure-cli-latest#az-aks-install-cli).

### Helm 3

Follow the steps at <https://helm.sh/docs/intro/install/> to install Helm on your system.\
Helm is a package manager for kubernetes which is required to install some of the prerequisites and Theia.Cloud itself.

## Setup the cluster

You should have an active Azure subscription and Owner (Admin) privileges on this subscription.

### Create a resource group

Create a new resource group for your deployment facilitating an easier overview and removal of all resources related to this deployment.
A group is created for a specific location in the world.

List available locations:

```bash
az account list-locations --output table
```

Create resource group with name `TheiaCloud` in western Europe:

```bash
az group create --location westeurope --name TheiaCloud
```

For additional information on creating resource groups see: <https://learn.microsoft.com/en-us/cli/azure/group?view=azure-cli-latest#az-group-create>

### Create a basic cluster

Create an Azure Kubernetes Service (AKS) which is a managed Kubernetes cluster.

The cluster contains 1 to 2 nodes with 4vCPUs and 16GB of RAM each.
This is VM size `Standard_D4s_v3`.

The `--ssh-key-value` parameter specifies a public SSH key that is installed on all nodes to allow SSH access.
Alternatively, parameter `--generate-ssh-keys` can be used to generate a new pair of SSH keys.
For more information on cluster creation parameters see: <https://learn.microsoft.com/en-us/cli/azure/aks?view=azure-cli-latest#az-aks-create>

```bash
az aks create \
    --resource-group TheiaCloud \
    --name TheiaCloudCluster \
    --node-count 2 \
    --ssh-key-value <path-or-pub-key> \
    --enable-cluster-autoscaler \
    --min-count 1 \
    --max-count 2 \
    --node-vm-size Standard_D4s_v3
```

### Configure Kubectl to access the cluster

Get the cluster credentials with

```bash
az aks get-credentials --resource-group TheiaCloud --name TheiaCloudCluster
```

This adds the credentials to the default Kubectl config file.
You can specify a custom file by providing the `--file` parameter.

For more information see: <https://learn.microsoft.com/en-us/azure/aks/learn/quick-kubernetes-deploy-cli#connect-to-the-cluster>

### Optional: Create and link a private Docker registry

To use custom images without exposing them publicly, you can create a private Docker registry and use its images in the deployment.

Create registry in the same resource group as the cluster.
Note that the name of the registry must be unique within Azure.

```bash
az acr create --resource-group TheiaCloud --name mycontainerregistry --sku Basic
```

Next, we attach the registry to the cluster.
This enables pulling images from the registry into the cluster without specifying any explicit pull secret.

```bash
az aks update --resource-group TheiaCloud --name TheiaCloudCluster --attach-acr mycontainerregistry
```

Documentation on how to login to the registry and push images to it can be found here:
<https://learn.microsoft.com/en-us/azure/container-registry/container-registry-get-started-azure-cli?source=recommendations#log-in-to-registry>.

Short summary:

```bash
# Login to registry
az acr login --name mycontainerregistry

# Tag image for your registry
docker tag myimage mycontainerregistry.azurecr.io/myimage:v1

# Push image
docker push mycontainerregistry.azurecr.io/myimage:v1
```

## Install prerequisites on the cluster

This section describes how to install cert manager, a nginx ingress controller and keycloak on the cluster.

### Cert manager

Please check <https://cert-manager.io/docs/installation/> for the latest installation instructions.

As of writing this guide the installation command looks like this:

```bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.10.0/cert-manager.yaml
```

Alternatively, cert-manager can be installed via Helm chart as documented at <https://cert-manager.io/docs/installation/helm/>.
As of writing this guide the installation command looks like this:

```bash
helm repo add jetstack https://charts.jetstack.io
helm repo update
helm install \
  cert-manager jetstack/cert-manager \
  --namespace cert-manager \
  --create-namespace \
  --version v1.10.0 \
  --set installCRDs=true
```

### NGINX ingress

Install the NGINX ingress controller via Helm.
The following command is based on the documentation at <https://learn.microsoft.com/en-us/azure/aks/ingress-basic?tabs=azure-cli#create-an-ingress-controller> which provides additional information.

```bash
# add nginx ingress helm repository
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx

# install
helm install ingress-nginx ingress-nginx/ingress-nginx \
--version 4.3.0 \
--namespace ingress-nginx \
--create-namespace \
--set controller.admissionWebhooks.patch.nodeSelector."kubernetes\.io/os"=linux \
--set controller.service.annotations."service\.beta\.kubernetes\.io/azure-load-balancer-health-probe-request-path"=/healthz \
--set controller.replicaCount=2 \
--set defaultBackend.nodeSelector."kubernetes\.io/os"=linux
```

Check that the nginx ingress controller is running and find the cluster's external IP address in column EXTERNAL-IP.
If the external IP is still pending, wait a bit and execute the command again.

```bash
kubectl get svc ingress-nginx-controller -n ingress-nginx
```

### Keycloak

[Keycloak](https://www.keycloak.org/) is the identify and access management tool used by Theia.Cloud.

Keycloak is also installed via Helm.
To configure the installation properly, we need the cluster's external IP.
Get it with:

```bash
kubectl get svc ingress-nginx-controller -n ingress-nginx -o jsonpath="{.status.loadBalancer.ingress[0].ip}"
```

For the installation via Helm, we assume that the cluster's IP address was set to environment variable `EXTERNAL`.
Instead, you can also just replace `${EXTERNAL}` with the IP.

**Note** that the [values file](./keycloak-azure-values.yaml) initializes an initial keycloak admin user. Its name is `admin` and its password `admin-password`.
You should change its password after the first login.

```bash
# add the codecentric helm repository
helm repo add codecentric https://codecentric.github.io/helm-charts

# install keycloak
helm install keycloak codecentric/keycloak \
--namespace keycloak --create-namespace \
--values ./doc/docs/platforms/keycloak-azure-values.yaml \
--set "ingress.rules[0].host=keycloak.${EXTERNAL}.nip.io" \
--set "ingress.tls[0].hosts={keycloak.${EXTERNAL}.nip.io}"
```

Add and configure a new realm for your Keycloak installation as described in [Keycloak.md](../Keycloak.md).

## Install Theia.Cloud

For this demo we will use nip.io hostnames.

Just as for the keycloak installation, we require the external IP address of our nginx ingress:

```bash
kubectl get svc ingress-nginx-controller -n ingress-nginx -o jsonpath="{.status.loadBalancer.ingress[0].ip}"
```

Open `./helm/theia.cloud/valuesAzure.yaml` and replace all occurrences of `<EXTERNAL-IP>` with the IP address obtained before.

```bash
helm install theia-cloud ./helm/theia.cloud --namespace theiacloud --create-namespace --values ./helm/theia.cloud/valuesAzure.yaml

# Optional: switch to the newly created namespace
kubectl config set-context --current --namespace=theiacloud
```

## Uninstall Theia.Cloud

```bash
helm uninstall theia-cloud -n theiacloud
```
