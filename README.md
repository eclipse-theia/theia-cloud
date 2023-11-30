# Theia Cloud

The goal of Theia Cloud is to simplify the deployment of Theia-based (and similar) products on Kubernetes. We follow a convention over configuration approach allowing users to get started fast. At the same time, we aim for extensibility allowing developers to customize all aspects of the kubernetes deployment as required.

For more information, please also visit [our website](https://theia-cloud.io/).

## Feedback, Help and Support

If you encounter any problems feel free to [open an issue](https://github.com/eclipsesource/theia-cloud/issues/new/choose) on the repo.
For questions and discussions please use the [the Github discussions](https://github.com/eclipsesource/theia-cloud/discussions).
You can also reach us via [email](mailto:support@theia-cloud.io?subject=Theia_Cloud).
In addition, EclipseSource also offers [professional support](https://eclipsesource.com/services/developer-support/) for Theia and Theia Cloud.

## Components

Theia Cloud consists of the following components.

### Kubernetes Custom Resource Definitions and Operator

Theia Cloud brings simple custom resource definitions (CRDs) that allow to specify the required configuration, like the docker image of the Theia-based product.\
A Java-based operator will listen for the creation, modification, and deletion of custom resources based on those CRDs and will manage the application.\
See [Architecture.md](doc/docs/Architecture.md) for more information on the architecture.

### Theia Cloud Service

This REST Service acts as the API for creating and stopping Theia-based products for an authenticated user as well as providing additional information.\
The Theia Cloud service creates, modifies, and deletes the custom resources the operator listens to.

### Sample Dashboard and reusable JS/UI components

Theia Cloud comes with a basic landing page/dashboard which allows to launch sessions.

We offer a common ts library for the API of the Theia Cloud service, which may be reused by clients to write their own custom dashboards.

We plan to offer reusable ui components in the future as well.

## Versioning

From version 0.9.0 onwards, every component in this repository (and the helm chart repo) will follow the same version number. This change will make it clearer which versions of different components are compatible with each other.

### Release Types

- **Releases:** Standard releases will occur every three months. We recommend to use these for deployments as those are thoroughly tested and are stable versions. You can then update, after three months, when the next version is available.
- **Pre-Releases:** Pre-release versions will be released on every commit. These versions will be tagged as `<current-version>-next.<git-sha>`. The latest version of a next version is available at `<current-version>-next`. Pre-releases are ideal for testing the latest features and changes or for making contributions. However we do not recommend, to use those versions in deployments.

The [helm charts](https://github.com/eclipsesource/theia-cloud-helm) are referencing the compatible version in their `appVersion` field.

Since, npm does not allow tags that follow Semver, next artifacts published to npm have the `next` tag instead of `<currentVersion-next>`.
This means, that those dependencies will be updated to newer version, once they are available. So again, for deployments you should either pin the version to a specific commit or use the released versions.

### Release a new version

New release every three months.

Make a commit were the next parts are removed from the `versions` across the repo (For consumed Theia Cloud npm dependencies, e.g. @eclipse-theiacloud/common, needs to be changed from `next` to the desired version).
When this commit is merged it should not result in pushed artifacts.
Create a `releases/<currentVersion>` branch. This will be used in the future if any backports are necessary. Also it makes versions easier to find.
Then create a Github release pointing to the commit. This will then publish the artifacts for the specific version and also set the version to latest.

## Building

All components are deployed as docker images and may be built with docker. See [Building.md](doc/docs/Building.md) for more information. We offer prebuilt images ready to use.

## Installation

We offer a helm chart at <https://github.com/eclipsesource/theia-cloud-helm> which may be used to install Theia Cloud. Please check our getting started guides below as well, which will explain the possible values in more detail.

We offer three charts:

* `theia-cloud-base` installs cluster wide resources that may be reused by multiple Theia Cloud installations in different namespaces
* `theia-cloud-crds` (starting with version 0.8.1) installs the custom resource definitions for Theia Cloud and migration servers for older custom resources. This may be reused by multiple Theia Cloud installations in different namespaces.
* `theia-cloud` installs the Theia Cloud operators, service, and landing-page. It depends on the two above charts.

Starting with version 0.8.1 you may use helm upgrade to update to newer Theia Cloud version.\
Older versions (before the introduction of `theia-cloud-crds`) require a manual uninstall and reinstall as well as a manual CRD upgrade step.

```bash
helm repo add theia-cloud-remote https://github.eclipsesource.com/theia-cloud-helm
helm repo update

# Last Release:
helm install theia-cloud-base theia-cloud-remote/theia-cloud-base --set issuer.email=your-mail@example.com

helm install theia-cloud theia-cloud-remote/theia-cloud --namespace theiacloud --create-namespace

# Cutting Edge:
helm install theia-cloud-base theia-cloud-remote/theia-cloud-base --set issuer.email=your-mail@example.com --devel

helm install theia-cloud-crds theia-cloud-remote/theia-cloud-crds  --namespace theiacloud --create-namespace --devel

helm install theia-cloud theia-cloud-remote/theia-cloud --namespace theiacloud --create-namespace --devel

# Optional: switch to the newly created namespace
kubectl config set-context --current --namespace=theiacloud

# Uninstall
helm uninstall theia-cloud -n theiacloud
```

### Getting started with

[...Terraform](terraform/terraform.md)

[...Minikube](doc/docs/platforms/Minikube.md)

[...GKE](terraform/terraform.md#google-kubernetes-engine)

[...configuring the Keycloak Realm](doc/docs/Keycloak.md)

## Security

Our Security Vulnerability Process may be found [here](SECURITY.md).
