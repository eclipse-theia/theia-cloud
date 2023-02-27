# Getting started with Minikube

Minikube is a local Kubernetes that allows you to test and develop for Kubernetes.\
In this guide we will show you how to install minikube and helm 3 as well as cert-manager, the cloud native certificate management, the NginX Ingress Controller and Keycloak. We will use existing installation methods for all of the Theia.Cloud preqrequisites.\
Finally we will install and try Theia.Cloud using helm.

If you have no experience with Kubernetes/Minikube yet, we encourage you to check out some basic tutorials first, e.g. https://kubernetes.io/docs/tutorials/hello-minikube/ and https://kubernetes.io/docs/tutorials/kubernetes-basics/

## Check out the latest tag

Rather than using the latest changes on the `main` branch, please **check out the latest tag**: https://github.com/eclipsesource/theia-cloud/tags

For every tag the checked in helm chart and the built Dockerimages will work together as expected. 

The helm chart on main works for yet unreleased images based on the latest changes. 

## Install and start minikube

If minikube is not installed on your system, go to https://minikube.sigs.k8s.io/docs/start/ and follow the instructions in Step 1 Installation.

Create a new Minikube cluster using the `minikube start` command.\
You may adjust the available RAM and CPU-cores depending on your system.\
`minikube start --addons=ingress --vm=true --memory=8192 --cpus=4 --disk-size=50g`

**Please note:** Minikube has some issues with persisted volumes and permissions. This will only be a problem in this demo, if you want to create files inside the `"./persisted/"` directory available in the demo workspace.\
If you want to try the persisted storage, please mount a directory from your hostsystem into minikube like follows.\
You may have to adjust the firewall rules on your system.

```bash
# create the directory that will be mounted
mkdir ~/tmp/minikube

# mount into minikube with expected ids
minikube mount ~/tmp/minikube:/tmp/hostpath-provisioner/theia-cloud --uid 101 --gid 101
```

## Install Prerequisites

Theia.cloud depends on some prerequisite applications to be available on your system or available in the Cluster.

### Helm 3

Follow the steps in https://helm.sh/docs/intro/install/ to install Helm on your system.\
Helm is a package manager for kubernetes which is required to install some of the prerequisites and Theia.Cloud itself.

### Cert-Manager

Please check https://cert-manager.io/docs/installation/ for the latest installation instructions.

As of writing this guide the installation command looks like this:\
`kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.11.0/cert-manager.yaml`

Cert-Manager is used to manage certificates für https.\
Theia.Cloud offers support for Let's Encrypt Certificates out of the box, but cert-manager may also be used to manage your certificates.

### Metric Server

Metrics Server is used to get usage information of pods.

On Minikube you may use `minikube addons enable metrics-server` to enable the metrics server.

### NginX Ingress Controller

This is installed in minikube already when started with the `--addons=ingress` argument, so nothing has to be installed for the Minikube Guide.

If you want to run the Minikube-Demo path on an actual cluster, please see https://kubernetes.github.io/ingress-nginx/deploy/ on how to install the controller.

### Global certificate - ONLY when using paths

If Theia Cloud is used with paths instead of subdomains, the global HTTPS certificate should exist and be configured as the NginX Ingress Controller's default certificate.

In [global-certificate.yaml](./platforms/global-certificate.yaml), replace `example.com` with `$(minikube ip).nip.io`  and `letsencrypt-prod` with `theia-cloud-selfsigned-issuer`.
Make sure the annotation `cert-manager.io/common-name` is **not** commented out.
Apply the configuration with `kubectl apply -f  ./doc/docs/platforms/global-certificate.yaml`.

Configure the nginx controllers deployment to use the certificate `default-tls` in the default namespace as its default certificate.
For this, add argument `"--default-ssl-certificate=default/default-tls"` to the container by patching the controller's deployment.

```bash
kubectl patch deploy ingress-nginx-controller --type='json' -n ingress-nginx \
-p '[{ "op": "add", "path": "/spec/template/spec/containers/0/args/-", "value": "--default-ssl-certificate=default/default-tls" }]'
```

### Keycloak

Keycloak is the identify and access management tool used by Theia.Cloud.

#### Installation

For installation we will be using the helm chart provided by codecentric: https://github.com/codecentric/helm-charts/tree/master/charts/keycloak

