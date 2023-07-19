# Building Internal

This page contains the build instructions including tagging and pushing the images to dockerhub.\
This documentation does not include secrets for pushing.

## Docker

Build and push the wondershaper init-container with:

```bash
docker build -t theiacloud/theia-cloud-wondershaper:osweek23-1 -f dockerfiles/wondershaper/Dockerfile .
docker push theiacloud/theia-cloud-wondershaper:osweek23-1
```

Build and push the Theia Demo application with:

```bash
docker build -t theiacloud/theia-cloud-demo:osweek23-1 -f demo/dockerfiles/demo-theia-docker/Dockerfile demo/dockerfiles/demo-theia-docker/.
docker push theiacloud/theia-cloud-demo:osweek23-1
```

Build and push the Theia Activity Tracker Demo application with:

```bash
docker tag theiacloud/theia-cloud-demo:osweek23-1 theiacloud/theia-cloud-demo
docker build -t theiacloud/theia-cloud-activity-demo:osweek23-1 -f demo/dockerfiles/demo-theia-monitor-vscode/Dockerfile demo/dockerfiles/demo-theia-monitor-vscode/.
docker push theiacloud/theia-cloud-activity-demo:osweek23-1
```

Build and push the Landing page with:

```bash
docker build -t theiacloud/theia-cloud-landing-page:osweek23-1 -f dockerfiles/landing-page/Dockerfile .
docker push theiacloud/theia-cloud-landing-page:osweek23-1

```

Build and push the Try Now page with:

```bash
docker build -t theiacloud/theia-cloud-try-now-page:osweek23-1 -f dockerfiles/try-now-page/Dockerfile .
docker push theiacloud/theia-cloud-try-now-page:osweek23-1

```

Build and push the Theia.cloud REST service with:

```bash
docker build -t theiacloud/theia-cloud-service:osweek23-1 -f dockerfiles/service/Dockerfile .
docker push theiacloud/theia-cloud-service:osweek23-1
```

Build and push the operator with:

```bash
docker build -t theiacloud/theia-cloud-operator:osweek23-1 -f dockerfiles/operator/Dockerfile .
docker push theiacloud/theia-cloud-operator:osweek23-1
```
