# Demo Internal

## Docker

Build and push the Theia Demo application with:

```bash
docker build -t theia-cloud-demo -f demo/dockerfiles/demo-theia-docker/Dockerfile demo/dockerfiles/demo-theia-docker/.
docker tag theia-cloud-demo:latest gcr.io/kubernetes-238012/theia-cloud-demo:latest
docker push gcr.io/kubernetes-238012/theia-cloud-demo:latest
```

Build and push the Landing page with:

```bash
docker build -t theia-cloud-landing-page -f dockerfiles/landing-page/Dockerfile .
docker tag theia-cloud-landing-page:latest gcr.io/kubernetes-238012/theia-cloud-landing-page:latest
docker push gcr.io/kubernetes-238012/theia-cloud-landing-page:latest

```

Build and push the workspace REST service with:

```bash
docker build -t theia-cloud-workspace -f dockerfiles/workspace/Dockerfile .
docker tag theia-cloud-workspace:latest gcr.io/kubernetes-238012/theia-cloud-workspace:latest
docker push gcr.io/kubernetes-238012/theia-cloud-workspace:latest
```

Build and push the operator with:

```bash
docker build -t theia-cloud-operator -f dockerfiles/operator/Dockerfile .
docker tag theia-cloud-operator:latest gcr.io/kubernetes-238012/theia-cloud-operator:latest
docker push gcr.io/kubernetes-238012/theia-cloud-operator:latest
```

## Kubernetes

```bash
# Install custom resource definitions
kubectl apply -f k8s/operator-k8s-yaml/template-spec-resource.yaml
kubectl apply -f k8s/operator-k8s-yaml/workspace-spec-resource.yaml

# Adjust demo/k8s/demo-k8s-yaml/theia-template-spec.yaml
# Then create the template
kubectl apply -f demo/k8s/demo-k8s-yaml/theia-template-spec.yaml

#Create a new namespace and switch to it
kubectl create namespace theiacloud
kubectl config set-context --current --namespace=theiacloud

# create service account for the workspace service
kubectl create serviceaccount workspace-api-service-account -n theiacloud

# create cluster role and role binding
kubectl apply -f k8s/workspace-k8s-yaml/workspace-role.yaml

# update app id in k8s/workspace-k8s-yaml/workspace-configmap.yaml to a custom value
# then create the configmap 
kubectl apply -f k8s/workspace-k8s-yaml/workspace-configmap.yaml

# Create workspace service deployment and service
kubectl apply -f k8s/workspace-k8s-yaml/workspace.yaml

# Update k8s/landing-page-k8s-yaml/landing-page-config-map.yaml
# then create the config map
kubectl apply -f k8s/landing-page-k8s-yaml/landing-page-config-map.yaml

# Create landing page service and deployment
kubectl apply -f k8s/landing-page-k8s-yaml/landing-page.yaml

# Update hostnames/static-ip (only available on Google) in k8s/ingress-k8s-yaml/ingress.yaml
# Create ingress to expose workspace service and webpage
kubectl apply -f k8s/ingress-k8s-yaml/ingress.yaml

# Create service account for operator
kubectl create serviceaccount operator-api-service-account -n theiacloud

# create cluster role and cluster role binding
kubectl apply -f k8s/operator-k8s-yaml/operator-role.yaml

# adjust the args in k8s/operator-k8s-yaml/operator.yaml if desired
# create operator deployment
kubectl apply -f k8s/operator-k8s-yaml/operator.yaml
```
