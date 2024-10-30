# Helm

## Install Prerequisites

### Helm 3

Follow the steps in <https://helm.sh/docs/intro/install/> to install Helm on your system.

### Cert-Manager

Please check <https://cert-manager.io/docs/installation/> for the latest installation instructions.

As of writing this guide the installation command looks like this:\
`kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.11.0/cert-manager.yaml`

### Metrics Server

Please check <https://github.com/kubernetes-sigs/metrics-server#installation> for the latest installation instructions.

As of writing this guide the installation command looks like this:\
`kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml`

### NginX Ingress Controller

Follow the installation guide for your platform:\
<https://kubernetes.github.io/ingress-nginx/deploy/>

### Global certificate (Only when using paths)

If Theia Cloud is used with paths instead of subdomains, the global HTTPS certificate should exist and be configured as the NginX Ingress Controller's default certificate.

You can either provide the default certificate yourself or use a dummy service to initially generate the certificate.
In [global-certificate.yaml](./platforms/global-certificate.yaml), replace `example.com` with your own host and `letsencrypt-prod` with your certificate issuer of choice.
Apply the configuration with `kubectl apply -f  ./documentation/platforms/global-certificate.yaml`.

Configure the nginx controllers deployment to use the certificate `default-tls` in the default namespace as its default certificate.
For this, add argument `"--default-ssl-certificate=default/default-tls"` to the container by patching the controller's deployment.

```bash
kubectl patch deploy ingress-nginx-controller --type='json' -n ingress-nginx \
-p '[{ "op": "add", "path": "/spec/template/spec/containers/0/args/-", "value": "--default-ssl-certificate=default/default-tls" }]'
```

## Install

For this demo we will use nip.io hostnames.

Obtain the external IP address of the `ingress-nginx-controller`:

```bash
$ kubectl get svc --all-namespaces
NAMESPACE       NAME                                 TYPE           CLUSTER-IP     EXTERNAL-IP    PORT(S)                      AGE
ingress-nginx   ingress-nginx-controller             LoadBalancer   10.52.4.129    34.141.62.32   80:32507/TCP,443:32114/TCP   11m
```

Open `./terraform/values/valuesDemo.yaml` and update the host section to use `subdomain.34.141.62.32.nip.io`:

```yaml
hosts:
  configuration:
    baseHost: 34.141.62.32.nip.io
    service: service
    landing: theia-cloud
    instance: ws
```

### Add & update Theia Cloud helm repo

```bash
helm repo add theia-cloud-remote https://eclipse-theia.github.io/theia-cloud-helm
helm repo update
```

### Install the last release

```bash
helm install theia-cloud-base theia-cloud-remote/theia-cloud-base --set issuer.email=your-mail@example.com

helm install theia-cloud-crds theia-cloud-remote/theia-cloud-crds  --namespace theia-cloud --create-namespace

helm install theia-cloud theia-cloud-remote/theia-cloud --namespace theia-cloud
```

### Install the current next version

Simply add the `--devel` flag:

```bash
helm install theia-cloud-base theia-cloud-remote/theia-cloud-base --set issuer.email=your-mail@example.com --devel

helm install theia-cloud-crds theia-cloud-remote/theia-cloud-crds  --namespace theia-cloud --create-namespace --devel

helm install theia-cloud theia-cloud-remote/theia-cloud --namespace theia-cloud --devel
```

### Optional: switch to the newly created namespace

```bash
kubectl config set-context --current --namespace=theia-cloud
```

### Trouble shooting

Recreate tls secret for sessions:

`kubectl delete secret ws-cert-secret -n theia-cloud`

## Check

```bash
helm get manifest theia-cloud -n theia-cloud
```

## Uninstall

```bash
helm uninstall theia-cloud -n theia-cloud
```
