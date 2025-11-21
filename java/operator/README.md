# Theia Cloud Operator

This folder contains the Java projects for the Theia Cloud operator, including:

- **Default Theia Cloud Operator**: Provides a default implementation of the operator.
- **Theia Cloud Operator Library**: The key functionality for the Theia Cloud Operator, to be used in your implementation.

## Projects

### Default Theia Cloud Operator

This project implements the default Kubernetes Operator for Theia Cloud.

#### Build and Run

```sh
mvn clean install
java -jar target/defaultoperator-1.2.0-SNAPSHOT-jar-with-dependencies.jar
```

#### Debugging the Default Theia Cloud Operator

To debug the Default Theia Cloud Operator, follow these steps:

- **Set the Replicas**:

  - Set the replicas of the operator in your Theia Cloud installation to 0.

- **Start the Debug Config**:

  - Launch the debug configuration with the appropriate flags for debugging.
  - There are specific configurations for the [terraform test-configurations](../../terraform/test-configurations/) available.
  - The configurations will prompt you for your Minikube IP address. To find out the IP address, run the following command in your terminal:

```sh
     minikube ip
```

_Note_: Ensure your local `kubectl` is set to the correct namespace (e.g., `theia-cloud`), as the debuggable operator will interact with your local Kubernetes cluster.

### Theia Cloud Operator Library

A shared library for building Theia Cloud Kubernetes operators.
