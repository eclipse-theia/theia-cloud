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
java -jar target/defaultoperator-1.3.0-SNAPSHOT.jar
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

#### Customizing the injected client

Override `createTheiaCloudClient(NamespacedKubernetesClient)` in your operator module
to supply a custom client. The existing Guice provider delegates to this factory and
keeps the client a singleton within each injector. Leave the inherited `@Provides`
method unchanged, and do not annotate the factory with `@Provides`.

For example, the following launcher uses a custom `DefaultTheiaCloudOperatorModule`
that adds logging when a session's ClusterIP cannot be found:

```java
package example;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.DefaultTheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.defaultoperator.DefaultTheiaCloudOperatorLauncher;
import org.eclipse.theia.cloud.defaultoperator.DefaultTheiaCloudOperatorModule;
import org.eclipse.theia.cloud.operator.TheiaCloudOperatorArguments;
import org.eclipse.theia.cloud.operator.di.AbstractTheiaCloudOperatorModule;

import io.fabric8.kubernetes.client.NamespacedKubernetesClient;

public class CustomTheiaCloudOperatorLauncher extends DefaultTheiaCloudOperatorLauncher {

    public static void main(String[] args) throws InterruptedException {
        new CustomTheiaCloudOperatorLauncher().runMain(args);
    }

    @Override
    public AbstractTheiaCloudOperatorModule createModule(TheiaCloudOperatorArguments arguments) {
        return new CustomTheiaCloudOperatorModule(arguments);
    }

    public static class CustomTheiaCloudOperatorModule extends DefaultTheiaCloudOperatorModule {

        public CustomTheiaCloudOperatorModule(TheiaCloudOperatorArguments arguments) {
            super(arguments);
        }

        @Override
        protected TheiaCloudClient createTheiaCloudClient(NamespacedKubernetesClient client) {
            return new LoggingTheiaCloudClient(client);
        }
    }

    public static class LoggingTheiaCloudClient extends DefaultTheiaCloudClient {

        private static final Logger LOGGER = LogManager.getLogger(LoggingTheiaCloudClient.class);

        public LoggingTheiaCloudClient(NamespacedKubernetesClient client) {
            super(client);
        }

        @Override
        public Optional<String> getClusterIPFromSessionName(String sessionName) {
            Optional<String> ip = super.getClusterIPFromSessionName(sessionName);
            if (ip.isEmpty()) {
                LOGGER.warn("No ClusterIP found for session {}", sessionName);
            }
            return ip;
        }
    }
}
```

Save this example as `example/CustomTheiaCloudOperatorLauncher.java` in your downstream
application, with the default operator and its dependencies on the classpath. Launch
`example.CustomTheiaCloudOperatorLauncher`, or configure your application's executable JAR to
use it as the main class. The example declares its own `main()` because the inherited static
entry point constructs `DefaultTheiaCloudOperatorLauncher` directly. The upstream default
operator JAR continues to use its original main class.

This example is also compiled and tested in
[`src/test/java/example/CustomTheiaCloudOperatorLauncher.java`](org.eclipse.theia.cloud.defaultoperator/src/test/java/example/CustomTheiaCloudOperatorLauncher.java).