Run the following command from the theia-cloud root repository. This uses the values from `./doc/docs/platforms/keycloak-minikube-values.yaml` and uses `keycloak.$(minikube ip).nip.io` as the hostname for keycloak.

```bash
# add the codecentric helm repository
helm repo add codecentric https://codecentric.github.io/helm-charts

# install keycloak.
helm install keycloak codecentric/keycloak --namespace keycloak --create-namespace --values ./doc/docs/platforms/keycloak-minikube-values.yaml --set "ingress.rules[0].host=keycloak.$(minikube ip).nip.io" --set "ingress.tls[0].hosts={keycloak.$(minikube ip).nip.io}"
```

#### Administration

In this step we will import a pre-created demo Realm into keycloak and create some test user.

Wait until keycloak is available at above hostname.

Go to the keycloak admin console and log in with admin - admin credentials.\
In the top left, hover over `Master` and selected `Add realm`.\
Import `doc/docs/platforms/realm-export.json` via `Select file` and click `Create`.\
Go to `Manage -> Users` in the left panel and select `Add user`.
Set

- Username, e.g. `foo`
- Email, e.g. `foo@theia-cloud.io`
- Email verified to On

Select `Save` and go to `Credentials` Tab.
Set

- Password
- Password Confirmation
- Temporary to OFF

Select `Set Password`

Add more users if you want to.

## Install Theia.Cloud

We use the Minikube IP for our hostnames as we did for keycloak already.

```bash
# Add the Theia Cloud Helm repository
helm repo add theia-cloud-remote https://github.eclipsesource.com/theia-cloud-helm
helm repo update

# --devel flag allows to see pre release versions of helm charts

# Install the cluster wide Theia Cloud resources
helm install theia-cloud-base theia-cloud-remote/theia-cloud-base --devel


# for other platforms determine the external nginx ingress controller ip with
# kubectl get services ingress-nginx-controller -n ingress-nginx'
# If you don't use Minikube or your shell does not support $(), replace $(minikube ip) with the IP you determined above

# Install Theia Cloud
helm install theia-cloud theia-cloud-remote/theia-cloud  --devel --namespace theiacloud --create-namespace -f ./helm/theia.cloud/valuesMinikube.yaml --set hosts.service=service.$(minikube ip).nip.io --set hosts.landing=theia.cloud.$(minikube ip).nip.io --set hosts.instance=ws.$(minikube ip).nip.io --set keycloak.authUrl=https://keycloak.$(minikube ip).nip.io/auth/ --set hosts.paths.baseHost=$(minikube ip).nip.io

# Optional: switch to the newly created namespace
kubectl config set-context --current --namespace=theiacloud
```

For more information about the possible helm values, see the comments in the [values.yaml](../../../helm/theia.cloud/values.yaml).

### Accept Self Signed Certificates

Since we are running in Minikube on our local machine, which is not exposed to the internet, we cannot use letsencrypt certifcates.

Browser to below URLs (update the ip to `minikube ip` accordingly) and accept the temporary certificate.

In Chrome:
`Your connection is not private -> Advanced -> Proceed to ...`

In Firefox:
`Warning: Potential Security Risk Ahead -> Advanced... -> Accept the Risk and Continue`

https://service.192.168.39.81.nip.io \
https://ws.192.168.39.81.nip.io \
https://keycloak.192.168.39.81.nip.io/

### Launch Theia using Theia.Cloud

At last, go to

https://theia.cloud.192.168.39.81.nip.io

and accept the certificate. This will bring you to the Keycloak login page and will start up a session for your user after login.\
The very first launch may take a bit longer, since the docker image for the session has to be pulled.

### Uninstall Theia.Cloud

Simply run `helm uninstall theia-cloud -n theiacloud`

## Testing local Theia.cloud images

You can test locally build images (e.g. of the landing page, service, or operator) by building them in Minikube and then using them in the Minikube Helm chart.

To build images directly in Minikube, execute `eval $(minikube docker-env)` in a terminal.
With this, the `docker` command in this terminal runs inside Minikube.

Build the docker image as usual.

Adapt [valuesMinikube.yaml](../../../helm/theia.cloud/valuesMinikube.yaml):

- Adapt the `image` value to match your built image.
- Specify `imagePullPolicy: Never` in the helm-template (e.g. for the landing page this would be in `helm/theia.cloud/templates/landing-page.yaml`) to prevent Kubernetes from trying to download the image.
