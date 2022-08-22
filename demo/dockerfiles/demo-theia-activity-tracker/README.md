# Demo for Theia Activity Tracker Extension

## Building

Build and push the Theia Activity Tracker Demo application with:

```bash
docker build -t theia-cloud-activity-demo:latest -f demo/dockerfiles/demo-theia-activity-tracker/Dockerfile demo/dockerfiles/demo-theia-activity-tracker/.
docker tag theia-cloud-activity-demo:latest theiacloud/theia-cloud-activity-demo:latest
docker push theiacloud/theia-cloud-activity-demo:latest
```
