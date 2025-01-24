# Run and Develop CI E2E Tests Locally

See the `e2e-tests.yml` Workflow file for all steps. Below will just be a short summary with adaptions that need to be done locally.

Start Minikube, enable the required plugins, and patch the ingress.

```sh
minikube start --memory=8192 --cpus=4 --driver=virtualbox

minikube addons enable dashboard
minikube addons enable default-storageclass
minikube addons enable ingress
minikube addons enable metrics-server

kubectl -n ingress-nginx patch cm ingress-nginx-controller --patch '{"data":{"allow-snippet-annotations":"true"}}'
kubectl -n ingress-nginx delete pod -l app.kubernetes.io/name=ingress-nginx
```

Adapt your environment so that all docker images are built in minikube.
Build all Theia Cloud docker images + Demos with tag `minikube-ci-e2e`, e.g. `theiacloud/theia-cloud-service:minikube-ci-e2e`.
The build commands need to be executed from the root of the repository.

```sh
eval $(minikube docker-env)

docker build --no-cache -t theiacloud/theia-cloud-service:minikube-ci-e2e -f dockerfiles/service/Dockerfile .
docker build --no-cache -t theiacloud/theia-cloud-operator:minikube-ci-e2e -f dockerfiles/operator/Dockerfile .
docker build --no-cache -t theiacloud/theia-cloud-landing-page:minikube-ci-e2e -f dockerfiles/landing-page/Dockerfile .
docker build --no-cache -t theiacloud/theia-cloud-wondershaper:minikube-ci-e2e -f dockerfiles/wondershaper/Dockerfile .
docker build --no-cache -t theiacloud/theia-cloud-conversion-webhook:minikube-ci-e2e -f dockerfiles/conversion-webhook/Dockerfile .
docker build --no-cache -t theiacloud/theia-cloud-demo:latest -f demo/dockerfiles/demo-theia-docker/Dockerfile demo/dockerfiles/demo-theia-docker/.
docker tag theiacloud/theia-cloud-demo:latest theiacloud/theia-cloud-demo:minikube-ci-e2e
docker build --no-cache -t theiacloud/theia-cloud-activity-demo:minikube-ci-e2e -f demo/dockerfiles/demo-theia-monitor-vscode/Dockerfile demo/dockerfiles/demo-theia-monitor-vscode/.
docker build --no-cache -t theiacloud/theia-cloud-activity-demo-theia:minikube-ci-e2e -f demo/dockerfiles/demo-theia-monitor-theia/Dockerfile .
```

Get the IP of Minikube's ingress controller

```sh
export INGRESS_HOST=$(minikube ip)
export MATRIX_PATHS=false
export MATRIX_EPHEMERAL=true
export MATRIX_KEYCLOAK=true
```

Run Terraform

```sh
terraform init
terraform apply \
    -var="ingress_ip=$INGRESS_HOST" \
    -var="use_paths=$MATRIX_PATHS" \
    -var="use_ephemeral_storage=$MATRIX_EPHEMERAL" \
    -var="enable_keycloak=$MATRIX_KEYCLOAK" \
    -auto-approve
```

Run Tests

```sh
npm run ui-tests -w e2e-tests
```
