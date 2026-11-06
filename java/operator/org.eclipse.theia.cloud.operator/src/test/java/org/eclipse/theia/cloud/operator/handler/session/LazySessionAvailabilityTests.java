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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionSpec;
import org.eclipse.theia.cloud.operator.TheiaCloudOperatorArguments;
import org.eclipse.theia.cloud.operator.handler.AddedHandlerUtil;
import org.eclipse.theia.cloud.operator.routing.SessionRoutingStrategy;
import org.eclipse.theia.cloud.operator.util.K8sUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;

import picocli.CommandLine;

class LazySessionAvailabilityTests {
    @ParameterizedTest
    @CsvSource({"true,true,true", "true,false,true", "false,true,true", "false,false,true", "true,true,false"})
    void wiresAvailabilityForAuthAndRoutingModes(boolean auth, boolean paths, boolean matching) {
        Session session = new Session();
        session.getMetadata().setName("session");
        session.getMetadata().setUid("session-uid");
        session.setSpec(new SessionSpec("session", "app", "user@example.com"));
        AppDefinition app = new AppDefinition();
        app.getMetadata().setName("app");
        app.getMetadata().setUid("app-uid");
        app.setSpec(Serialization.unmarshal(
                "{\"name\":\"app\",\"port\":8080}", AppDefinitionSpec.class));
        TheiaCloudClient client = mock(TheiaCloudClient.class, RETURNS_DEEP_STUBS);
        when(client.namespace()).thenReturn("sessions");
        when(client.appDefinitions().get("app")).thenReturn(Optional.of(app));
        LazySessionHandler handler = spy(new LazySessionHandler());
        handler.client = client;
        handler.arguments = new TheiaCloudOperatorArguments();
        new CommandLine(handler.arguments).parseArgs("--instancesHost=public.example", "--keycloak=" + auth);
        handler.routingStrategy = mock(SessionRoutingStrategy.class);
        String url = paths ? "https://public.example/instances/session/" : "https://session.public.example/";
        when(handler.routingStrategy.ensureRoutingResourceExists(app, "test")).thenReturn(true);
        when(handler.routingStrategy.addSessionRouting(eq(session), eq(app), any(), eq("test"))).thenReturn(url);
        var external = new ServiceBuilder().withNewMetadata().withName("public").withNamespace("sessions")
                .endMetadata().withNewSpec().addToSelector("app", "session-uid").endSpec().build();
        var internal = new ServiceBuilder(external).editMetadata().withName("internal").endMetadata().build();
        if (!matching) internal.getSpec().setSelector(Map.of("app", "another-session"));
        doReturn(false).when(handler).hasMaxInstancesReached(any(), any(), anyString());
        doReturn(false).when(handler).hasMaxSessionsReached(any(), anyString());
        doNothing().when(handler).syncSessionDataToWorkspace(any(), anyString());
        doReturn(Optional.empty()).when(handler).getStorageName(any(), anyString());
        doReturn(Optional.of(external)).when(handler).createAndApplyService(anyString(), anyString(), anyString(),
                any(), any(), anyBoolean(), anyMap());
        doReturn(Optional.of(internal)).when(handler).createAndApplyInternalService(anyString(), anyString(), anyString(),
                any(), any(), anyMap());
        doNothing().when(handler).createAndApplyEmailConfigMap(anyString(), anyString(), anyString(), any(), anyMap());
        doNothing().when(handler).createAndApplyProxyConfigMap(anyString(), anyString(), anyString(), any(), any(), anyMap());
        doNothing().when(handler).createAndApplyDeployment(anyString(), anyString(), anyString(), any(), any(), any(),
                anyBoolean(), anyMap());
        try (var resources = mockStatic(K8sUtil.class); var availability = mockStatic(AddedHandlerUtil.class)) {
            resources.when(() -> K8sUtil.getExistingServices(any(), anyString(), anyString(), anyString()))
                    .thenReturn(List.of());
            resources.when(() -> K8sUtil.getExistingDeployments(any(), anyString(), anyString(), anyString()))
                    .thenReturn(List.of());
            resources.when(() -> K8sUtil.getExistingConfigMaps(any(), anyString(), anyString(), anyString()))
                    .thenReturn(List.of());
            assertEquals(!auth || matching, handler.sessionAdded(session, "test"));
            if (!matching) {
                availability.verifyNoInteractions();
            } else if (auth) {
                availability.verify(() -> AddedHandlerUtil.updateSessionURLAsync(client.sessions(), session, "sessions",
                        url, internal, "test"));
            } else {
                availability.verify(() -> AddedHandlerUtil.updateSessionURLAsync(client.sessions(), session, "sessions",
                        url, "test"));
            }
        }
    }
}
