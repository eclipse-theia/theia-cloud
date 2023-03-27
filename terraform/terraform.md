# Terraform

We use [Terraform](https://www.terraform.io/) to provide configuration files for trying out Theia Cloud in multiple environments.

## Install Terraform

Please follow the official installation instruction at <https://developer.hashicorp.com/terraform/downloads?product_intent=terraform>.

If you are unfamiliar with Terraform you may want to have a look at their tutorials avilable at <https://developer.hashicorp.com/terraform/tutorials>.

## Directory Structure

The `modules` directory contains our reuseable terraform modules for creating clusters, installing dependencies via helm, and configuring keycloak. The modules will be used by the actual terraform configurations available in the `configurations` directory. 

## Available Configurations

### Minikube

`./configurations/minikube_getting_started` may be used to create a Theia Cloud cluster in minikube.

Please check the variables passed to the modules in `minikube_getting_started.tf` for possible modifications.

#### Prerequisites

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
terraform apply
```

Point for browser to `https://${minikube ip}.nip.io/trynow/`
