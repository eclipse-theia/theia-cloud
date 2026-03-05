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

### Gateway API + Envoy Gateway (required)

Theia Cloud routes traffic through Gateway API (`HTTPRoute`) and requires Envoy Gateway as controller.

The operator configures `X-Forwarded-Uri` using Envoy runtime syntax (`%REQ(:PATH)%`) in HTTPRoute rules.
With non-Envoy Gateway controllers this may be interpreted as a literal value and break path forwarding.

Install Gateway API CRDs and Envoy Gateway first by following:

- Gateway API: <https://gateway-api.sigs.k8s.io/guides/>
- Envoy Gateway: <https://gateway.envoyproxy.io/latest/install/install-helm/>

After installation, verify the controller is available (default class name usually `envoy`):

```bash
kubectl get gatewayclass
kubectl get gateway -A
```

### Global certificate (only when using paths)

If Theia Cloud is configured to use paths instead of subdomains, ensure the configured Gateway listeners cover your base host and use TLS certificates that match that host.

When using the Helm chart-managed Gateway (`gateway.create=true`), listener + certificate references are rendered automatically from chart values.
When using a shared external Gateway (`gateway.create=false`), configure matching listeners and TLS certificateRefs on that shared Gateway before deploying Theia Cloud.

## Install

For this demo we will use nip.io hostnames.

Obtain the external IP address of your Envoy Gateway service:

```bash
$ kubectl get svc --all-namespaces
NAMESPACE       NAME                                 TYPE           CLUSTER-IP     EXTERNAL-IP    PORT(S)                      AGE
envoy-gateway-system   envoy-gateway                 LoadBalancer   10.52.4.129    34.141.62.32   80:32507/TCP,443:32114/TCP   11m
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
