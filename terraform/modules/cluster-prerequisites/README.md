# Cluster Prerequisites Setup Module

This Terraform module sets up various prerequisites of Theia Cloud in a running cluster.
The module offers various customization options via variables including skipping some of the prerequisites (e.g. Keycloak) if they are already installed another way.

## Features

- Installs Keycloak Operator without Operator Lifecycle Manager (OLM)
- Installs all CRDs required by the selected operator version, including the OIDC and SAML client CRDs introduced in Keycloak 26.7. Versions below 26.7 skip these CRDs; downgrading removes them and any resources that use them.
- Configurable Keycloak operator version (default: 26.7.2)
- Deploys Keycloak with configurable resources and replicas
- Optional integrated PostgreSQL database deployment
- Kubernetes Ingress support with TLS
- **Optional cert-manager installation** (can be disabled if already installed)
- Cert-manager integration for automatic certificate generation
- Optional self-signed ClusterIssuer for local development
- Support for Minikube, OpenShift, and generic Kubernetes clusters
- Configurable HTTP relative path (e.g., `/keycloak/`)

## Prerequisites

The following components must be installed in your Kubernetes cluster before using this module:

1. **Persistent Volume provisioner**: For PostgreSQL data persistence (if using integrated database)

Note: cert-manager can be installed automatically by this module (default) or you can disable it if already present in your cluster.

## Usage

### Minikube Example (with cert-manager installation)

```hcl
module "cluster-prerequisites" {
  source = "../../modules/cluster-prerequisites"

  hostname                   = "192.168.49.2.nip.io"
  keycloak_admin_password    = "admin"
  postgres_password          = "admin"

  # Cert-manager installation
  install_cert_manager       = true
  install_selfsigned_issuer  = true

  # Minikube-specific configuration
  postgres_storage_class       = "manual"
  postgres_volume_permissions  = true
  ingress_cert_manager_cluster_issuer = "keycloak-selfsigned-issuer"
  cloud_provider               = "MINIKUBE"
}
```

### GKE Example (with Let's Encrypt)

```hcl
module "cluster-prerequisites" {
  source = "../../modules/cluster-prerequisites"

  hostname                   = "keycloak.example.com"
  keycloak_admin_password    = var.keycloak_admin_password
  postgres_password          = var.postgres_password

  # Cert-manager installation
  install_cert_manager        = true
  cert_manager_issuer_email   = "admin@example.com"

  # GKE-specific configuration
  postgres_storage_class       = "standard-rwo"
  ingress_cert_manager_cluster_issuer = "letsencrypt-prod"

  # Production resources
  keycloak_replicas            = 2
  keycloak_resource_requests_cpu    = "1"
  keycloak_resource_requests_memory = "2Gi"
  keycloak_resource_limits_cpu      = "2"
  keycloak_resource_limits_memory   = "4Gi"
}
```

### Using Existing cert-manager Installation

```hcl
module "cluster-prerequisites" {
  source = "../../modules/cluster-prerequisites"

  hostname                   = "keycloak.example.com"
  keycloak_admin_password    = var.keycloak_admin_password
  postgres_password          = var.postgres_password

  # Use existing cert-manager
  install_cert_manager        = false
  ingress_cert_manager_cluster_issuer = "my-existing-issuer"

  postgres_storage_class       = "standard"
}
```

### Using External PostgreSQL Database

```hcl
module "cluster-prerequisites" {
  source = "../../modules/cluster-prerequisites"

  hostname                = "keycloak.example.com"
  keycloak_admin_password = var.keycloak_admin_password

  # Disable integrated PostgreSQL
  postgres_enabled = false

  # Note: You'll need to manually configure external database
  # connection in the Keycloak CR or use environment variables
}
```

## Input Variables

### Required Variables

| Name                      | Type     | Description                                                        |
| ------------------------- | -------- | ------------------------------------------------------------------ |
| `hostname`                | `string` | Hostname for Keycloak ingress                                      |
| `keycloak_admin_password` | `string` | Keycloak admin password (sensitive)                                |
| `postgres_password`       | `string` | PostgreSQL password (sensitive, required if postgres_enabled=true) |

### Keycloak Configuration

