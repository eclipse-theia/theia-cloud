/********************************************************************************
 * Copyright (C) 2022 EclipseSource, Lockular, Ericsson, STMicroelectronics and
 * others.
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
package org.eclipse.theia.cloud.operator.handler.session;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Optional;

import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionSpec;
import org.eclipse.theia.cloud.common.util.JavaUtil;
import org.eclipse.theia.cloud.operator.TheiaCloudOperatorArguments;
import org.eclipse.theia.cloud.operator.handler.AddedHandlerUtil;
import org.eclipse.theia.cloud.operator.routing.SessionRoutingStrategy;
import org.eclipse.theia.cloud.operator.util.TheiaCloudServiceUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.PodListBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import io.fabric8.kubernetes.client.utils.Serialization;

class EagerSessionAvailabilityTests {

    @SuppressWarnings("unchecked")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void probesTheRoutedInstanceOnlyWhenKeycloakIsEnabled(boolean keycloak) throws Exception {
        var client = mock(TheiaCloudClient.class, RETURNS_DEEP_STUBS);
        NonNamespaceOperation<Service, ServiceList, ServiceResource<Service>> services = mock(NonNamespaceOperation.class);
        when(client.services().inNamespace("sessions")).thenReturn(services);
        ServiceResource<Service> service = mock(ServiceResource.class);
        when(services.withName(Mockito.anyString())).thenReturn(service);
        when(client.namespace()).thenReturn("sessions");
        when(client.kubernetes().pods().list()).thenReturn(new PodListBuilder().build());
        var app = new AppDefinition();
        app.getMetadata().setName("app");
        String appUid = "6f1a8966-4d5a-41dc-82ba-381261d79c23";
        app.getMetadata().setUid(appUid);
        app.setSpec(Serialization.unmarshal("{\"name\":\"app\",\"port\":8080}", AppDefinitionSpec.class));
        when(client.appDefinitions().get("app")).thenReturn(Optional.of(app));

        var session = new Session();
        session.getMetadata().setName("session");
        session.getMetadata().setUid("session-uid");
        session.setSpec(new SessionSpec("session", "app", "user@example.com"));
        var handler = Mockito.spy(new EagerSessionHandler());
        var clientField = EagerSessionHandler.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(handler, client);
        handler.arguments = mock(TheiaCloudOperatorArguments.class);
        when(handler.arguments.isUseKeycloak()).thenReturn(keycloak);
        handler.routingStrategy = mock(SessionRoutingStrategy.class);
        when(handler.routingStrategy.ensureRoutingResourceExists(app, "test")).thenReturn(true);

        var external = new ServiceBuilder().withNewMetadata()
                .withName(TheiaCloudServiceUtil.getServiceName(app, 1)).endMetadata().build();
        // The separate internal reservation can select a sibling of the publicly routed instance.
        var sibling = new ServiceBuilder().withNewMetadata()
                .withName(TheiaCloudServiceUtil.getInternalServiceName(app, 2)).endMetadata().build();
        var kubernetes = client.kubernetes();
        RollableScalableResource<Deployment> deployment = mock(RollableScalableResource.class);
        when(kubernetes.apps().deployments().withName(Mockito.anyString())).thenReturn(deployment);
        Resource<ConfigMap> configMap = mock(Resource.class);
        when(kubernetes.configMaps().withName(Mockito.anyString())).thenReturn(configMap);
        Mockito.doReturn(JavaUtil.tuple(Optional.of(external), false)).when(handler).reserveService(
                kubernetes, "sessions", "app", appUid, "app", "session", "session-uid", "test");
        Mockito.doReturn(JavaUtil.tuple(Optional.of(sibling), false)).when(handler).reserveInternalService(
                kubernetes, "sessions", "app", appUid, "app", "session", "session-uid", "test");
        String publicUrl = "https://public.example/session/";
        when(handler.routingStrategy.addSessionRouting(session, app, external, 1, "test")).thenReturn(publicUrl);
        String expectedName = TheiaCloudServiceUtil.getInternalServiceName(app, 1);
        Optional<URI> expected = keycloak ? Optional.of(URI.create("http://" + expectedName + ".sessions.svc:3001/"))
                : Optional.empty();
        try (var availability = mockStatic(AddedHandlerUtil.class)) {
            assertTrue(handler.sessionAdded(session, "test"));
            availability.verify(() -> AddedHandlerUtil.updateSessionURLAsync(client.sessions(), session, "sessions",
                    publicUrl, expected, "test"));
        }
    }
}
