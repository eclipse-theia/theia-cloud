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

## Building and Pushing Custom Images

When developing locally, you can build custom Theia Cloud images and push them to the CRC internal image registry. This avoids the need for an external registry.

### Registry Overview

OpenShift Local ships with an internal image registry. It is exposed externally via a Route at `default-route-openshift-image-registry.apps-crc.testing` (uses a self-signed certificate). Inside the cluster, pods pull from `image-registry.openshift-image-registry.svc:5000`.

There are two addresses to keep in mind:

| Address | Usage |
| ------- | ----- |
| `default-route-openshift-image-registry.apps-crc.testing` | External, used to push images from the host |
| `image-registry.openshift-image-registry.svc:5000` | Internal, used by pods to pull images |

### Logging in to the Registry

The registry uses the CRC self-signed CA, so docker/podman needs to trust it or skip verification.

With podman:

```bash
oc login -u kubeadmin https://api.crc.testing:6443
podman login --tls-verify=false -u kubeadmin -p $(oc whoami -t) default-route-openshift-image-registry.apps-crc.testing
```

With docker, you need to trust the CRC CA certificate. The `insecure-registries` daemon option alone is not sufficient because Docker's OAuth token exchange still verifies TLS. Extract the CRC CA and install it as a system-trusted certificate:

```bash
# Extract the CRC ingress CA certificate
oc extract secret/router-ca --keys=tls.crt -n openshift-ingress-operator --confirm
sudo cp tls.crt /usr/local/share/ca-certificates/crc-ingress-ca.crt
sudo update-ca-certificates
sudo systemctl restart docker
```

Then log in:

```bash
docker login -u kubeadmin -p $(oc whoami -t) default-route-openshift-image-registry.apps-crc.testing
```

### Building and Pushing Images

Make sure you have logged in to the registry first (see [Logging in to the Registry](#logging-in-to-the-registry)).

The OpenShift internal registry requires the target namespace and ImageStreams to exist before you can push images. Create them if they do not exist yet:

```bash
oc new-project theia-cloud || true
oc create imagestream theia-cloud-operator -n theia-cloud
oc create imagestream theia-cloud-service -n theia-cloud
oc create imagestream theia-cloud-landing-page -n theia-cloud
```

All docker builds run from the `theia-cloud` repository root. The tag format is `default-route-openshift-image-registry.apps-crc.testing/<namespace>/<image>:<tag>`. Use the `theia-cloud` namespace to match the Helm deployment.

| Component    | Dockerfile                            | Build context | Helm value to override |
| ------------ | ------------------------------------- | ------------- | ---------------------- |
| Operator     | `dockerfiles/operator/Dockerfile`     | repo root (`.`) | `operator.image`     |
| Service      | `dockerfiles/service/Dockerfile`      | repo root (`.`) | `service.image`      |
| Landing Page | `dockerfiles/landing-page/Dockerfile` | repo root (`.`) | `landingPage.image`  |

All commands below assume you are in the `theia-cloud` repository root.

Operator:

```bash
EXT_REG=default-route-openshift-image-registry.apps-crc.testing

docker build -f dockerfiles/operator/Dockerfile -t $EXT_REG/theia-cloud/theia-cloud-operator:dev .
docker push $EXT_REG/theia-cloud/theia-cloud-operator:dev
```

Service:

```bash
docker build -f dockerfiles/service/Dockerfile -t $EXT_REG/theia-cloud/theia-cloud-service:dev .
docker push $EXT_REG/theia-cloud/theia-cloud-service:dev
```

Landing Page:

```bash
docker build -f dockerfiles/landing-page/Dockerfile -t $EXT_REG/theia-cloud/theia-cloud-landing-page:dev .
docker push $EXT_REG/theia-cloud/theia-cloud-landing-page:dev
```

### Overriding Images in the Helm Deployment

When deploying via terraform (`4-01_openshift_monitor/theia_cloud.tf`), add `set` blocks to override the image. Inside the cluster, use the internal registry address (not the external route):

```hcl
{
  name  = "operator.image"
  value = "image-registry.openshift-image-registry.svc:5000/theia-cloud/theia-cloud-operator:dev"
},
{
  name  = "operator.imagePullPolicy"
  value = "Always"
}
```

Set `imagePullPolicy` to `Always` during development so that new pushes with the same tag are picked up.

See the commented-out examples in `4-01_openshift_monitor/theia_cloud.tf` for all three images.

### Verifying the Image Was Pushed

```bash
# List images in the theia-cloud namespace
oc get imagestreams -n theia-cloud

# Check a specific image
oc get imagestreamtag -n theia-cloud theia-cloud-operator:dev
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

Install cert-manager before deploying Theia Cloud. The CRDs chart uses cert-manager to generate the conversion webhook certificate. Wait for the cert-manager pods to be ready before proceeding.

```bash
oc apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.17.2/cert-manager.yaml
oc rollout status deployment/cert-manager-webhook -n cert-manager --timeout=120s
```

Then deploy Theia Cloud:

```bash
cd terraform/test-configurations/4-01_openshift_monitor

terraform init
terraform apply
```

This installs (in order):

* `theia-cloud-base`, RBAC, cluster roles, and cert-manager issuers
* `theia-cloud-crds`, Custom Resource Definitions and conversion webhook
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
