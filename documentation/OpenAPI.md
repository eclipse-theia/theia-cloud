# Swagger / OpenAPI Integration

## Generate and Update

Start Theia Cloud service from IDE (which starts quarkus in dev mode).

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

Then browse to <http://localhost/> and explore the spec from here: <https://raw.githubusercontent.com/eclipse-theia/theia-cloud/main/documentation/openapi.json>

Or explore it from the locally started service: <http://localhost:8081/q/openapi?format=json>.

## Generate Typescript API

You can generate the typescript client used in the common package.

Install the `openapi-generator-cli`:

```bash
npm install @openapitools/openapi-generator-cli -g
openapi-generator-cli version-manager set 7.8.0
```

Use the `openapi-generator-cli` from the root of this repository:

```bash
openapi-generator-cli generate -g typescript-axios -i ./documentation/openapi.json -o node/common/src/client/ --additional-properties=supportsES6=true,typescriptThreePlus=true --skip-validate-spec
```

## Generate API Documentation

Documentation for the service API can be generated as markdown files.

If not already done for generating the typescript API, install the `openapi-generator-cli`:

```bash
npm install @openapitools/openapi-generator-cli -g
openapi-generator-cli version-manager set 7.8.0
```

Use the `openapi-generator-cli` from the root of this repository:

```bash
openapi-generator-cli generate -g markdown -i ./documentation/openapi.json -o ./documentation/api --skip-validate-spec
```

> [!TIP]
> The Open API generator supports generating code and documentation in various languages.
> Execute `openapi-generator-cli list` to see all of them.
