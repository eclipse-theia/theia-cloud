# Theia.cloud

The goal of Theia.cloud is to simplify the deployment of Theia-based (and similar) products on Kubernetes. We follow a convention over configuration approach allowing users to get started fast. At the same time, we aim for extensibility allowing developers to customize certain aspects of the kubernetes deployment if required.

## Components

Theia.cloud consists of the following components.

### Kubernetes Custom Resource Definitions and Operator

Theia.cloud brings simple custom resource definitions (CRDs) that allow to specify the required conifugration, like the docker image of the Theia-based product.\
A Java-based operator will listen for the creation, modification, and deletion of custom resources based on those CRDs and will manage the application.\
See [Architecture.md](doc/docs/Architecture.md) for more information on the architecture.

### Workspace REST Service

This REST Service acts as the API for creating and stopping Theia-based products for an authenticated user as well as providing additional information.\
The workspace service creates, modifies, and deletes the custom resources the operator listens to.

### Dashboard and resusable UI components

The UI components communicating with the REST service.

## Building

All components are deployed as docker images and may be built with docker. See [Building.md](doc/docs/Building.md) for more information. We offer prebuilt images ready to use.

## Installation

We offer a helm chart under `helm/theia.cloud` which may be used to install Theia.cloud. Please check our getting started guides below as well, which will explain the possible values in more detail.

```bash
helm install theia-cloud ./helm/theia.cloud --namespace theiacloud --create-namespace
# Optional: switch to the newly created namespace
kubectl config set-context --current --namespace=theiacloud

# Uninstall
helm uninstall theia-cloud -n theiacloud
```

### Getting started with

[...Minikube](doc/docs/platforms/Minikube.md)

[...configuring the Keycloak Realm](doc/docs/Keycloak.md)
