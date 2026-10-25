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
package org.eclipse.theia.cloud.operator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.eclipse.theia.cloud.common.k8s.client.DefaultTheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.operator.di.AbstractTheiaCloudOperatorModule;
import org.junit.jupiter.api.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.util.Modules;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;

class TheiaCloudOperatorLauncherTests {

    private static final String[] ARGUMENTS = { "--instancesHost", "example.test", "--cloudProvider", "K8S" };

    @Test
    void ordinaryStartup_withoutCustomizationUsesOriginalModule() throws InterruptedException {
        try (NamespacedKubernetesClient client = offlineClient()) {
            Fixture fixture = new Fixture(client);
            TheiaCloudOperatorLauncher launcher = new TheiaCloudOperatorLauncher() {
                @Override
                AbstractTheiaCloudOperatorModule createModule(TheiaCloudOperatorArguments arguments) {
                    return fixture.createModule(arguments);
                }
            };

            launcher.runMain(ARGUMENTS);

            assertEquals(1, fixture.fallbackStarts);
            assertEquals(0, fixture.customizedStarts);
        }
    }

    @Test
    void leaderStartup_withoutCustomizationUsesOriginalModule() {
        try (NamespacedKubernetesClient client = offlineClient()) {
            Fixture fixture = new Fixture(client);
            LeaderElectionTheiaCloudOperatorLauncher launcher = new LeaderElectionTheiaCloudOperatorLauncher() {
                @Override
                public AbstractTheiaCloudOperatorModule createModule(TheiaCloudOperatorArguments arguments) {
                    return fixture.createModule(arguments);
                }
            };

            launcher.startOperatorAsLeader(launcher.createArguments(ARGUMENTS));

            assertEquals(1, fixture.fallbackStarts);
            assertEquals(0, fixture.customizedStarts);
        }
    }

    @Test
    void ordinaryStartup_usesCustomizedModule() throws InterruptedException {
        try (NamespacedKubernetesClient client = offlineClient()) {
            Fixture fixture = new Fixture(client);

            new OrdinaryLauncher(fixture).runMain(ARGUMENTS);

            assertCustomizedStartup(fixture, 1);
        }
    }

    @Test
    void leaderStartup_usesCustomizedModule() {
        try (NamespacedKubernetesClient client = offlineClient()) {
            Fixture fixture = new Fixture(client);

            new LeaderLauncher(fixture).startAsLeader();

            assertCustomizedStartup(fixture, 1);
        }
    }

    @Test
    void ordinaryStartup_customizesOnceForEachInvocation() throws InterruptedException {
        try (NamespacedKubernetesClient client = offlineClient()) {
            Fixture fixture = new Fixture(client);
            OrdinaryLauncher launcher = new OrdinaryLauncher(fixture);
            launcher.runMain(ARGUMENTS);
            assertCustomizedStartup(fixture, 1);
            Module firstModule = fixture.originalModule;
            TheiaCloudClient firstClient = fixture.injectedClient;

            launcher.runMain(ARGUMENTS);

            assertCustomizedStartup(fixture, 2);
            assertNotSame(firstModule, fixture.originalModule);
            assertNotSame(firstClient, fixture.injectedClient);
        }
    }

    @Test
    void leaderStartup_customizesOnceForEachInvocation() {
        try (NamespacedKubernetesClient client = offlineClient()) {
            Fixture fixture = new Fixture(client);
            LeaderLauncher launcher = new LeaderLauncher(fixture);
            launcher.startAsLeader();
            assertCustomizedStartup(fixture, 1);
            Module firstModule = fixture.originalModule;
            TheiaCloudClient firstClient = fixture.injectedClient;

            launcher.startAsLeader();

            assertCustomizedStartup(fixture, 2);
            assertNotSame(firstModule, fixture.originalModule);
            assertNotSame(firstClient, fixture.injectedClient);
        }
    }

    private static NamespacedKubernetesClient offlineClient() {
        return new KubernetesClientBuilder().withConfig(new ConfigBuilder(Config.empty())
                .withMasterUrl("http://127.0.0.1:1").withNamespace("launcher-tests").build())
                .build().adapt(NamespacedKubernetesClient.class);
    }

