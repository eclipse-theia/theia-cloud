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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionStatus;
import org.eclipse.theia.cloud.common.util.JavaUtil;
import org.eclipse.theia.cloud.operator.BasicTheiaCloudOperator;
import org.eclipse.theia.cloud.operator.TheiaCloudOperatorArguments;
import org.eclipse.theia.cloud.operator.handler.AddedHandlerUtil;
import org.eclipse.theia.cloud.operator.routing.SessionRoutingStrategy;
import org.eclipse.theia.cloud.operator.util.TheiaCloudConfigMapUtil;
import org.eclipse.theia.cloud.operator.util.TheiaCloudDeploymentUtil;
import org.eclipse.theia.cloud.operator.util.TheiaCloudServiceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.PodListBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.ServiceListBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import io.fabric8.kubernetes.client.utils.Serialization;

import picocli.CommandLine;

@SuppressWarnings("unchecked")
class EagerSessionAvailabilityTests {
    private NonNamespaceOperation<Service, ServiceList, ServiceResource<Service>> scopedServices;
    private static final String NAMESPACE = "sessions";
    private static final String PUBLIC_URL = "https://public.example/instances/app/";
    private final Map<String, Service> services = new LinkedHashMap<>();
    private TheiaCloudClient client;
    private NamespacedKubernetesClient kubernetes;
    private HookHandler handler;
    private AppDefinition app;
    private Session session;
    private Service external;
    private Service internal;
    private Service otherInternal;
    private Deployment deployment;
    private Consumer<Service> beforeEdit = service -> {};

    @BeforeEach
    void setUp() throws Exception {
        app = new AppDefinition();
        app.getMetadata().setName("app");
        app.getMetadata().setUid("6f1a8966-4d5a-41dc-82ba-381261d79c23");
        app.setSpec(Serialization.unmarshal(
                "{\"name\":\"app\",\"port\":8080}", AppDefinitionSpec.class));
        session = new Session();
        session.getMetadata().setName("session");
        session.getMetadata().setUid("session-uid");
        session.setSpec(new SessionSpec("session", "app", "user@example.com"));
        session.setStatus(new SessionStatus());
        client = mock(TheiaCloudClient.class, RETURNS_DEEP_STUBS);
        kubernetes = mock(NamespacedKubernetesClient.class, RETURNS_DEEP_STUBS);
        when(client.namespace()).thenReturn(NAMESPACE);
        when(client.kubernetes()).thenReturn(kubernetes);
        MixedOperation<Service, ServiceList, ServiceResource<Service>> serviceOperations = mock(MixedOperation.class);
        scopedServices = mock(NonNamespaceOperation.class);
        when(serviceOperations.inNamespace(NAMESPACE)).thenReturn(scopedServices);
        when(kubernetes.services()).thenReturn(serviceOperations);
        when(client.services()).thenReturn(serviceOperations);
        when(client.appDefinitions().get("app")).thenReturn(Optional.of(app));
        when(scopedServices.list()).thenAnswer(invocation ->
                new ServiceListBuilder().withItems(new ArrayList<>(services.values())).build());
        when(kubernetes.pods().list()).thenReturn(new PodListBuilder().build());
        external = service(1, false);
        // Deliberately put the wrong internal instance first.
        otherInternal = service(2, true);
        internal = service(1, true);
        service(2, false);
        deployment = new DeploymentBuilder().withNewMetadata().withName("deployment")
                .withOwnerReferences(appOwner()).endMetadata().build();
        RollableScalableResource<Deployment> deploymentResource =
                mock(RollableScalableResource.class);
        when(kubernetes.apps().deployments().withName(TheiaCloudDeploymentUtil.getDeploymentName(app, 1)))
                .thenReturn(deploymentResource);
        when(deploymentResource.edit(ArgumentMatchers.<UnaryOperator<Deployment>>any())).thenAnswer(invocation -> {
                    UnaryOperator<Deployment> edit = invocation.getArgument(0);
                    return edit.apply(deployment);
                });
        Resource<ConfigMap> configMapResource =
                mock(Resource.class);
        when(kubernetes.configMaps().withName(TheiaCloudConfigMapUtil.getEmailConfigName(app, 1)))
                .thenReturn(configMapResource);
        when(configMapResource.edit(ArgumentMatchers.<UnaryOperator<ConfigMap>>any()))
                .thenAnswer(invocation -> {
                    UnaryOperator<ConfigMap> edit = invocation.getArgument(0);
                    return edit.apply(new ConfigMap());
                });
        handler = new HookHandler();
        handler.arguments = new TheiaCloudOperatorArguments();
        new CommandLine(handler.arguments).parseArgs("--instancesHost=public.example", "--keycloak");
        handler.routingStrategy = mock(SessionRoutingStrategy.class);
        when(handler.routingStrategy.ensureRoutingResourceExists(app, "test")).thenReturn(true);
        when(handler.routingStrategy.ensureRoutingResourceExists(eq(app), anyString())).thenReturn(true);
        when(handler.routingStrategy.addSessionRouting(eq(session), eq(app), any(Service.class), eq(1), anyString()))
                .thenReturn(PUBLIC_URL);
        inject(EagerSessionHandler.class, handler, "client", client);
    }

