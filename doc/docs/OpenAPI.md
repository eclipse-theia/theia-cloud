# Swagger / OpenAPI

## Generate and Update

Start Theia.cloud service from IDE (which starts quarkus in dev mode).

Access generated specs at http://localhost:8081/q/openapi?format=json

Commit results [here](openapi.json).

## Exploring API

```bash
docker pull swaggerapi/swagger-ui
docker run -p 80:8080 swaggerapi/swagger-ui
```

Then browse to http://localhost/ and explore the spec from here: https://raw.githubusercontent.com/eclipsesource/theia-cloud/main/doc/docs/openapi.json