    private static void assertCustomizedStartup(Fixture fixture, int invocations) {
        assertEquals(invocations, fixture.customizationCalls, "Customize the module once per startup");
        assertSame(fixture.originalModule, fixture.customizationInput);
        assertEquals(0, fixture.fallbackStarts, "The injector must use the module returned by the hook");
        assertEquals(invocations, fixture.customizedStarts);
        assertEquals(invocations, fixture.clientCreations, "Create one custom client per injector");
        assertInstanceOf(CustomClient.class, fixture.injectedClient);
        assertSame(fixture.injectedClient, fixture.resolvedClient);
        assertSame(fixture.kubernetesClient, fixture.injectedClient.kubernetes());
    }

    private static final class Fixture {
        private final NamespacedKubernetesClient kubernetesClient;
        private AbstractTheiaCloudOperatorModule originalModule;
        private Module customizationInput;
        private int customizationCalls;
        private int fallbackStarts;
        private int customizedStarts;
        private int clientCreations;
        private TheiaCloudClient injectedClient;
        private TheiaCloudClient resolvedClient;

        private Fixture(NamespacedKubernetesClient kubernetesClient) {
            this.kubernetesClient = kubernetesClient;
        }

        private AbstractTheiaCloudOperatorModule createModule(TheiaCloudOperatorArguments arguments) {
            originalModule = new AbstractTheiaCloudOperatorModule(arguments) {
                @Override
                protected void configure() {
                    super.configure();
                    bind(Fixture.class).toInstance(Fixture.this);
                }

                @Override
                protected Class<? extends TheiaCloudOperator> bindTheiaCloudOperator() {
                    return FallbackOperator.class;
                }
            };
            return originalModule;
        }

        private Module customizeModule(Module module) {
            customizationCalls++;
            customizationInput = module;
            return Modules.override(module).with(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(NamespacedKubernetesClient.class).toInstance(kubernetesClient);
                    bind(TheiaCloudOperator.class).to(CustomizedOperator.class).in(Singleton.class);
                }

                @Provides
                @Singleton
                TheiaCloudClient provideCustomClient(NamespacedKubernetesClient client) {
                    clientCreations++;
                    return new CustomClient(client);
                }
            });
        }
    }

    private static final class OrdinaryLauncher extends TheiaCloudOperatorLauncher {
        private final Fixture fixture;

        private OrdinaryLauncher(Fixture fixture) {
            this.fixture = fixture;
        }

        @Override
        AbstractTheiaCloudOperatorModule createModule(TheiaCloudOperatorArguments arguments) {
            return fixture.createModule(arguments);
        }

        @Override
        protected Module customizeModule(Module module) {
            return fixture.customizeModule(module);
        }
    }

    private static final class LeaderLauncher extends LeaderElectionTheiaCloudOperatorLauncher {
        private final Fixture fixture;

        private LeaderLauncher(Fixture fixture) {
            this.fixture = fixture;
        }

        @Override
        public AbstractTheiaCloudOperatorModule createModule(TheiaCloudOperatorArguments arguments) {
            return fixture.createModule(arguments);
        }

        @Override
        protected Module customizeModule(Module module) {
            return fixture.customizeModule(module);
        }

        private void startAsLeader() {
            startOperatorAsLeader(createArguments(ARGUMENTS));
        }
    }

    /** The original module can start safely even when the customization hook is never called. */
    private static final class FallbackOperator implements TheiaCloudOperator {
        private final Fixture fixture;

        @Inject
        private FallbackOperator(Fixture fixture) {
            this.fixture = fixture;
        }

        @Override
        public void start() {
            fixture.fallbackStarts++;
        }
    }

    private static final class CustomizedOperator implements TheiaCloudOperator {
        private final Fixture fixture;
        private final TheiaCloudClient client;
        private final Provider<TheiaCloudClient> clientProvider;

        @Inject
        private CustomizedOperator(Fixture fixture, TheiaCloudClient client, Provider<TheiaCloudClient> clientProvider) {
            this.fixture = fixture;
            this.client = client;
            this.clientProvider = clientProvider;
        }

        @Override
        public void start() {
            fixture.customizedStarts++;
            fixture.injectedClient = client;
            fixture.resolvedClient = clientProvider.get();
        }
    }

    private static final class CustomClient extends DefaultTheiaCloudClient {
        private CustomClient(NamespacedKubernetesClient client) {
            super(client);
        }
    }
}
