# Terraform test configurations

This directory contains terraform configuration that can be used to set up test clusters without any Theia Cloud installation but all required other dependencies.

Moreover the directory contains terraform configurations to install common Theia Cloud setups into existing clusters.

## Cluster/Base installations

Run `terraform init` and `terraform apply` in both directories below:

- `0_minikube-setup` installs a minikube cluster with all required dependencies, including a Keycloak with a TheiaCloud realm, but without Theia Cloud.
- `1_theia-cloud-base` installs theia cloud base

```bash
# run before destroying cluster
terraform state rm kubernetes_persistent_volume.minikube
```

## Theia Cloud Setups

Pick an installation in one of below directories and run `terraform init` and `terraform apply`.

- `2-01_try-now` installs a local version of <https://try.theia-cloud.io/>
- `2-02_monitor` installs a setup that allows to test the monitor (VSCode extension or Theia extension based) with and without authentication
- `2-03_try-now_paths` installs a local version of <https://try.theia-cloud.io/> using paths instead of subdomains.
- `2-04_try-now_paths_eager-start` installs a local version of <https://try.theia-cloud.io/> using paths and eager instead of lazy starting of pods. See its [README](./2-04_try-now_paths_eager-start/README.md) for more details.

## Getting a Keycloak access token

To test the service's APIs via a REST client such as Postman or Bruno, you need to provide a Bearer token to authenticate.
You can get such a token from Keycloak with a simple CLI call using `curl` and `jq`.
This call gets the token for test user `foo`.

```sh
curl -s --insecure --request POST \
  --url https://$(minikube ip).nip.io/keycloak/realms/TheiaCloud/protocol/openid-connect/token \
  --header 'content-type: application/x-www-form-urlencoded' \
  --data grant_type=password \
  --data client_id=theia-cloud \
  --data username=foo \
  --data password=foo | jq -r '.access_token'
```
