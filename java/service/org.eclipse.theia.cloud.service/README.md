# service Project

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/> .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_** Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Debug the service

When you have the service running in dev mode you can use the `Attach to Service` debug config to debug the service.

Check the [OpenAPI documentation](../../../documentation/OpenAPI.md) to see how you can send requests to the service.

_Note_: The landing page will not communicate with this service, as it will be hosted on another address.
Checkout the [testing-page](../../../node/testing-page/README.md) to test requests against the server.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/service-1.1.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Provided Code

### RESTEasy JAX-RS

Easily start your RESTful Web Services

[Related guide section...](https://quarkus.io/guides/getting-started#the-jax-rs-resources)

## Unit tests

Unit tests use the Quarkus test infrastructure.

When running tests from an IDE, the following VM argument might need to be set to enable logging during tests (this is pre-configured in the launch config):

```bash
-Djava.util.logging.manager=org.jboss.logmanager.LogManager
```

See [here](https://quarkus.io/guides/getting-started-testing#test-from-ide) for more information.

### Conventions

For unit test naming, we use the naming pattern `MethodName_StateUnderTest_ExpectedBehavior`, e.g. `authenticate_anonymousUser_throwForbidden`.

### Further links

- [Getting started with Quarkus testing](https://quarkus.io/guides/getting-started-testing)
- [Security testing (configure auth for tests)](https://quarkus.io/guides/security-testing)
