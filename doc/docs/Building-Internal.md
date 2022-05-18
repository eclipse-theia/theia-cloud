# Building Internal

This page contains the build instructions including tagging and pushing the images to the internal test cluster. There are no secrets in this document.

## Docker

Build and push the wondershaper init-container with:

```bash
docker build -t theia-cloud-wondershaper -f dockerfiles/wondershaper/Dockerfile .
docker tag theia-cloud-wondershaper:latest eu.gcr.io/kubernetes-238012/theia-cloud-wondershaper:latest
docker push eu.gcr.io/kubernetes-238012/theia-cloud-wondershaper:latest
```

Build and push the Theia Demo application with:

```bash
docker build -t theia-cloud-demo -f demo/dockerfiles/demo-theia-docker/Dockerfile demo/dockerfiles/demo-theia-docker/.
docker tag theia-cloud-demo:latest eu.gcr.io/kubernetes-238012/theia-cloud-demo:latest
docker push eu.gcr.io/kubernetes-238012/theia-cloud-demo:latest
```

Build and push the Landing page with:

```bash
docker build -t theia-cloud-landing-page -f dockerfiles/landing-page/Dockerfile .
docker tag theia-cloud-landing-page:latest eu.gcr.io/kubernetes-238012/theia-cloud-landing-page:latest
docker push eu.gcr.io/kubernetes-238012/theia-cloud-landing-page:latest

```

Build and push the Try Now page with:

```bash
docker build -t theia-cloud-try-now-page -f dockerfiles/try-now-page/Dockerfile .
docker tag theia-cloud-try-now-page:latest eu.gcr.io/kubernetes-238012/theia-cloud-try-now-page:latest
docker push eu.gcr.io/kubernetes-238012/theia-cloud-try-now-page:latest

```

Build and push the workspace REST service with:

```bash
docker build -t theia-cloud-workspace -f dockerfiles/workspace/Dockerfile .
docker tag theia-cloud-workspace:latest eu.gcr.io/kubernetes-238012/theia-cloud-workspace:latest
docker push eu.gcr.io/kubernetes-238012/theia-cloud-workspace:latest
```

Build and push the operator with:

```bash
docker build -t theia-cloud-operator -f dockerfiles/operator/Dockerfile .
docker tag theia-cloud-operator:latest eu.gcr.io/kubernetes-238012/theia-cloud-operator:latest
docker push eu.gcr.io/kubernetes-238012/theia-cloud-operator:latest
```
