# Helm

## Prerequisites

### Install Cert-Manager & NginX Ingress Controller

https://cert-manager.io/docs/installation/

https://kubernetes.github.io/ingress-nginx/deploy/

## Install

```bash
# Setup without Keycloak
helm install theia-cloud ./helm/theia.cloud --namespace theiacloud --create-namespace

# Setup with keycloak
helm install theia-cloud ./helm/theia.cloud --namespace theiacloud --create-namespace --values ./helm/theia.cloud/valuesKeycloak.yaml
```

## Check

```bash
helm get manifest theia-cloud -n theiacloud
```

## Uninstall

```bash
helm uninstall theia-cloud -n theiacloud
```
