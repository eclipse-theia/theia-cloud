# Getting started with Minikube

Minikube is a local Kubernetes that allows you to test and develop for Kubernetes.\
In this guide we will show you how to install minikube and helm 3 as well as cert-manager, the cloud native certificate management, the NginX Ingress Controller and Keycloak. We will use existing installation methods for all of the Theia.Cloud preqrequisites.\
Finally we will install and try Theia.Cloud using helm.

If you have no experience with Kubernetes/Minikube yet, we encourage you to check out some basic tutorials first, e.g. https://kubernetes.io/docs/tutorials/hello-minikube/ and https://kubernetes.io/docs/tutorials/kubernetes-basics/

## Install and start Theia Cloud on minikube

Please refer to [our terraform documentation](../../../terraform/terraform.md) and the ready to use [minikube configuration](../../../terraform/terraform.md#minikube).

### Accept Self Signed Certificates

Since we are running in Minikube on our local machine, which is not exposed to the internet, we cannot use letsencrypt certificates.

Please accept the locally issued certificates:

In Chrome:
`Your connection is not private -> Advanced -> Proceed to ...`

In Firefox:
`Warning: Potential Security Risk Ahead -> Advanced... -> Accept the Risk and Continue`

## Testing local Theia.cloud images

You can test locally build images (e.g. of the landing page, service, or operator) by building them in Minikube and then using them in the Minikube Helm chart.

To build images directly in Minikube, execute `eval $(minikube docker-env)` in a terminal.
With this, the `docker` command in this terminal runs inside Minikube.

Build the docker image as usual.

- Adapt the AppDefinition `image` value to match your built image.
