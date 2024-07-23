# Building

Besides the Operator, REST-Service, and the landing page, Theia Cloud also provides a few simple Theia-based demo applications and an init-container running wondershaper which may enable limiting the network bandwidth of a running application.

**Note:** all commands are from the root of the repository.

## REST service

```bash
docker build --no-cache -t theia-cloud-service -f dockerfiles/service/Dockerfile .
```

## Operator

```bash
docker build --no-cache -t theia-cloud-operator -f dockerfiles/operator/Dockerfile .
```

## Landing page

```bash
docker build -t theia-cloud-landing-page -f dockerfiles/landing-page/Dockerfile .
```

## Wondershaper

```bash
docker build -t theia-cloud-wondershaper -f dockerfiles/wondershaper/Dockerfile .
```

## CRD conversion webhook

```bash
docker build -t theia-cloud-conversion-webhook -f dockerfiles/conversion-webhook/Dockerfile .
```

## Demo applications

### Theia demo

```bash
docker build -t theia-cloud-demo -f demo/dockerfiles/demo-theia-docker/Dockerfile demo/dockerfiles/demo-theia-docker/.
```

### Theia demo with Activity Tracker (VSCode extension)

**Note:** requires the [Theia Demo](#theia-demo).

```bash
docker tag theiacloud/theia-cloud-demo:latest theiacloud/theia-cloud-demo
docker build -t theiacloud/theia-cloud-activity-demo:latest -f demo/dockerfiles/demo-theia-monitor-vscode/Dockerfile demo/dockerfiles/demo-theia-monitor-vscode/.
```

### Theia demo with Activity Tracker (Theia extension)

```bash
docker build -t theiacloud/theia-cloud-activity-demo-theia:latest -f demo/dockerfiles/demo-theia-monitor-theia/Dockerfile .
```
