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
package org.eclipse.theia.cloud.operator.di;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Optional;

import org.eclipse.theia.cloud.common.k8s.client.AppDefinitionResourceClient;
import org.eclipse.theia.cloud.common.k8s.client.DefaultAppDefinitionResourceClient;
import org.eclipse.theia.cloud.common.k8s.client.DefaultTheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionSpec;
import org.eclipse.theia.cloud.operator.TheiaCloudOperator;
import org.eclipse.theia.cloud.operator.TheiaCloudOperatorArguments;
import org.eclipse.theia.cloud.operator.messaging.MonitorMessagingServiceImpl;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import picocli.CommandLine;

class TheiaCloudClientInjectionTests {

    private static final String SESSION_NAME = "custom-client-session";
    private static final String APP_DEFINITION_NAME = "custom-client-app";
    private static final String SENTINEL_IP = "192.0.2.42";

    @Test
    void defaultClient_isSingletonAndUsesSuppliedKubernetesClient() {
        try (NamespacedKubernetesClient kubernetes = offlineClient()) {
            Injector injector = Guice.createInjector(offlineModule(kubernetes));

            try (TheiaCloudClient client = injector.getInstance(TheiaCloudClient.class)) {
                assertEquals(DefaultTheiaCloudClient.class, client.getClass());
                assertSame(client, injector.getInstance(TheiaCloudClient.class));
                assertSame(kubernetes, client.kubernetes());
                assertEquals("client-injection-tests", client.namespace());
            }
        }
    }

    @Test
    void factoryHook_providesCustomSingletonUsingSuppliedKubernetesClient() {
        try (NamespacedKubernetesClient kubernetes = offlineClient()) {
            CustomClientModule module = new CustomClientModule(Optional.of(SENTINEL_IP), Optional.empty());
            Injector injector = Guice.createInjector(offlineModule(kubernetes, module));

            try (TheiaCloudClient client = injector.getInstance(TheiaCloudClient.class)) {
                assertInstanceOf(CustomTheiaCloudClient.class, client);
                assertSame(client, injector.getInstance(TheiaCloudClient.class));
                assertSame(kubernetes, client.kubernetes());
                assertEquals(1, module.provisionCount);
            }
        }
    }

    @Test
    void monitorUrl_usesCustomClientIpAndApplicationDefinitionPort() throws Exception {
        assertMonitorUrl(Optional.of(SENTINEL_IP), Optional.of(applicationDefinition()),
                Optional.of("http://192.0.2.42:3001/monitor/message"));
    }

    @Test
    void monitorUrl_missingCustomClientIpReturnsEmpty() throws Exception {
        assertMonitorUrl(Optional.empty(), Optional.of(applicationDefinition()), Optional.empty());
    }

    @Test
    void monitorUrl_missingApplicationDefinitionReturnsEmpty() {
        assertMonitorUrl(Optional.of(SENTINEL_IP), Optional.empty(), Optional.empty());
    }

    private void assertMonitorUrl(Optional<String> ip, Optional<AppDefinition> appDefinition,
            Optional<String> expectedUrl) {
        try (NamespacedKubernetesClient kubernetes = offlineClient()) {
            Injector injector = Guice.createInjector(offlineModule(kubernetes, new CustomClientModule(ip, appDefinition)));

            try (TheiaCloudClient client = injector.getInstance(TheiaCloudClient.class)) {
                CustomTheiaCloudClient customClient = assertInstanceOf(CustomTheiaCloudClient.class, client);
                UrlInspectingMonitorMessagingService monitor = injector
                        .getInstance(UrlInspectingMonitorMessagingService.class);
                Session session = new Session();
                session.setSpec(new SessionSpec(SESSION_NAME, APP_DEFINITION_NAME, "test-user"));

                assertEquals(expectedUrl, monitor.url(session));
                assertEquals(SESSION_NAME, customClient.requestedSessionName);
                assertEquals(APP_DEFINITION_NAME, customClient.requestedAppDefinitionName);
                assertSame(kubernetes, customClient.kubernetes());
            }
        }
    }

