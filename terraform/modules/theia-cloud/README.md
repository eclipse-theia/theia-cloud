# Theia Cloud Installation Module

This module installs Theia Cloud components in a Kubernetes cluster via Helm.

## Prerequisites

Before using this module, ensure the following are already installed in your cluster:

- **Cert Manager** (v1.17.4 or compatible) - Required for certificate management
- **Nginx Ingress Controller** (v4.13.0 or compatible) - Required for ingress routing
- **Keycloak** (v26.4.5 or compatible) - Required for authentication

## What This Module Installs

This module will install:

1. **theia-cloud-base** - Cluster-wide resources including cert issuers
2. **theia-cloud-crds** - Custom resource definitions for Theia Cloud
3. **theia-cloud** - The Theia Cloud operators, service, and landing page

## Usage

We expect users to be familiar with Helm and that `kubectl` points to the cluster where Theia Cloud will be installed.

### Basic Example

```terraform
module "theia_cloud" {
  source = "./modules/theia-cloud"

  hostname                   = "theia.example.com"
  cert_manager_issuer_email  = "admin@example.com"
  cloudProvider              = "K8S"
}
```

## Variables

- `install_theia_cloud_base` (optional, default: `true`) - Whether to install theia-cloud-base chart
- `install_theia_cloud_crds` (optional, default: `true`) - Whether to install theia-cloud-crds chart
- `install_theia_cloud` (optional, default: `true`) - Whether to install theia-cloud chart
- `hostname` (required) - The hostname for Theia Cloud services
- `keycloak_url` (optional) - The base URL of the Keycloak instance used for authentication. If not provided, it will be constructed from the 'hostname' variable assuming keycloak is hosted at relative path /keycloak/.
- `cert_manager_issuer_email` (required) - Email address used for certificate management
- `cloudProvider` (optional, default: `"K8S"`) - The cloud provider (e.g., "K8S", "MINIKUBE", "GKE")
