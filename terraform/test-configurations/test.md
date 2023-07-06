# Terraform test configurations

This directory contains terraform configuration that can be used to set up test clusters without any Theia Cloud installation but all required other dependencies.

Moreover the directory contains terraform configurations to install common Theia Cloud setups into existing clusters.

## Cluster/Base installations

Run `terraform init` and `terraform apply` in both directories below:

- `0_minikube-setup` installs a minikube cluster with all required dependencies, including a Keycloak with a TheiaCloud realm, but without TheiaCloud.
- `1_theia-cloud-base` installs theia cloud base

```bash
# run before destroying cluster
terraform state rm kubernetes_persistent_volume.minikube
```

## Theia Cloud Setups

Pick an installation in one of below directories and run `terraform init` and `terraform apply`.

- `2-01_try-now` installs a local version of <https://try.theia-cloud.io/>
- `2-02_monitor-vscode` installs a setup that allows to test the vscode monitor with and without authentication