    private static Module offlineModule(NamespacedKubernetesClient kubernetes) {
        AbstractTheiaCloudOperatorModule operatorModule = new AbstractTheiaCloudOperatorModule(arguments()) {
            @Override
            protected Class<? extends TheiaCloudOperator> bindTheiaCloudOperator() {
                return InertOperator.class;
            }
        };
        return offlineModule(kubernetes, operatorModule);
    }

    private static TheiaCloudOperatorArguments arguments() {
        TheiaCloudOperatorArguments arguments = new TheiaCloudOperatorArguments();
        new CommandLine(arguments).parseArgs("--instancesHost", "example.test", "--cloudProvider", "K8S");
        return arguments;
    }

    private static Module offlineModule(NamespacedKubernetesClient kubernetes,
            AbstractTheiaCloudOperatorModule operatorModule) {
        // Replace the binding itself so the production CRD-validating provider is never invoked.
        return Modules.override(operatorModule)
                .with((Module) binder -> binder.bind(NamespacedKubernetesClient.class).toInstance(kubernetes));
    }

    private static NamespacedKubernetesClient offlineClient() {
        Config config = new ConfigBuilder(Config.empty()).withMasterUrl("http://127.0.0.1:1")
                .withNamespace("client-injection-tests").build();
        return new KubernetesClientBuilder().withConfig(config).build().adapt(NamespacedKubernetesClient.class);
    }

    private static AppDefinition applicationDefinition() throws Exception {
        AppDefinition appDefinition = new AppDefinition();
        appDefinition.setSpec(new ObjectMapper().readValue("{\"monitor\":{\"port\":3001}}", AppDefinitionSpec.class));
        return appDefinition;
    }

    public static class InertOperator implements TheiaCloudOperator {
        @Override
        public void start() {
        }
    }

    private static class CustomClientModule extends AbstractTheiaCloudOperatorModule {
        private final Optional<String> ip;
        private final Optional<AppDefinition> appDefinition;
        private int provisionCount;

        CustomClientModule(Optional<String> ip, Optional<AppDefinition> appDefinition) {
            super(arguments());
            this.ip = ip;
            this.appDefinition = appDefinition;
        }

        @Override
        protected Class<? extends TheiaCloudOperator> bindTheiaCloudOperator() {
            return InertOperator.class;
        }

        @Override
        protected TheiaCloudClient createTheiaCloudClient(NamespacedKubernetesClient kubernetes) {
            provisionCount++;
            return new CustomTheiaCloudClient(kubernetes, ip, appDefinition);
        }
    }

    private static class CustomTheiaCloudClient extends DefaultTheiaCloudClient {
        private final Optional<String> ip;
        private final AppDefinitionResourceClient appDefinitions;
        private String requestedSessionName;
        private String requestedAppDefinitionName;

        CustomTheiaCloudClient(NamespacedKubernetesClient kubernetes, Optional<String> ip,
                Optional<AppDefinition> appDefinition) {
            super(kubernetes);
            this.ip = ip;
            appDefinitions = new DefaultAppDefinitionResourceClient(kubernetes) {
                @Override
                public Optional<AppDefinition> get(String name) {
                    requestedAppDefinitionName = name;
                    return appDefinition.filter(ignored -> APP_DEFINITION_NAME.equals(name));
                }
            };
        }

        @Override
        public Optional<String> getClusterIPFromSessionName(String sessionName) {
            requestedSessionName = sessionName;
            return ip;
        }

        @Override
        public AppDefinitionResourceClient appDefinitions() {
            return appDefinitions;
        }
    }

    public static class UrlInspectingMonitorMessagingService extends MonitorMessagingServiceImpl {
        Optional<String> url(Session session) {
            return getURL(session);
        }
    }
}
