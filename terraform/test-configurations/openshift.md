# OpenShift Local Development Setup

This guide describes how to set up a local OpenShift cluster using [Red Hat OpenShift Local](https://console.redhat.com/openshift/create/local) for testing Theia Cloud with OpenShift Route support.

OpenShift Local is the local OpenShift equivalent of minikube. Unlike minikube, there is no Terraform provider for OpenShift Local, the cluster is managed via the `crc` CLI. The `0_openshift-setup` terraform configuration reads connection details from the running cluster and outputs them for downstream terraform steps.

## Prerequisites

* **OS**: Ubuntu 24.04, or another supported Linux distribution
* **virtiofsd**: Required by CRC on Ubuntu, install via `sudo apt install virtiofsd`
* **OpenShift Local**: Download and install from [https://console.redhat.com/openshift/create/local](https://console.redhat.com/openshift/create/local)
* **Pull secret**: Obtain from [https://console.redhat.com/openshift/create/local](https://console.redhat.com/openshift/create/local), requires a free Red Hat account
* **Minimum resources**: 4 CPUs, 12 GiB RAM, 50 GiB disk, more than the OpenShift Local defaults to run Theia Cloud

## OpenShift Local Setup

```bash
# Download and install OpenShift Local following the instructions at:
# https://console.redhat.com/openshift/create/local

# Run the setup, installs required system components
crc setup

# Configure resources, higher than OpenShift Local defaults for Theia Cloud
crc config set cpus 4
crc config set memory 12288    # 12 GiB
crc config set disk-size 50    # 50 GiB

# Start the cluster, first start downloads the OpenShift VM image
crc start
```

## Post Start Configuration

```bash
# Set up oc CLI access
eval $(crc oc-env)

# Get the login credentials (CRC generates a random kubeadmin password)
crc console --credentials

# Log in as cluster admin using the password from above
oc login -u kubeadmin -p <password> https://api.crc.testing:6443

# Verify cluster is running
oc get nodes
oc get routes -A   # should show OpenShift console routes

# Get the apps domain, needed for Theia Cloud Route hostnames
# OpenShift Local uses: apps-crc.testing
```

## Key Differences from Minikube

* No external DNS needed, OpenShift Local configures a local DNS resolver, routes are accessible from the host at `*.apps-crc.testing`
* No ingress controller needed, OpenShift has a built in HAProxy based router that handles routes natively
* No cert manager needed for initial setup, OpenShift Local comes with a self signed CA, routes can use `tls.termination: edge` with the default router certificate, or start without TLS
* Token based authentication, OpenShift uses token based auth instead of client certificates

## Hostname Pattern

OpenShift Local exposes routes at `*.apps-crc.testing`. Theia Cloud routes will be:

| Component    | Hostname                          |
| ------------ | --------------------------------- |
| Landing page | `try.apps-crc.testing`            |
| Service      | `service.apps-crc.testing`        |
| Sessions     | `ws-<sessionid>.apps-crc.testing` |

## Step 0, OpenShift Setup, Terraform

After starting OpenShift Local, configure the terraform state that downstream steps depend on:

```bash
cd terraform/test-configurations/0_openshift-setup

# Get your login token
oc login -u kubeadmin https://api.crc.testing:6443
TOKEN=$(oc whoami -t)

# Initialize and apply
terraform init
terraform apply -var="openshift_token=$TOKEN"
```

The configuration accepts the following variables:

| Variable           | Default                        | Description                         |
| ------------------ | ------------------------------ | ----------------------------------- |
| `openshift_server` | `https://api.crc.testing:6443` | OpenShift API server URL            |
| `openshift_token`  | required                       | Login token, get via `oc whoami -t` |
| `apps_domain`      | `apps-crc.testing`             | Apps domain for route hostnames     |

## Step 4-01, Deploy Theia Cloud on OpenShift

```bash
cd terraform/test-configurations/4-01_openshift_monitor

terraform init
terraform apply
```

This installs:

* `theia-cloud-crds`, Custom Resource Definitions
* `theia-cloud-base`, base RBAC and cluster roles
* `theia-cloud`, operator, landing page, and service with OpenShift route configuration

## Verification

After deployment, verify the installation:

```bash
# Check routes are created, no ingress resources should exist
oc get routes -n theia-cloud
oc get ingress -n theia-cloud

# Check the session ServiceAccount and SCC RoleBinding
oc get sa theia-cloud-sessions -n theia-cloud
oc get rolebindings theia-cloud-sessions-anyuid -n theia-cloud

# Access the landing page
curl -s -o /dev/null -w "%{http_code}" http://try.apps-crc.testing
```

## Teardown

```bash
# Destroy Theia Cloud installation, reverse order
cd terraform/test-configurations/4-01_openshift_monitor
terraform destroy

cd ../0_openshift-setup
terraform destroy

# Stop or delete the OpenShift Local cluster
crc stop          # pause the cluster, preserves state
crc delete        # remove the cluster entirely
```
