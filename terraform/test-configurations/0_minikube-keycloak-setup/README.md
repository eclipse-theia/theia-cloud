# Minikube Keycloak Setup Test Configuration

This test configuration demonstrates the use of the `keycloak-setup` module without the deprecated `helm` module. It provides a complete, standalone Keycloak deployment on Minikube.

## What This Configuration Includes

1. **Minikube Cluster Creation**: Sets up a local Kubernetes cluster
2. **Nginx Ingress Controller**: Deployed directly via Helm (not through the helm module)
3. **Cert-Manager**: Installed by the keycloak-setup module
4. **Self-Signed ClusterIssuer**: For local TLS certificates
5. **Keycloak**: Deployed using the official Keycloak Operator
6. **PostgreSQL**: Integrated database deployment
7. **Keycloak Realm Configuration**: Test realm with users and groups

## Key Differences from Old Configuration

| Old (0_minikube-setup)             | New (0_minikube-keycloak-setup)            |
| ---------------------------------- | ------------------------------------------ |
| Uses `modules/helm` for everything | Uses `modules/keycloak-setup` for Keycloak |
| Bitnami Helm chart for Keycloak    | Official Keycloak Operator                 |
| Cert-manager in helm module        | Cert-manager in keycloak-setup module      |
| nginx-ingress via helm module      | nginx-ingress deployed separately          |

## Prerequisites

1. Minikube installed
2. KVM2 driver (or adjust the `driver` variable in the configuration)
3. Terraform >= 1.12.2

## Usage

### Initialize and Apply

```bash
cd terraform/test-configurations/0_minikube-keycloak-setup
terraform init
terraform apply
```

### Access Keycloak

After successful deployment:

1. Get the hostname:

   ```bash
   terraform output hostname
   ```

2. Access Keycloak at:

   ```
   https://<hostname>/keycloak/
   ```

3. Login with:
   - Username: `admin`
   - Password: `admin` (or the value set in `keycloak_admin_password` variable)

### Test Users

The configuration creates a test realm with two users:

- **foo** (password: `foo`) - Member of admin group
- **bar** (password: `bar`)

### Verify Installation

Check all components are running:

```bash
# Cert-manager
kubectl get pods -n cert-manager

# Keycloak operator
kubectl get pods -n keycloak -l app=keycloak-operator

# Keycloak instance
kubectl get keycloak -n keycloak

# PostgreSQL
kubectl get pods -n keycloak -l app=postgres

# Ingress
kubectl get ingress -n keycloak

# Certificate
kubectl get certificate -n keycloak
```

### Cleanup

```bash
terraform destroy
```

## Configuration Variables

You can customize the deployment by setting these variables:

```hcl
# In terraform.tfvars
kubernetes_version = "v1.26.3"
cert_manager_issuer_email = "your-email@example.com"
keycloak_admin_password = "your-secure-password"
```

## Troubleshooting

### Keycloak Not Accessible

1. Check if the ingress controller is ready:

   ```bash
   kubectl get pods -n ingress-nginx
   ```

2. Verify Keycloak pods are running:

   ```bash
   kubectl get pods -n keycloak
   ```

3. Check certificate status:

   ```bash
   kubectl get certificate -n keycloak
   kubectl describe certificate -n keycloak
   ```

### Self-Signed Certificate Warning

This is expected for local development. The browser will show a certificate warning. You can safely proceed by accepting the self-signed certificate.

### 401 Unauthorized When Configuring Keycloak Realm

If you get a 401 error during `terraform apply`, this is likely due to:

1. **Keycloak URL format**: Keycloak 26 changed the admin console path structure. Check the actual URL:

   ```bash
   kubectl get ingress -n keycloak -o yaml
   ```

2. **Verify Keycloak admin credentials**: Test login manually:

   ```bash
   # Get the Keycloak URL
   terraform output keycloak_url

   # Test authentication
   curl -k -X POST \
     "$(terraform output -raw keycloak_url)/realms/master/protocol/openid-connect/token" \
     -d "client_id=admin-cli" \
     -d "username=admin" \
     -d "password=admin" \
     -d "grant_type=password"
   ```

3. **Keycloak not fully initialized**: Even after pods are ready, Keycloak's admin console might need more time. Wait 1-2 minutes after the module completes, then run `terraform apply` again.

### Database Connection Issues

Check PostgreSQL logs:

```bash
kubectl logs -n keycloak -l app=postgres
```

Check Keycloak operator logs:

```bash
kubectl logs -n keycloak -l app=keycloak-operator
```

## Benefits of This Approach

1. **No Deprecated Dependencies**: Uses official Keycloak Operator instead of Bitnami chart
2. **Modular**: Keycloak setup is independent of other components
3. **Self-Contained**: All cert-manager setup included in keycloak-setup module
4. **Production-Ready**: Same pattern can be used for production deployments
5. **Easier to Update**: Module can be updated independently

## Next Steps

This configuration can be extended to:

1. Install Theia Cloud components
2. Add additional Keycloak realms and clients
3. Configure external PostgreSQL database
4. Use Let's Encrypt for production TLS certificates
