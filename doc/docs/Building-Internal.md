# Building Internal

This page contains the build instructions including tagging and pushing the images to dockerhub.\
This documentation does not include secrets for pushing.

## Docker

Build and push the wondershaper init-container with:

```bash
docker build -t theia-cloud-wondershaper:latest -f dockerfiles/wondershaper/Dockerfile .
docker tag theia-cloud-wondershaper:latest theiacloud/theia-cloud-wondershaper:latest
docker push theiacloud/theia-cloud-wondershaper:latest
```

Build and push the Theia Demo application with:

```bash
docker build -t theia-cloud-demo:latest -f demo/dockerfiles/demo-theia-docker/Dockerfile demo/dockerfiles/demo-theia-docker/.
docker tag theia-cloud-demo:latest theiacloud/theia-cloud-demo:latest
docker push theiacloud/theia-cloud-demo:latest
```

Build and push the Landing page with:

```bash
docker build -t theia-cloud-landing-page:latest -f dockerfiles/landing-page/Dockerfile .
docker tag theia-cloud-landing-page:latest theiacloud/theia-cloud-landing-page:latest
docker push theiacloud/theia-cloud-landing-page:latest

```

Build and push the Try Now page with:

```bash
docker build -t theia-cloud-try-now-page:latest -f dockerfiles/try-now-page/Dockerfile .
docker tag theia-cloud-try-now-page:latest theiacloud/theia-cloud-try-now-page:latest
docker push theiacloud/theia-cloud-try-now-page:latest

```

Build and push the Theia.cloud REST service with:

```bash
docker build -t theia-cloud-service:latest -f dockerfiles/service/Dockerfile .
docker tag theia-cloud-service:latest theiacloud/theia-cloud-service:latest
docker push theiacloud/theia-cloud-service:latest
```

Build and push the operator with:

```bash
docker build -t theia-cloud-operator:latest -f dockerfiles/operator/Dockerfile .
docker tag theia-cloud-operator:latest theiacloud/theia-cloud-operator:latest
docker push theiacloud/theia-cloud-operator:latest
```
