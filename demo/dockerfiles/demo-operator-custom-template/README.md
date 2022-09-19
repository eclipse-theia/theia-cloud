# Operator with custom templates

## Building

```bash
docker build -t theia-cloud-operator:template -f demo/dockerfiles/demo-operator-custom-template/Dockerfile .
docker tag theia-cloud-operator:template theiacloud/theia-cloud-operator:template
docker push theiacloud/theia-cloud-operator:template
```

## Testing

This custom operator adds a further container launching an nginx-server hosting a hello world page on port 80.
Moreover the default user for the demo theia is overwritten to use the root user. This allows to install tools like curl to test our new container as well.

E.g. you may now run

```bash
apt install curl
curl localhost:80
```

and access the hello world server.
