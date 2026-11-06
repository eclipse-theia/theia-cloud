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
package org.eclipse.theia.cloud.operator.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.operator.TheiaCloudOperatorArguments;
import org.eclipse.theia.cloud.operator.ingress.IngressPathProvider;
import org.eclipse.theia.cloud.operator.util.K8sUtil;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;

import picocli.CommandLine;

class IngressSessionReplayTests {
    @Test
    void authenticatedEagerReplayReplacesOnlyItsOwnPath() throws Exception {
        var args = new TheiaCloudOperatorArguments();
        new CommandLine(args).parseArgs("--instancesHost=public.example", "--keycloak");
        var strategy = new IngressRoutingStrategy();
        var client = mock(TheiaCloudClient.class, RETURNS_DEEP_STUBS);
        var paths = mock(IngressPathProvider.class);
        AppDefinition app = new AppDefinition();
        app.getMetadata().setName("app");
        app.getMetadata().setUid("app-uid");
        app.setSpec(Serialization.unmarshal(
                "{\"name\":\"app\",\"port\":8080}", AppDefinitionSpec.class));
        Ingress ingress = new IngressBuilder().withNewMetadata().withName("ingress").endMetadata()
                .withNewSpec().addNewRule().withHost("public.example").withNewHttp().addNewPath()
                .withPath("/other(/|$)(.*)").endPath().endHttp().endRule().endSpec().build();
        when(client.namespace()).thenReturn("sessions");
        when(paths.getPath(app, 1)).thenReturn("/instances/app");
        var ingressClient = client.ingresses();
        doAnswer(invocation -> {
            Consumer<Ingress> edit = invocation.getArgument(2);
            edit.accept(ingress);
            return ingress;
        }).when(ingressClient).edit(eq("test"), eq("ingress"), any());
        inject(strategy, "client", client);
        inject(strategy, "arguments", args);
        inject(strategy, "ingressPathProvider", paths);
        try (var resources = mockStatic(K8sUtil.class)) {
            resources.when(() -> K8sUtil.getExistingIngress(any(), eq("sessions"), eq("app"), eq("app-uid")))
                    .thenReturn(Optional.of(ingress));
            var service = new ServiceBuilder().withNewMetadata().withName("service").endMetadata().build();
            String first = strategy.addSessionRouting(new Session(), app, service, 1, "test");
            String second = strategy.addSessionRouting(new Session(), app, service, 1, "test");
            assertEquals(first, second);
            assertEquals(2, ingress.getSpec().getRules().size());
            assertEquals("/other(/|$)(.*)", ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).getPath());
            assertEquals("service", ingress.getSpec().getRules().get(1).getHttp().getPaths().get(0)
                    .getBackend().getService().getName());
        }
    }

    private void inject(Object instance, String name, Object value) throws Exception {
        Field field = IngressRoutingStrategy.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(instance, value);
    }
}
