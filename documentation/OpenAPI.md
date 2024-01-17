# Swagger / OpenAPI Integration

## Generate and Update

Start Theia.cloud service from IDE (which starts quarkus in dev mode).

Access generated specs at <http://localhost:8081/q/openapi?format=json>

Copy the results to [here](./openapi.json) and replace the securitySchemes at the bottom with this:

```json
"securitySchemes" : {
  "SecurityScheme" : {
    "type" : "oauth2",
    "description" : "Authentication",
    "flows" : {
      "implicit" : {
      }
    }
  }
}
```

## Explore API with Swagger

Download and run the latest `swagger-ui` image:

```bash
docker pull swaggerapi/swagger-ui
docker run -p 80:8080 swaggerapi/swagger-ui
```

Then browse to <http://localhost/> and explore the spec from here: <https://raw.githubusercontent.com/eclipsesource/theia-cloud/main/documentation/openapi.json>

## Generate Typescript API

You can generate the typescript client used in the common package.

Install the `openapi-generator-cli`:

```bash
npm install @openapitools/openapi-generator-cli -g
```

Use the `openapi-generator-cli` from the root of this repository:

```bash
openapi-generator-cli generate -g typescript-axios -i ./documentation/openapi.json -o node/common/src/client/ --additional-properties=supportsES6=true,typescriptThreePlus=true --skip-validate-spec
```