| Name                                | Type     | Default        | Description                                                            |
| ----------------------------------- | -------- | -------------- | ---------------------------------------------------------------------- |
| `keycloak_admin_username`           | `string` | `"admin"`      | Keycloak admin username                                                |
| `keycloak_namespace`                | `string` | `"keycloak"`   | Kubernetes namespace for Keycloak                                      |
| `keycloak_version`                  | `string` | `"26.7.2"`     | Keycloak operator version (tag from keycloak-k8s-resources repository) |
| `keycloak_http_relative_path`       | `string` | `"/keycloak/"` | HTTP relative path for Keycloak                                        |
| `keycloak_replicas`                 | `number` | `1`            | Number of Keycloak replicas                                            |
| `keycloak_resource_requests_cpu`    | `string` | `"500m"`       | CPU resource requests                                                  |
| `keycloak_resource_requests_memory` | `string` | `"1Gi"`        | Memory resource requests                                               |
| `keycloak_resource_limits_cpu`      | `string` | `"1"`          | CPU resource limits                                                    |
| `keycloak_resource_limits_memory`   | `string` | `"2Gi"`        | Memory resource limits                                                 |
| `keycloak_ready_timeout_seconds`    | `number` | `600`          | Maximum seconds to wait for Keycloak to serve requests                 |
| `keycloak_ready_check_in_cluster`   | `bool`   | `true`         | Verify the master realm from inside the Keycloak pod                   |
| `keycloak_ready_check_external`     | `bool`   | `true`         | Verify Keycloak through the ingress or OpenShift Route                 |

### PostgreSQL Configuration

| Name                          | Type     | Default         | Description                                        |
| ----------------------------- | -------- | --------------- | -------------------------------------------------- |
| `postgres_enabled`            | `bool`   | `true`          | Whether to deploy PostgreSQL database              |
| `postgres_database`           | `string` | `"keycloak"`    | PostgreSQL database name                           |
| `postgres_username`           | `string` | `"keycloak"`    | PostgreSQL username                                |
| `postgres_storage_class`      | `string` | `""`            | Storage class for PostgreSQL PVC (empty = default) |
| `postgres_storage_size`       | `string` | `"10Gi"`        | Storage size for PostgreSQL PVC                    |
| `postgres_volume_permissions` | `bool`   | `false`         | Enable init container for volume permissions       |
| `postgres_image`              | `string` | `"postgres:17"` | PostgreSQL Docker image                            |

### Ingress Configuration

| Name                                  | Type          | Default           | Description                                    |
| ------------------------------------- | ------------- | ----------------- | ---------------------------------------------- |
| `ingress_controller_type`             | `string`      | `"haproxy"`       | Type of ingress controller to use              |
| `ingress_enabled`                     | `bool`        | `true`            | Whether to create Kubernetes Ingress           |
| `ingress_class_name`                  | `string`      | `"haproxy"`       | Ingress class name                             |
| `ingress_tls_enabled`                 | `bool`        | `true`            | Whether to enable TLS for ingress              |
| `ingress_cert_manager_cluster_issuer` | `string`      | `""`              | Cert-manager cluster issuer for TLS            |
| `ingress_cert_manager_common_name`    | `string`      | `""`              | The common name for the certificate            |
| `ingress_annotations`                 | `map(string)` | `{}`              | Additional annotations for ingress             |
| `ingress_tls_secret_name`             | `string`      | `""`              | Name of TLS secret (auto-generated if empty)   |
| `install_ingress_controller`          | `bool`        | `false`           | Whether to install the ingress controller      |
| `ingress_controller_version`          | `string`      | `"4.15.1"`        | ingress-nginx chart version (nginx only)       |
| `ingress_controller_namespace`        | `string`      | `"ingress-nginx"` | ingress-nginx namespace (nginx only)           |
| `load_balancer_ip`                    | `string`      | `""`              | External IP for the ingress controller service |

### Cert-Manager Configuration

| Name                        | Type     | Default          | Description                                                    |
| --------------------------- | -------- | ---------------- | -------------------------------------------------------------- |
| `install_cert_manager`      | `bool`   | `true`           | Whether to install cert-manager                                |
| `cert_manager_version`      | `string` | `"v1.21.2"`      | Version of cert-manager to install                             |
| `cert_manager_namespace`    | `string` | `"cert-manager"` | Namespace for cert-manager installation                        |
| `install_selfsigned_issuer` | `bool`   | `false`          | Whether to install self-signed ClusterIssuer for Keycloak      |
| `cert_manager_issuer_email` | `string` | `""`             | Email address for certificates (required for letsencrypt-prod) |

### Other Configuration

| Name             | Type     | Default | Description                                    |
| ---------------- | -------- | ------- | ---------------------------------------------- |
| `cloud_provider` | `string` | `"K8S"` | Cloud provider type (MINIKUBE, K8S, OPENSHIFT) |

## Outputs

