# Terraform

We use [Terraform](https://www.terraform.io/) to provide configuration files for trying out Theia Cloud in multiple environments.

## Install Terraform

Please follow the official installation instruction at <https://developer.hashicorp.com/terraform/downloads?product_intent=terraform>.

If you are unfamiliar with Terraform, you may want to have a look at their tutorials available at <https://developer.hashicorp.com/terraform/tutorials>.

## Directory Structure

The `modules` directory contains our reusable terraform modules for creating clusters, installing dependencies via helm, and configuring keycloak. The modules will be used by the actual terraform configurations available in the `configurations` directory.

If you can't use Terraform, the `./modules/helm/main.tf` contains the information which helm charts are installed from which helm repository and you may extract the passed values. For an initial Keycloak realm configuration, you may check the values in `./modules/keycloak/main.tf`.

## Theia Cloud Getting Started

Currently, we have Getting Started configurations for the following Clusters/Services:

* [Minikube](#minikube)
* [Google Kubernetes Engine](#google-kubernetes-engine)

The configurations will create a small cluster running Theia Cloud and all dependencies.\
We will create a Keycloak with two dummy users *foo* and *bar*. The password matches their user names.
The keycloak admin password may be passed during the installation. The defaults to "admin" for local installations while it has to be entered for remote installations.

### Minikube

`./configurations/minikube_getting_started` may be used to create a Theia Cloud cluster in minikube.

Please check the variables passed to the modules in `minikube_getting_started.tf` for possible modifications.

#### Prerequisites for Minikube

If minikube is not installed on your system, go to <https://minikube.sigs.k8s.io/docs/start/> and follow the instructions in Step 1 Installation.

By default we are using the VirtualBox driver since this is available on all OS. Please install VirtualBox: <https://www.virtualbox.org/wiki/Downloads>

Then download the minikube virtualbox driver before invoking terraform:

```bash
minikube start --vm=true --driver=virtualbox --download-only
```

#### Create Minikube Cluster

```bash
cd configurations/minikube_getting_started

# download required providers
terraform init

# dry run
terraform plan

# create the cluster
# You will be asked for an email address used by the cert-manager to contact you about expiring certs.
terraform apply
```

Point your browser to the `try_now` output value URL printed to the console at the end.

#### Destroy Minikube Cluster

First remove the persistent volume from the terraform state:

```bash
terraform state rm kubernetes_persistent_volume.minikube
```

Helm uninstall does not remove persistent volume claims, so the destruction of this persistent volume is blocked. The continue with a regular destroy:

```bash
terraform destroy
```

### Google Kubernetes Engine

`./configurations/gke_getting_started` may be used to create a Theia Cloud cluster on GKE.

Please check the variables passed to the module in `gke_getting_started` for possible modifications.

#### Prerequisites for GKE

We expect users to have basic experience with GKE: <https://cloud.google.com/kubernetes-engine>\
You may want to check their guides as a new user first: <https://cloud.google.com/kubernetes-engine/docs/deploy-app-cluster>

Please note that using our configurations may cause you cost depending ob whether the free credit provided for new GKE users was used up already.

Our modules expect you to have the Google Cloud SDK installed and set up: <https://cloud.google.com/sdk>

#### Create GKE Cluster

```bash
cd configurations/gke_getting_started

# download required providers
terraform init

# dry run
terraform plan

# create the cluster
# You will be asked for an email address used by the cert-manager to contact you about expiring certs.
# Furthermore you are asked for password for the Keycloak Admin User, the Postgres DB and the Postgres Admin User.
# Finally you have to pass the id of your Google Cloud Project in which the cluster will be created.
terraform apply
```

Point your browser to the `try_now` output value URL printed to the console at the end.

#### Destroy GKE Cluster

```bash
terraform destroy
```
