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

#### Customizing injected dependencies

Override `customizeModule(Module)` in your launcher to replace bindings with
`Modules.override(original).with(overrides)`. The hook receives the module returned by
`createModule()` and must return the effective, non-null module used to create the injector.
It runs once per injector-creation/startup invocation, including when the operator starts as
leader. Its default implementation returns the original module unchanged.

For example, the following launcher injects a client that logs unsuccessful session IP lookups:

```java
package example;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.DefaultTheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.defaultoperator.DefaultTheiaCloudOperatorLauncher;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.util.Modules;

import io.fabric8.kubernetes.client.NamespacedKubernetesClient;

public class CustomTheiaCloudOperatorLauncher extends DefaultTheiaCloudOperatorLauncher {

    public static void main(String[] args) throws InterruptedException {
        new CustomTheiaCloudOperatorLauncher().runMain(args);
    }

    @Override
    protected Module customizeModule(Module module) {
        return Modules.override(module).with(new AbstractModule() {
            @Provides
            @Singleton
            TheiaCloudClient provideCustomTheiaCloudClient(NamespacedKubernetesClient client) {
                return new LoggingTheiaCloudClient(client);
            }
        });
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

The provider belongs to a separate override module. Do not override an inherited `@Provides`
method or add a competing provider to the original module: Guice rejects those approaches.
The custom provider keeps the client singleton-scoped and receives the existing Kubernetes
client through injection.

Save this example as `example/CustomTheiaCloudOperatorLauncher.java` in your downstream
application, with the default operator and its dependencies on the classpath. Launch
`example.CustomTheiaCloudOperatorLauncher`, or configure your application's executable JAR to
use it as the main class. The example declares its own `main()` because the inherited static
entry point constructs `DefaultTheiaCloudOperatorLauncher` directly. The upstream default
operator JAR continues to use its original main class.

This example is also compiled and tested in
[`src/test/java/example/CustomTheiaCloudOperatorLauncher.java`](org.eclipse.theia.cloud.defaultoperator/src/test/java/example/CustomTheiaCloudOperatorLauncher.java).
