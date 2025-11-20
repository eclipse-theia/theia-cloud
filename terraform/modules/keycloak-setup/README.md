# Keycloak Setup Module

This Terraform module deploys Keycloak in a Kubernetes cluster using the official Keycloak Operator. It replaces the deprecated Bitnami Helm chart approach with a native Kubernetes operator installation.

## Features

- Installs Keycloak Operator without Operator Lifecycle Manager (OLM)
- Configurable Keycloak operator version (default: v26.4.5)
- Deploys Keycloak with configurable resources and replicas
- Optional integrated PostgreSQL database deployment
- Kubernetes Ingress support with TLS
- **Optional cert-manager installation** (can be disabled if already installed)
- Cert-manager integration for automatic certificate generation
- Optional self-signed ClusterIssuer for local development
- Support for Minikube, GKE, and generic Kubernetes clusters
- Configurable HTTP relative path (e.g., `/keycloak/`)

## Prerequisites

The following components must be installed in your Kubernetes cluster before using this module:

1. **nginx-ingress-controller** (if using ingress): For routing traffic to Keycloak
2. **Persistent Volume provisioner**: For PostgreSQL data persistence (if using integrated database)

Note: cert-manager can be installed automatically by this module (default) or you can disable it if already present in your cluster.

## Usage

### Minikube Example (with cert-manager installation)

```hcl
module "keycloak" {
  source = "../../modules/keycloak-setup"

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
module "keycloak" {
  source = "../../modules/keycloak-setup"

  hostname                   = "keycloak.example.com"
  keycloak_admin_password    = var.keycloak_admin_password
  postgres_password          = var.postgres_password

  # Cert-manager installation
  install_cert_manager        = true
  cert_manager_issuer_email   = "admin@example.com"

  # GKE-specific configuration
  postgres_storage_class       = "standard-rwo"
  ingress_cert_manager_cluster_issuer = "letsencrypt-prod"
  cloud_provider               = "GKE"

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
module "keycloak" {
  source = "../../modules/keycloak-setup"

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
module "keycloak" {
  source = "../../modules/keycloak-setup"

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
| `keycloak_version`                  | `string` | `"26.4.5"`     | Keycloak operator version (tag from keycloak-k8s-resources repository) |
| `keycloak_http_relative_path`       | `string` | `"/keycloak/"` | HTTP relative path for Keycloak                                        |
| `keycloak_replicas`                 | `number` | `1`            | Number of Keycloak replicas                                            |
| `keycloak_resource_requests_cpu`    | `string` | `"500m"`       | CPU resource requests                                                  |
| `keycloak_resource_requests_memory` | `string` | `"1Gi"`        | Memory resource requests                                               |
| `keycloak_resource_limits_cpu`      | `string` | `"1"`          | CPU resource limits                                                    |
| `keycloak_resource_limits_memory`   | `string` | `"2Gi"`        | Memory resource limits                                                 |

### PostgreSQL Configuration

| Name                          | Type     | Default         | Description                                        |
| ----------------------------- | -------- | --------------- | -------------------------------------------------- |
| `postgres_enabled`            | `bool`   | `true`          | Whether to deploy PostgreSQL database              |
| `postgres_database`           | `string` | `"keycloak"`    | PostgreSQL database name                           |
| `postgres_username`           | `string` | `"keycloak"`    | PostgreSQL username                                |
| `postgres_storage_class`      | `string` | `""`            | Storage class for PostgreSQL PVC (empty = default) |
| `postgres_storage_size`       | `string` | `"10Gi"`        | Storage size for PostgreSQL PVC                    |
| `postgres_volume_permissions` | `bool`   | `false`         | Enable init container for volume permissions       |
| `postgres_image`              | `string` | `"postgres:16"` | PostgreSQL Docker image                            |

### Ingress Configuration

| Name                                  | Type          | Default   | Description                                  |
| ------------------------------------- | ------------- | --------- | -------------------------------------------- |
| `ingress_enabled`                     | `bool`        | `true`    | Whether to create Kubernetes Ingress         |
| `ingress_class_name`                  | `string`      | `"nginx"` | Ingress class name                           |
| `ingress_tls_enabled`                 | `bool`        | `true`    | Whether to enable TLS for ingress            |
| `ingress_cert_manager_cluster_issuer` | `string`      | `""`      | Cert-manager cluster issuer for TLS          |
| `ingress_cert_manager_common_name`    | `string`      | `""`      | The common name for the certificate          |
| `ingress_annotations`                 | `map(string)` | `{}`      | Additional annotations for ingress           |
| `ingress_tls_secret_name`             | `string`      | `""`      | Name of TLS secret (auto-generated if empty) |

### Cert-Manager Configuration

| Name                       | Type     | Default          | Description                                                          |
| -------------------------- | -------- | ---------------- | -------------------------------------------------------------------- |
| `install_cert_manager`     | `bool`   | `true`           | Whether to install cert-manager                                      |
| `cert_manager_version`     | `string` | `"v1.17.4"`      | Version of cert-manager to install                                   |
| `cert_manager_namespace`   | `string` | `"cert-manager"` | Namespace for cert-manager installation                              |
| `install_selfsigned_issuer`| `bool`   | `false`          | Whether to install self-signed ClusterIssuer for Keycloak            |
| `cert_manager_issuer_email`| `string` | `""`             | Email address for certificates (required for letsencrypt-prod)       |

### Other Configuration

| Name             | Type     | Default | Description                              |
| ---------------- | -------- | ------- | ---------------------------------------- |
| `cloud_provider` | `string` | `"K8S"` | Cloud provider type (MINIKUBE, GKE, K8S) |

## Outputs

| Name                    | Description                                  |
| ----------------------- | -------------------------------------------- |
| `namespace`             | Keycloak namespace                           |
| `keycloak_url`          | Full URL to access Keycloak                  |
| `admin_username`        | Keycloak admin username                      |
| `postgres_service_name` | PostgreSQL service name (if deployed)        |
| `keycloak_service_name` | Keycloak service name                        |
| `tls_secret_name`       | TLS certificate secret name (if TLS enabled) |

## Migration from Bitnami Helm Chart

This module replaces the deprecated Bitnami Helm chart with the official Keycloak Operator. Key differences:

### What Changed

1. **Installation Method**: Uses Keycloak Operator instead of Helm chart
2. **Image Source**: Uses official Keycloak images instead of Bitnami images
3. **CRD-based**: Keycloak instance is defined as a Custom Resource
4. **Database**: PostgreSQL is deployed separately (not as a sub-chart)

### Migration Steps

1. **Backup Data**: Export realms and data from existing Keycloak instance
2. **Update Module Reference**: Change from `helm` module to `keycloak-setup` module
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
kubectl logs -n keycloak -l app=keycloak-operator
```

Check Keycloak resource status:

```bash
kubectl get keycloak -n keycloak keycloak -o yaml
```

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

## License

This module follows the same license as the parent project.
