# Building Internal

This page contains the build instructions including tagging and pushing the images to dockerhub.\
This documentation does not include secrets for pushing.

## Docker

Build and push the wondershaper init-container with:

```bash
docker build -t theia-cloud-wondershaper -f dockerfiles/wondershaper/Dockerfile .
docker tag theia-cloud-wondershaper:latest theiacloud/theia-cloud-wondershaper:latest
docker push theiacloud/theia-cloud-wondershaper:latest
```

Build and push the Theia Demo application with:

```bash
docker build -t theia-cloud-demo -f demo/dockerfiles/demo-theia-docker/Dockerfile demo/dockerfiles/demo-theia-docker/.
docker tag theia-cloud-demo:latest theiacloud/theia-cloud-demo:latest
docker push theiacloud/theia-cloud-demo:latest
```

Build and push the Landing page with:

```bash
docker build -t theia-cloud-landing-page -f dockerfiles/landing-page/Dockerfile .
docker tag theia-cloud-landing-page:latest theiacloud/theia-cloud-landing-page:latest
docker push theiacloud/theia-cloud-landing-page:latest

```

Build and push the Try Now page with:

```bash
docker build -t theia-cloud-try-now-page -f dockerfiles/try-now-page/Dockerfile .
docker tag theia-cloud-try-now-page:latest theiacloud/theia-cloud-try-now-page:latest
docker push theiacloud/theia-cloud-try-now-page:latest

```

Build and push the workspace REST service with:

```bash
docker build -t theia-cloud-workspace -f dockerfiles/workspace/Dockerfile .
docker tag theia-cloud-workspace:latest theiacloud/theia-cloud-workspace:latest
docker push theiacloud/theia-cloud-workspace:latest
```

Build and push the operator with:

```bash
docker build -t theia-cloud-operator -f dockerfiles/operator/Dockerfile .
docker tag theia-cloud-operator:latest theiacloud/theia-cloud-operator:latest
docker push theiacloud/theia-cloud-operator:latest
```
