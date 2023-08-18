# Building Internal

This page contains the build instructions including tagging and pushing the images to dockerhub.\
This documentation does not include secrets for pushing.

## Docker

Build and push the wondershaper init-container with:

```bash
docker build -t theiacloud/theia-cloud-wondershaper:latest -f dockerfiles/wondershaper/Dockerfile .
docker push theiacloud/theia-cloud-wondershaper:latest
```

Build and push the Theia Cloud conversion hook with:

```bash
docker build --no-cache -t theiacloud/theia-cloud-conversion:latest -f dockerfiles/conversion-hook/Dockerfile .
docker push theiacloud/theia-cloud-conversion:latest
```

Build and push the Theia Demo application with:

```bash
docker build -t theiacloud/theia-cloud-demo:latest -f demo/dockerfiles/demo-theia-docker/Dockerfile demo/dockerfiles/demo-theia-docker/.
docker push theiacloud/theia-cloud-demo:latest
```

Build and push the Theia Activity Tracker VSCode Demo application with:

```bash
docker tag theiacloud/theia-cloud-demo:latest theiacloud/theia-cloud-demo
docker build -t theiacloud/theia-cloud-activity-demo:latest -f demo/dockerfiles/demo-theia-monitor-vscode/Dockerfile demo/dockerfiles/demo-theia-monitor-vscode/.
docker push theiacloud/theia-cloud-activity-demo:latest
```

Build and push the Theia Activity Tracker Theia Extension Demo application with:

```bash
docker build -t theiacloud/theia-cloud-activity-demo-theia:latest -f demo/dockerfiles/demo-theia-monitor-theia/Dockerfile demo/dockerfiles/demo-theia-monitor-theia/.
docker push theiacloud/theia-cloud-activity-demo-theia:latest
```

Build and push the Landing page with:

```bash
docker build -t theiacloud/theia-cloud-landing-page:latest -f dockerfiles/landing-page/Dockerfile .
docker push theiacloud/theia-cloud-landing-page:latest
```

Build and push the Try Now page with:

```bash
docker build -t theiacloud/theia-cloud-try-now-page:latest -f dockerfiles/try-now-page/Dockerfile .
docker push theiacloud/theia-cloud-try-now-page:latest
```

Build and push the Theia.cloud REST service with:

```bash
docker build --no-cache -t theiacloud/theia-cloud-service:latest -f dockerfiles/service/Dockerfile .
docker push theiacloud/theia-cloud-service:latest
```

Build and push the operator with:

```bash
docker build --no-cache -t theiacloud/theia-cloud-operator:latest -f dockerfiles/operator/Dockerfile .
docker push theiacloud/theia-cloud-operator:latest
```