    private Service service(int instance, boolean isInternal) {
        String name = isInternal ? TheiaCloudServiceUtil.getInternalServiceName(app, instance)
                : TheiaCloudServiceUtil.getServiceName(app, instance);
        Service service = new ServiceBuilder().withNewMetadata().withName(name).withNamespace(NAMESPACE)
                .withOwnerReferences(appOwner()).endMetadata().withNewSpec()
                .addToSelector("app", "app-" + instance).addNewPort()
                .withName(isInternal ? "http-internal" : "http").withPort(isInternal ? 3001 : 8080)
                .endPort().endSpec().build();
        services.put(name, service);
        ServiceResource<Service> resource = mock(ServiceResource.class);
        when(scopedServices.withName(name)).thenReturn(resource);
        when(resource.edit(ArgumentMatchers.<UnaryOperator<Service>>any())).thenAnswer(invocation -> {
            beforeEdit.accept(service);
            UnaryOperator<Service> edit = invocation.getArgument(0);
            return edit.apply(service);
        });
        return service;
    }

    private OwnerReference appOwner() {
        return new OwnerReferenceBuilder().withApiVersion(AppDefinition.API).withKind(AppDefinition.KIND)
                .withName("app").withUid("6f1a8966-4d5a-41dc-82ba-381261d79c23").build();
    }

    private OwnerReference sessionOwner(String uid) {
        return new OwnerReferenceBuilder().withApiVersion(Session.API).withKind(Session.KIND)
                .withName("session").withUid(uid).build();
    }

    private void own(Service service, String uid) {
        service.getMetadata().getOwnerReferences().add(sessionOwner(uid));
    }

    private static void inject(Class<?> type, Object instance, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(instance, value);
    }

    @Test
    void selectsMatchingInstanceAndInvokesExistingOverrides() {
        try (var availability = mockStatic(AddedHandlerUtil.class)) {
            assertTrue(handler.sessionAdded(session, "test"));
            assertEquals(1, handler.externalCalls);
            assertEquals(1, handler.internalCalls);
            availability.verify(() -> AddedHandlerUtil.updateSessionURLAsync(eq(client.sessions()), eq(session), eq(NAMESPACE),
                    eq(PUBLIC_URL), argThat(service -> service.getMetadata().getName().equals(internal.getMetadata().getName())), eq("test")));
            assertEquals(1, otherInternal.getMetadata().getOwnerReferences().size());
        }
    }

    @Test
    void partialReservationResumesOnOperatorInitializationAndIsIdempotent() throws Exception {
        own(internal, "another-session-uid");
        try (var availability = mockStatic(AddedHandlerUtil.class)) {
            assertFalse(handler.sessionAdded(session, "test"));
            availability.verifyNoInteractions();
            assertEquals(2, external.getMetadata().getOwnerReferences().size());
            internal.getMetadata().getOwnerReferences().removeIf(owner -> Session.KIND.equals(owner.getKind()));
            var operator = new ReplayingOperator();
            inject(BasicTheiaCloudOperator.class, operator, "sessionHandler", handler);
            inject(BasicTheiaCloudOperator.class, operator, "arguments", handler.arguments);
            operator.replay(session);
            availability.verify(() -> AddedHandlerUtil.updateSessionURLAsync(eq(client.sessions()), eq(session),
                    eq(NAMESPACE), eq(PUBLIC_URL), argThat(service -> service.getMetadata().getName().equals(internal.getMetadata().getName())), anyString()));
            assertTrue(handler.sessionAdded(session, "test"));
            assertEquals(2, external.getMetadata().getOwnerReferences().size());
            assertEquals(2, internal.getMetadata().getOwnerReferences().size());
            assertEquals(2, deployment.getMetadata().getOwnerReferences().size());
            assertEquals(1, otherInternal.getMetadata().getOwnerReferences().size());
        }
    }

    @Test
    void sameNameDifferentSessionUIDCannotBeTaken() {
        own(external, "session-uid");
        own(internal, "old-session-uid");
        try (var availability = mockStatic(AddedHandlerUtil.class)) {
            assertFalse(handler.sessionAdded(session, "test"));
            availability.verifyNoInteractions();
            assertEquals("old-session-uid", internal.getMetadata().getOwnerReferences().get(1).getUid());
        }
    }

