# Theia Cloud

## Housekeeping

`kubectl delete deployments --all -n theiacloud && kubectl delete services --all -n theiacloud && kubectl delete ingresses --all -n theiacloud && kubectl delete configmap coffee-editor-config-1 -n theiacloud && kubectl delete configmap coffee-editor-config-2 -n theiacloud && kubectl delete configmap coffee-editor-emailconfig-1 -n theiacloud && kubectl delete configmap coffee-editor-emailconfig-2 -n theiacloud`

## Running

### Local Setup with Minikube

#### Initial Setup

Remove old Minikube clusters with `minikube delete`.

Start a new Minikube instance:\
`minikube start --addons=ingress --vm=true --memory=8192 --cpus=6 --disk-size=75g`


##### Install Keycloak

Based on https://www.keycloak.org/getting-started/getting-started-kube

Create Keycloak instance in minikube:

```bash
kubectl create -f https://raw.githubusercontent.com/keycloak/keycloak-quickstarts/latest/kubernetes-examples/keycloak.yaml

wget -q -O - https://raw.githubusercontent.com/keycloak/keycloak-quickstarts/latest/kubernetes-examples/keycloak-ingress.yaml | \
sed "s/KEYCLOAK_HOST/keycloak.$(minikube ip).nip.io/" | \
kubectl create -f -

KEYCLOAK_URL=https://keycloak.$(minikube ip).nip.io/auth &&
echo "" &&
echo "Keycloak:                 $KEYCLOAK_URL" &&
echo "Keycloak Admin Console:   $KEYCLOAK_URL/admin" &&
echo "Keycloak Account Console: $KEYCLOAK_URL/realms/myrealm/account" &&
echo ""
```

#### Administrate Keycloak

Follow https://oauth2-proxy.github.io/oauth2-proxy/docs/configuration/oauth_provider/#keycloak-oidc-auth-provider

Valid Redirect URL: `https://*.192.168.39.3.nip.io/oauth2/callback` (IP comes from `minikube ip`)

##### Run Theia.Cloud

Update `host: 192.168.39.3.nip.io` in `k8s/operator-k8s-yaml/template-spec-resource.yaml` with the IP received from `minikube ip`.

Create a new namespace and switch to it:\
`kubectl create namespace theiacloud`\
`kubectl config set-context --current --namespace=theiacloud`

Install custom resource definitions (from git root directory):\
`kubectl apply -f k8s/operator-k8s-yaml/template-spec-resource.yaml`\
`kubectl apply -f k8s/operator-k8s-yaml/workspace-spec-resource.yaml`

Launch operator from Eclipse.

Install sample template (from git root directory):\
`kubectl apply -f demo/k8s/demo-k8s-yaml/coffee-template-spec.yaml`\
`kubectl apply -f demo/k8s/demo-k8s-yaml/coffee-workspace-1.yaml`\
`kubectl apply -f demo/k8s/demo-k8s-yaml/coffee-workspace-2.yaml`\
`kubectl apply -f demo/k8s/demo-k8s-yaml/configmap-oauth2proxy-keycloak.yaml`\
`kubectl apply -f demo/k8s/demo-k8s-yaml/configmap-htmlpage.yaml`

# Docker

`docker build -t theia-cloud-operator -f dockerfiles/operator/Dockerfile .`