/********************************************************************************
 * Copyright (C) 2026 EclipseSource and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License v. 2.0 are satisfied: GNU General Public License, version 2
 * with the GNU Classpath Exception which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 ********************************************************************************/
package example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.defaultoperator.DefaultTheiaCloudOperatorModule;
import org.eclipse.theia.cloud.operator.TheiaCloudOperator;
import org.eclipse.theia.cloud.operator.TheiaCloudOperatorArguments;
import org.eclipse.theia.cloud.operator.di.AbstractTheiaCloudOperatorModule;
import org.junit.jupiter.api.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;

class CustomTheiaCloudOperatorLauncherTests {

    @Test
    void documentedLauncherReplacesClientFromAnotherPackage() {
        ExampleLauncher launcher = new ExampleLauncher();
        TheiaCloudOperatorArguments arguments = launcher.createArguments(
                new String[] { "--instancesHost", "example.test", "--cloudProvider", "K8S" });
        try (NamespacedKubernetesClient kubernetesClient = offlineClient()) {
            // Replace the CRD-validating provider independently of the documented hook.
            Module module = Modules.override(launcher.effectiveModule(arguments)).with(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(NamespacedKubernetesClient.class).toInstance(kubernetesClient);
                }
            });
            Injector injector = Guice.createInjector(module);
            TheiaCloudClient client = injector.getInstance(TheiaCloudClient.class);

            assertInstanceOf(CustomTheiaCloudOperatorLauncher.LoggingTheiaCloudClient.class, client);
            assertSame(client, injector.getInstance(TheiaCloudClient.class));
            assertSame(kubernetesClient, client.kubernetes());
        }
    }

    @Test
    void documentedLauncherCustomizesInheritedLeaderStartup() {
        try (NamespacedKubernetesClient kubernetesClient = offlineClient()) {
            StartupLauncher launcher = new StartupLauncher(kubernetesClient);
            TheiaCloudOperatorArguments arguments = launcher.createArguments(
                    new String[] { "--instancesHost", "example.test", "--cloudProvider", "K8S" });

            launcher.startAsLeader(arguments);

            assertEquals(1, launcher.operator.starts);
            assertInstanceOf(CustomTheiaCloudOperatorLauncher.LoggingTheiaCloudClient.class,
                    launcher.operator.client);
            assertSame(kubernetesClient, launcher.operator.client.kubernetes());
        }
    }

    private static NamespacedKubernetesClient offlineClient() {
        Config config = new ConfigBuilder(Config.empty()).withMasterUrl("http://127.0.0.1:1")
                .withNamespace("test").build();
        return new KubernetesClientBuilder().withConfig(config).build().adapt(NamespacedKubernetesClient.class);
    }

    private static class StartupLauncher extends CustomTheiaCloudOperatorLauncher {

        private final NamespacedKubernetesClient kubernetesClient;
        private final RecordingOperator operator = new RecordingOperator();

        StartupLauncher(NamespacedKubernetesClient kubernetesClient) {
            this.kubernetesClient = kubernetesClient;
        }

        @Override
        public AbstractTheiaCloudOperatorModule createModule(TheiaCloudOperatorArguments arguments) {
            return new DefaultTheiaCloudOperatorModule(arguments) {
                @Override
                protected Class<? extends TheiaCloudOperator> bindTheiaCloudOperator() {
                    // A missing hook must fail an assertion without starting Kubernetes watches.
                    return InertOperator.class;
                }
            };
        }

        @Override
        protected Module customizeModule(Module module) {
            return Modules.override(super.customizeModule(module)).with(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(NamespacedKubernetesClient.class).toInstance(kubernetesClient);
                    bind(TheiaCloudOperator.class).toInstance(operator);
                }
            });
        }

        void startAsLeader(TheiaCloudOperatorArguments arguments) {
            startOperatorAsLeader(arguments);
        }
    }

    public static class InertOperator implements TheiaCloudOperator {
        @Override
        public void start() {
        }
    }

    private static class RecordingOperator implements TheiaCloudOperator {
        @Inject
        private TheiaCloudClient client;

        private int starts;

        @Override
        public void start() {
            starts++;
        }
    }

    private static class ExampleLauncher extends CustomTheiaCloudOperatorLauncher {

        Module effectiveModule(TheiaCloudOperatorArguments arguments) {
            // Existing callers can still use the original createModule return type.
            AbstractTheiaCloudOperatorModule original = createModule(arguments);
            return customizeModule(original);
        }
    }
}
