# Swagger / OpenAPI

## Generate and Update

Start Theia.cloud service from IDE (which starts quarkus in dev mode).

Access generated specs at <http://localhost:8081/q/openapi?format=json>

Copy the results [here](openapi.json) and remove the `authorizationUrl`, `tokenUrl`, and `refreshUrl` properties from the OAuth2 security scheme at the bottom.
Afterwards, it should look like this:

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

## Exploring API

```bash
docker pull swaggerapi/swagger-ui
docker run -p 80:8080 swaggerapi/swagger-ui
```

Then browse to <http://localhost/> and explore the spec from here: <https://raw.githubusercontent.com/eclipsesource/theia-cloud/main/documentation/openapi.json>

## Generate Typescript API

You can generate the typescript client used in the common package by using the following command from the root of this repository:

```bash
openapi-generator-cli generate -g typescript-axios -i ./documentation/openapi.json -o node/common/src/client/ --additional-properties=supportsES6=true,typescriptThreePlus=true --skip-validate-spec
```

If the command is not found, you may install the generator with:

```bash
npm install @openapitools/openapi-generator-cli -g
```