    @Test
    void resumesWhenBothServicesAlreadyBelongToSession() {
        own(external, "session-uid");
        own(internal, "session-uid");
        try (var availability = mockStatic(AddedHandlerUtil.class)) {
            assertTrue(handler.sessionAdded(session, "test"));
            availability.verify(() -> AddedHandlerUtil.updateSessionURLAsync(eq(client.sessions()), eq(session), eq(NAMESPACE),
                    eq(PUBLIC_URL), argThat(service -> service.getMetadata().getName().equals(internal.getMetadata().getName())), eq("test")));
        }
    }

    @Test
    void resumesInternalOnlyReservationWithReorderedExternalServices() {
        own(internal, "session-uid");
        Service first = services.remove(external.getMetadata().getName());
        services.put(first.getMetadata().getName(), first);
        try (var availability = mockStatic(AddedHandlerUtil.class)) {
            assertTrue(handler.sessionAdded(session, "test"));
            availability.verify(() -> AddedHandlerUtil.updateSessionURLAsync(eq(client.sessions()), eq(session),
                    eq(NAMESPACE), eq(PUBLIC_URL), argThat(service -> service.getMetadata().getName()
                            .equals(internal.getMetadata().getName())), eq("test")));
            assertEquals(2, external.getMetadata().getOwnerReferences().size());
            assertEquals(1, otherInternal.getMetadata().getOwnerReferences().size());
        }
    }

    @Test
    void staleInternalCandidateCannotAcquireSecondSessionOwner() {
        beforeEdit = service -> {
            if (service == internal && service.getMetadata().getOwnerReferences().size() == 1) {
                own(service, "other-uid");
            }
        };
        try (var availability = mockStatic(AddedHandlerUtil.class)) {
            assertFalse(handler.sessionAdded(session, "test"));
            availability.verifyNoInteractions();
            assertEquals(2, internal.getMetadata().getOwnerReferences().size());
            assertEquals("other-uid", internal.getMetadata().getOwnerReferences().get(1).getUid());
        }
    }

    @Test
    void rejectsMismatchedServiceReturnedBySubclass() {
        own(external, "session-uid");
        own(otherInternal, "session-uid");
        handler.overrideInternal = otherInternal;
        try (var availability = mockStatic(AddedHandlerUtil.class)) {
            assertFalse(handler.sessionAdded(session, "test"));
            assertEquals(1, handler.internalCalls);
            availability.verifyNoInteractions();
        }
    }

    @Test
    void rejectsForeignServiceReturnedBySubclass() {
        own(internal, "other-uid");
        handler.overrideInternal = internal;
        try (var availability = mockStatic(AddedHandlerUtil.class)) {
            assertFalse(handler.sessionAdded(session, "test"));
            availability.verifyNoInteractions();
        }
    }

    @Test
    void alreadyPublishedSessionDoesNotRepeatSetup() {
        session.getNonNullStatus().setUrl(PUBLIC_URL);
        try (var availability = mockStatic(AddedHandlerUtil.class)) {
            assertTrue(handler.sessionAdded(session, "test"));
            assertEquals(0, handler.externalCalls);
            availability.verifyNoInteractions();
        }
    }

    @Test
    void unauthenticatedReservationKeepsEarlyReturn() {
        handler.arguments = new TheiaCloudOperatorArguments();
        own(external, "session-uid");
        try (var availability = mockStatic(AddedHandlerUtil.class)) {
            assertTrue(handler.sessionAdded(session, "test"));
            assertEquals(0, handler.internalCalls);
            availability.verifyNoInteractions();
        }
    }

    static class HookHandler extends EagerSessionHandler {
        int externalCalls;
        int internalCalls;
        Service overrideInternal;

        @Override
        protected synchronized Entry<Optional<Service>, Boolean> reserveService(NamespacedKubernetesClient client,
                String namespace, String appName, String appUID, String appID, String sessionName, String sessionUID,
                String correlationId) {
            externalCalls++;
            return super.reserveService(client, namespace, appName, appUID, appID, sessionName, sessionUID, correlationId);
        }

        @Override
        protected synchronized Entry<Optional<Service>, Boolean> reserveInternalService(NamespacedKubernetesClient client,
                String namespace, String appName, String appUID, String appID, String sessionName, String sessionUID,
                String correlationId) {
            internalCalls++;
            return overrideInternal == null ? super.reserveInternalService(client, namespace, appName, appUID, appID,
                    sessionName, sessionUID, correlationId) : JavaUtil.tuple(Optional.of(overrideInternal), false);
        }
    }

    static class ReplayingOperator extends BasicTheiaCloudOperator {
        void replay(Session session) {
            initSession(session);
        }
    }
}