| Name                    | Description                                          |
| ----------------------- | ---------------------------------------------------- |
| `namespace`             | Keycloak namespace                                   |
| `keycloak_url`          | Full URL to access Keycloak (without trailing slash) |
| `admin_username`        | Keycloak admin username                              |
| `postgres_service_name` | PostgreSQL service name (if deployed)                |
| `keycloak_service_name` | Keycloak service name                                |
| `tls_secret_name`       | TLS certificate secret name (if TLS enabled)         |

## Migration from Bitnami Helm Chart

This module replaces the deprecated Bitnami Helm chart with the official Keycloak Operator. Key differences:

### What Changed

1. **Installation Method**: Uses Keycloak Operator instead of Helm chart
2. **Image Source**: Uses official Keycloak images instead of Bitnami images
3. **CRD-based**: Keycloak instance is defined as a Custom Resource
4. **Database**: PostgreSQL is deployed separately (not as a sub-chart)

### Migration Steps

1. **Backup Data**: Export realms and data from existing Keycloak instance
2. **Update Module Reference**: Change from `helm` module to `cluster-prerequisites` module
3. **Update Variables**: Some variable names have changed (see mapping below)
4. **Apply Changes**: Run `terraform apply` to deploy new Keycloak
5. **Restore Data**: Import realms and data into new instance

### Variable Mapping

| Old (Bitnami)                  | New (Operator)                |
| ------------------------------ | ----------------------------- |
| `postgresql_storageClass`      | `postgres_storage_class`      |
| `postgresql_volumePermissions` | `postgres_volume_permissions` |
| `postgresql_enabled`           | `postgres_enabled`            |
| `auth.adminPassword`           | `keycloak_admin_password`     |
| `httpRelativePath`             | `keycloak_http_relative_path` |

## Troubleshooting

### Keycloak Pod Not Starting

Check the operator logs:

```bash
kubectl logs -n keycloak -l app.kubernetes.io/name=keycloak-operator
```

Check Keycloak resource status:

```bash
kubectl get keycloak -n keycloak keycloak -o yaml
```

If the operator logs report `Couldn't start informer` and `Not Found` for `keycloakoidcclients` or `keycloaksamlclients`, verify that all operator CRDs are installed:

```bash
kubectl get crd \
  keycloaks.k8s.keycloak.org \
  keycloakrealmimports.k8s.keycloak.org \
  keycloakoidcclients.k8s.keycloak.org \
  keycloaksamlclients.k8s.keycloak.org
```

### Keycloak Returns 503 After Pods Are Ready

Keycloak can report Kubernetes readiness before the master realm is ready to serve requests. The module therefore waits for both the in-cluster realm endpoint and, when enabled, the external ingress or OpenShift Route.

The Terraform output reports both checks on every attempt:

```text
[1/120] in-cluster: realms/master=503 health/ready=200
[2/120] external https://keycloak.example.com/keycloak/realms/master -> HTTP 503
```

If either check cannot run in the local environment, disable it explicitly:

```hcl
keycloak_ready_check_in_cluster = false
keycloak_ready_check_external   = false
```

The readiness provisioners require a POSIX-compatible shell. On Windows, run Terraform from WSL or Git Bash. The two flags disable the HTTP probes when `kubectl exec` or external routing cannot be used, but the provisioners still require a POSIX shell. Kubernetes resource, pod, and service endpoint readiness checks always run.

Use `keycloak_ready_timeout_seconds` to change the default 600-second timeout; values below 5 seconds are rejected. On failure, inspect the response headers and body to identify whether the 503 came from Keycloak or the ingress/router, then check the Keycloak service endpoints and pod logs.

### Database Connection Issues

Verify PostgreSQL is running:

```bash
kubectl get pods -n keycloak -l app=postgres
kubectl logs -n keycloak -l app=postgres
```

Check database credentials:

```bash
kubectl get secret -n keycloak postgres-credentials -o yaml
```

### TLS Certificate Not Generated

Check cert-manager:

```bash
kubectl get certificate -n keycloak
kubectl describe certificate -n keycloak <certificate-name>
```

Verify cluster issuer exists:

```bash
kubectl get clusterissuer
```

### Ingress Not Working

Check ingress status:

```bash
kubectl get ingress -n keycloak
kubectl describe ingress -n keycloak keycloak
```

Verify ingress controller is running:

```bash
kubectl get pods -n ingress-nginx
```

### Volume Permission Errors

If PostgreSQL fails with permission errors, enable volume permissions:

```hcl
postgres_volume_permissions = true
```

## Additional Resources

- [Keycloak Operator Documentation](https://www.keycloak.org/operator/installation)
- [Keycloak on Kubernetes Guide](https://www.keycloak.org/operator/basic-deployment)
- [Keycloak K8s Resources Repository](https://github.com/keycloak/keycloak-k8s-resources)
