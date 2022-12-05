# Helm

## Install Prerequisites

### Helm 3

Follow the steps in https://helm.sh/docs/intro/install/ to install Helm on your system.

### Cert-Manager

Please check https://cert-manager.io/docs/installation/ for the latest installation instructions.

As of writing this guide the installation command looks like this:\
`kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.8.0/cert-manager.yaml`

### Metrics Server

Please check https://github.com/kubernetes-sigs/metrics-server#installation for the latest installation instructions.

As of writing this guide the installation command looks like this:\
`kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml`

### NginX Ingress Controller

Follow the installation guide for your platform:\
https://kubernetes.github.io/ingress-nginx/deploy/

### Global certificate (Only when using paths)

If Theia Cloud is used with paths instead of subdomains, the global HTTPS certificate should exist and be configured as the NginX Ingress Controller's default certificate.

You can either provide the default certificate yourself or use a dummy service to initially generate the certificate.
In [global-certificate.yaml](./platforms/global-certificate.yaml), replace `example.com` with your own host and `letsencrypt-prod` with your certificate issuer of choice.
Apply the configuration with `kubectl apply -f  ./doc/docs/platforms/global-certificate.yaml`.

Configure the nginx controllers deployment to use the certificate `default-tls` in the default namespace as its default certificate.
For this, add argument `"--default-ssl-certificate=default/default-tls"` to the container by patching the controller's deployment.

```bash
kubectl patch deploy ingress-nginx-controller --type='json' -n ingress-nginx \
-p '[{ "op": "add", "path": "/spec/template/spec/containers/0/args/-", "value": "--default-ssl-certificate=default/default-tls" }]'
```

### Optional: Keycloak

## Install

For this demo we will use nip.io hostnames.

Obtain the external IP address of the `ingress-nginx-controller`:

```bash
$ kubectl get svc --all-namespaces
NAMESPACE       NAME                                 TYPE           CLUSTER-IP     EXTERNAL-IP    PORT(S)                      AGE
ingress-nginx   ingress-nginx-controller             LoadBalancer   10.52.4.129    34.141.62.32   80:32507/TCP,443:32114/TCP   11m

```

Open `./helm/theia.cloud/valuesGKETryNow.yaml` and update the host section to use `subdomain.34.141.62.32.nip.io`:

```yaml
hosts:
  service: service.34.141.62.32.nip.io
  landing: theia.cloud.34.141.62.32.nip.io
  instance: ws.34.141.62.32.nip.io
```

```bash
kubectl config set-context --current --namespace=theiacloud
# Setup without Keycloak
helm install theia-cloud ./helm/theia.cloud --namespace theiacloud --create-namespace

# Setup with keycloak
helm install theia-cloud ./helm/theia.cloud --namespace theiacloud --create-namespace --values ./helm/theia.cloud/valuesKeycloak.yaml
```

### Trouble shooting

Recreate tls secret for sessions:

`kubectl delete secret ws-cert-secret -n theiacloud`

## Check

```bash
helm get manifest theia-cloud -n theiacloud
```

## Uninstall

```bash
helm uninstall theia-cloud -n theiacloud
```
