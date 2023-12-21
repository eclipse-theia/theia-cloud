# Building

Besides the Operator, Theia.cloud-REST-Service, and the Dashboard, Theia.Cloud also has a simple Theia-based demo application and an init-container running wondershaper which may enable limiting the network bandwidth of a running application.

```bash
# Build the wondershaper init-container.
docker build -t theia-cloud-wondershaper -f dockerfiles/wondershaper/Dockerfile .

# Build the conversion hook.
docker build -t theia-cloud-conversion -f dockerfiles/conversion-hook/Dockerfile .

# Build the Theia Demo application with:
docker build -t theia-cloud-demo -f demo/dockerfiles/demo-theia-docker/Dockerfile demo/dockerfiles/demo-theia-docker/.

# Build the Landing page/Dashboard with:
docker build -t theia-cloud-landing-page -f dockerfiles/landing-page/Dockerfile .

# Build the Try Now page with:
docker build -t theia-cloud-try-now-page -f dockerfiles/try-now-page/Dockerfile .

# Build the Theia.cloud REST service with:
docker build -t theia-cloud-service -f dockerfiles/service/Dockerfile .

# Build the operator with:
docker build -t theia-cloud-operator -f dockerfiles/operator/Dockerfile .
```
