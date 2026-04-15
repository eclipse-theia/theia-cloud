# Terraform test configurations

This directory contains terraform configuration that can be used to set up test clusters without any Theia Cloud installation but all required other dependencies.

Moreover the directory contains terraform configurations to install common Theia Cloud setups into existing clusters.

## Cluster/Base installations

Run `terraform init` and `terraform apply` in both directories below:

- `0_minikube-setup` installs a minikube cluster with all required dependencies, including a Keycloak with a TheiaCloud realm, but without Theia Cloud.
- `1_theia-cloud-base` installs theia cloud base

```bash
# run before destroying cluster
terraform state rm kubernetes_persistent_volume_v1.minikube
```

## OpenShift Setup

For testing on OpenShift (using OpenShift Local / CRC), see [openshift.md](./openshift.md).

- `4_openshift-setup` captures OpenShift connection details and installs all dependencies (cert-manager, Keycloak with a TheiaCloud realm) for downstream terraform steps.

## Theia Cloud Setups

Pick an installation in one of below directories and run `terraform init` and `terraform apply`.

- `3-01_try-now` installs a local version of <https://try.theia-cloud.io/>
- `3-02_monitor` installs a setup that allows to test the monitor (VSCode extension or Theia extension based) with and without authentication
- `3-03_try-now_paths` installs a local version of <https://try.theia-cloud.io/> using paths instead of subdomains.
- `3-04_try-now_paths_eager-start` installs a local version of <https://try.theia-cloud.io/> using paths and eager instead of lazy starting of pods. See its [README](./3-04_try-now_paths_eager-start/README.md) for more details.

### OpenShift Setups

These configurations deploy Theia Cloud on an OpenShift cluster (using Routes instead of Ingress). Run `4_openshift-setup` first, see [openshift.md](./openshift.md).

- `5-01_openshift_monitor` installs Theia Cloud with OpenShift Route support, activity monitoring, and Keycloak authentication, using `valuesOpenShiftMonitor.yaml`

## Getting a Keycloak access token

To test the service's APIs via a REST client such as Postman or Bruno, you need to provide a Bearer token to authenticate.
You can get such a token from Keycloak with a simple CLI call using `curl` and `jq`.
This call gets the token for test user `foo`.

Minikube:

```sh
curl -s --insecure --request POST \
  --url https://$(minikube ip).nip.io/keycloak/realms/TheiaCloud/protocol/openid-connect/token \
  --header 'content-type: application/x-www-form-urlencoded' \
  --data grant_type=password \
  --data client_id=theia-cloud \
  --data username=foo \
  --data password=foo | jq -r '.access_token'
```

OpenShift Local:

```sh
curl -s --insecure --request POST \
  --url https://keycloak.apps-crc.testing/realms/TheiaCloud/protocol/openid-connect/token \
  --header 'content-type: application/x-www-form-urlencoded' \
  --data grant_type=password \
  --data client_id=theia-cloud \
  --data username=foo \
  --data password=foo | jq -r '.access_token'
```
