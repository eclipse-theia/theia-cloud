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
package org.eclipse.theia.cloud.operator.handler.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.theia.cloud.common.k8s.client.DefaultTheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionSpec;
import org.eclipse.theia.cloud.common.util.LabelsUtil;
import org.eclipse.theia.cloud.operator.TheiaCloudOperatorArguments;
import org.eclipse.theia.cloud.operator.handler.AddedHandlerUtil;
import org.eclipse.theia.cloud.operator.ingress.IngressPathProvider;
import org.eclipse.theia.cloud.operator.ingress.IngressPathProviderImpl;
import org.eclipse.theia.cloud.operator.routing.IngressRoutingStrategy;
import org.eclipse.theia.cloud.operator.routing.SessionRoutingStrategy;
import org.eclipse.theia.cloud.operator.util.TheiaCloudConfigMapUtil;
import org.eclipse.theia.cloud.operator.util.TheiaCloudServiceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import com.google.inject.Guice;
import com.sun.net.httpserver.HttpServer;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceListBuilder;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.kubernetes.client.utils.Serialization;
import picocli.CommandLine;

@EnableKubernetesMockClient(crud = true)
class EagerSessionServiceReservationTests {

    private KubernetesClient kube;
    private KubernetesMockServer server;
    private final TheiaCloudOperatorArguments arguments = new TheiaCloudOperatorArguments();
    private AppDefinition app;
    private Session session;
    private EagerSessionHandler handler;
    private String servicesPath;

    @BeforeEach
    void setUp() {
        TheiaCloudClient client = new DefaultTheiaCloudClient(kube);
        servicesPath = "/api/v1/namespaces/" + client.namespace() + "/services";
        app = new AppDefinition();
        app.getMetadata().setName("editor");
        app.setSpec(Serialization.unmarshal("{\"name\":\"editor\",\"port\":8080}", AppDefinitionSpec.class));
        app = kube.resource(app).create();
        session = new Session();
        session.getMetadata().setName("session");
        session.setSpec(new SessionSpec("session", "editor", "user@example.com"));
        session = kube.resource(session).create();
        kube.resource(new IngressBuilder().withNewMetadata().withName("ingress").withOwnerReferences(owner(app))
                .endMetadata().withNewSpec().endSpec().build()).create();
        configure(false);
        handler = Guice.createInjector(binder -> {
            binder.bind(TheiaCloudClient.class).toInstance(client);
            binder.bind(TheiaCloudOperatorArguments.class).toInstance(arguments);
            binder.bind(IngressPathProvider.class).to(IngressPathProviderImpl.class);
            binder.bind(SessionRoutingStrategy.class).to(IngressRoutingStrategy.class);
        }).getInstance(EagerSessionHandler.class);
    }

    private void configure(boolean keycloak) {
        new CommandLine(arguments).parseArgs("--instancesHost=" + URI.create(server.url("/")).getAuthority(),
                "--keycloak=" + keycloak, "--ingressPathSuffix=");
    }

    private OwnerReference owner(HasMetadata resource) {
        return new OwnerReferenceBuilder().withApiVersion(resource.getApiVersion()).withKind(resource.getKind())
                .withName(resource.getMetadata().getName()).withUid(resource.getMetadata().getUid()).build();
    }

    private String name(int instance, boolean internal) {
        return internal ? TheiaCloudServiceUtil.getInternalServiceName(app, instance)
                : TheiaCloudServiceUtil.getServiceName(app, instance);
    }

    private Service service(int instance, boolean internal, OwnerReference... extraOwners) {
        return kube.resource(new ServiceBuilder().withNewMetadata().withName(name(instance, internal))
                .withOwnerReferences(owner(app)).addToOwnerReferences(extraOwners).addToLabels("app", "editor-" + instance)
                .endMetadata().withNewSpec().addToSelector("custom-instance", "editor-" + instance).endSpec().build()).create();
    }

    private void workload(int instance) {
        String deployment = name(instance, false);
        kube.resource(new DeploymentBuilder().withNewMetadata().withName(deployment).withOwnerReferences(owner(app))
                .endMetadata().build()).create();
        if (arguments.isUseKeycloak()) {
            kube.resource(new ConfigMapBuilder().withNewMetadata()
                    .withName(TheiaCloudConfigMapUtil.getEmailConfigName(app, instance)).endMetadata().build()).create();
        }
        kube.resource(new PodBuilder().withNewMetadata().withName(deployment + "-pod")
                .addNewOwnerReference().withKind("ReplicaSet").withName(deployment + "-rs").endOwnerReference()
                .endMetadata().build()).create();
        server.expect().get().withPath("/editor-" + instance + "/").andReturn(200, "ready").always();
    }

    private Service stored(Service service) {
        return kube.services().withName(service.getMetadata().getName()).get();
    }

    private void assertOwners(HasMetadata resource, OwnerReference... owners) {
        assertEquals(List.of(owners), resource.getMetadata().getOwnerReferences(), resource.getMetadata().getName());
    }

    private boolean add() {
        return handler.sessionAdded(session, "test");
    }

    @ParameterizedTest
    @CsvSource({"1,2,false", "2,1,true"})
    void pairsOppositelyOrderedServicesAndCleansUpOnlyTheRoutedInstance(int first, int second, boolean keycloak)
            throws IOException {
        configure(keycloak);
        Service firstExternal = service(first, false);
        Service secondExternal = service(second, false);
        Service secondInternal = service(second, true);
        Service firstInternal = service(first, true);
        workload(first);
        workload(second);
        // Control the LIST response independently of the mock API server's storage order.
        server.expect().get().withPath(servicesPath).andReturn(200, new ServiceListBuilder()
                .withItems(firstExternal, secondExternal, secondInternal, firstInternal).build()).always();

        if (keycloak) {
            addWithInternalProbe();
        } else {
            assertTrue(add());
        }
        var ingress = kube.network().v1().ingresses().withName("ingress").get();
        var paths = ingress.getSpec().getRules().getFirst().getHttp().getPaths();
        String backend = paths.getFirst().getBackend().getService().getName();
        int selected = backend.equals(name(first, false)) ? first : second;
        int sibling = selected == first ? second : first;
        assertEquals(name(selected, false), backend);
        assertEquals("/editor-" + selected, paths.getFirst().getPath());
        for (boolean internal : List.of(false, true)) {
            Service reserved = kube.services().withName(name(selected, internal)).get();
            assertOwners(reserved, owner(app), owner(session));
            assertTrue(reserved.getMetadata().getLabels().entrySet()
                    .containsAll(LabelsUtil.createSessionLabels(session, app).entrySet()));
            assertOwners(kube.services().withName(name(sibling, internal)).get(), owner(app));
        }
        assertOwners(kube.apps().deployments().withName(backend).get(), owner(app), owner(session));
        if (keycloak) {
            assertEquals(Map.of(AddedHandlerUtil.FILENAME_AUTHENTICATED_EMAILS_LIST, session.getSpec().getUser()),
                    kube.configMaps().withName(TheiaCloudConfigMapUtil.getEmailConfigName(app, selected)).get().getData());
            assertTrue(kube.pods().withName(backend + "-pod").get().getMetadata().getAnnotations()
                    .containsKey(EagerSessionHandler.EAGER_START_REFRESH_ANNOTATION));
        }
        awaitPublication(selected);
        server.clearExpectations();
        assertTrue(handler.sessionDeleted(session, "test"));
        for (boolean internal : List.of(false, true)) {
            Service cleaned = kube.services().withName(name(selected, internal)).get();
            assertOwners(cleaned, owner(app));
            assertFalse(cleaned.getMetadata().getLabels().containsKey(LabelsUtil.LABEL_KEY_SESSION_UUID));
        }
        assertOwners(kube.apps().deployments().withName(backend).get(), owner(app));
        assertNull(kube.pods().withName(backend + "-pod").get());
        assertTrue(kube.network().v1().ingresses().withName("ingress").get().getSpec().getRules().isEmpty());
        assertNotNull(kube.pods().withName(name(sibling, false) + "-pod").get());
    }

    private void addWithInternalProbe() throws IOException {
        // The Fabric8 mock has no cluster DNS. Expose the paired internal Service over loopback.
        HttpServer internal = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        internal.createContext("/", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        internal.start();
        String internalName = name(1, true);
        try (var serviceUtil = Mockito.mockStatic(TheiaCloudServiceUtil.class, Mockito.CALLS_REAL_METHODS)) {
            serviceUtil.when(() -> TheiaCloudServiceUtil.getInternalServiceURL(internalName, kube.getNamespace()))
                    .thenReturn(URI.create("http://127.0.0.1:" + internal.getAddress().getPort() + "/"));
            assertTrue(add());
            awaitPublication(1);
        } finally {
            internal.stop(0);
        }
    }

    @ParameterizedTest
    @CsvSource({"true,false", "false,true", "true,true"})
    void resumesExistingReservationAndReplaysSetup(boolean ownsExternal, boolean ownsInternal) {
        Service sibling = service(1, false);
        service(1, true);
        Service external = ownsExternal ? service(2, false, owner(session)) : service(2, false);
        Service internal = ownsInternal ? service(2, true, owner(session)) : service(2, true);
        workload(2);

        server.expect().patch().withPath("/apis/networking.k8s.io/v1/namespaces/" + kube.getNamespace()
                + "/ingresses/ingress").andReturn(403, new StatusBuilder().withCode(403).build()).once();
        assertFalse(add()); // Both Services and the deployment are already owned when routing fails.
        assertTrue(add());
        awaitPublication(2);
        // Recreate the restart boundary after the route exists but before status.url has been published.
        session = kube.resources(Session.class).withName(session.getMetadata().getName()).editStatus(s -> {
            s.getStatus().setUrl(null);
            return s;
        });
        assertTrue(add());
        awaitPublication(2);
        session = kube.resource(session).get();
        assertTrue(add());
        assertOwners(stored(external), owner(app), owner(session));
        assertOwners(stored(internal), owner(app), owner(session));
        assertOwners(stored(sibling), owner(app));
        assertOwners(kube.apps().deployments().withName(name(2, false)).get(), owner(app), owner(session));
        assertEquals(1, kube.network().v1().ingresses().withName("ingress").get().getSpec().getRules().size());
    }

    private OwnerReference competingOwner() {
        // A recreated Session may even have the same name.
        return new OwnerReferenceBuilder(owner(session)).withUid("other-session-uid").build();
    }

    @Test
    void skipsBlockedPairForACompletelyFreePair() {
        Service external = service(1, false);
        Service internal = service(1, true, competingOwner());
        Service free = service(2, false);
        Service matching = service(2, true);
        workload(2);

        assertTrue(add());
        assertOwners(stored(external), owner(app));
        assertOwners(stored(internal), owner(app), competingOwner());
        assertOwners(stored(free), owner(app), owner(session));
        assertOwners(stored(matching), owner(app), owner(session));
        awaitPublication(2);
    }

    @Test
    void missingPartnerDoesNotClaimAnUnrelatedService() {
        Service external = service(1, false);
        Service sibling = service(2, true);
        Service foreign = service(1, true);
        kube.resource(foreign).edit(s -> new ServiceBuilder(s).editMetadata()
                .withOwnerReferences(new OwnerReferenceBuilder(owner(app)).withUid("old-app-uid").build())
                .endMetadata().build());

        assertFalse(add());
        assertOwners(stored(external), owner(app));
        assertOwners(stored(sibling), owner(app));
        assertEquals("old-app-uid", stored(foreign).getMetadata().getOwnerReferences().getFirst().getUid());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void cannotSwitchInstancesAfterAPartialReservation(boolean internalFirst) {
        Service reserved = service(1, internalFirst, owner(session));
        Service blocked = service(1, !internalFirst, competingOwner());
        Service sibling = service(2, false);
        Service siblingInternal = service(2, true);

        assertFalse(add());
        assertOwners(stored(reserved), owner(app), owner(session));
        assertOwners(stored(blocked), owner(app), competingOwner());
        assertOwners(stored(sibling), owner(app));
        assertOwners(stored(siblingInternal), owner(app));
    }

    @Test
    void rejectsAnExistingSplitReservation() {
        Service external = service(1, false, owner(session));
        Service internal = service(2, true, owner(session));
        Service otherExternal = service(2, false);
        Service otherInternal = service(1, true);

        assertFalse(add());
        assertOwners(stored(external), owner(app), owner(session));
        assertOwners(stored(internal), owner(app), owner(session));
        assertOwners(stored(otherExternal), owner(app));
        assertOwners(stored(otherInternal), owner(app));
        assertTrue(kube.network().v1().ingresses().withName("ingress").get().getSpec().getRules().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void handlesAStaleSnapshotWhenAnotherSessionClaimsAService(boolean staleInternal) {
        Service external = service(1, false);
        Service internal = service(1, true);
        Service stale = staleInternal ? internal : external;
        kube.resource(stale).edit(s -> new ServiceBuilder(s).editMetadata().addToOwnerReferences(competingOwner())
                .endMetadata().build());
        server.expect().get().withPath(servicesPath).andReturn(200,
                new ServiceListBuilder().withItems(external, internal).build()).once();

        assertFalse(add());
        assertOwners(stored(stale), owner(app), competingOwner());
        Service free = stored(staleInternal ? external : internal);
        assertOwners(free, owner(app));
        assertEquals(Map.of("app", "editor-1"), free.getMetadata().getLabels());
    }

    private void rejectWrite(Service service, int code) {
        server.expect().put().withPath(servicesPath + "/" + service.getMetadata().getName())
                .andReturn(code, new StatusBuilder().withCode(code).withMessage("injected rejection").build()).once();
    }

    @Test
    void failedSecondWritePreservesEarlierOwnership() {
        Service external = service(1, false, owner(session));
        Service internal = service(1, true);
        rejectWrite(internal, 403);
        assertFalse(add());
        assertOwners(stored(external), owner(app), owner(session));
        assertOwners(stored(internal), owner(app));
        workload(1);
        assertTrue(add());
        awaitPublication(1);
    }

    @Test
    void rollbackConflictLeavesReservationPinnedForRetry() {
        Service external = service(2, false);
        Service internal = service(2, true);
        // Reject the rollback PUT after the initial external claim has succeeded.
        server.expect().put().withPath(servicesPath + "/" + internal.getMetadata().getName())
                .andReply(403, request -> {
                    rejectWrite(external, 409);
                    return new StatusBuilder().withCode(403).withMessage("injected rejection").build();
                }).once();
        assertFalse(add());
        Service firstClaim = stored(external);
        assertOwners(firstClaim, owner(app), owner(session));
        Service sibling = service(1, false);
        service(1, true);
        workload(2);
        assertTrue(add());
        assertOwners(stored(sibling), owner(app));
        assertOwners(stored(external), owner(app), owner(session));
        assertOwners(stored(internal), owner(app), owner(session));
        awaitPublication(2);
    }

    @Test
    void deletionReleasesAPartialReservationWithoutTouchingTheSibling() {
        Service reserved = service(1, true, owner(session));
        Service sibling = service(2, false, competingOwner());
        assertTrue(handler.sessionDeleted(session, "test"));
        assertOwners(stored(reserved), owner(app));
        assertOwners(stored(sibling), owner(app), competingOwner());
    }

    @Test
    void failedRouteCleanupKeepsBothServicesReserved() {
        Service external = service(1, false, owner(session));
        Service internal = service(1, true, owner(session));
        workload(1);
        assertTrue(add());
        awaitPublication(1);

        server.expect().patch().withPath("/apis/networking.k8s.io/v1/namespaces/" + kube.getNamespace()
                + "/ingresses/ingress").andReturn(403, new StatusBuilder().withCode(403).build()).once();
        assertFalse(handler.sessionDeleted(session, "failed-cleanup"));
        assertOwners(stored(external), owner(app), owner(session));
        assertOwners(stored(internal), owner(app), owner(session));
    }

    @Test
    void failedSecondReleaseLeavesOnlyThatServiceReserved() {
        Service external = service(1, false, owner(session));
        Service internal = service(1, true, owner(session));
        workload(1);
        assertTrue(add());
        awaitPublication(1);
        server.expect().get().withPath(servicesPath).andReturn(200,
                new ServiceListBuilder().withItems(stored(external), stored(internal)).build()).once();
        server.expect().put().withPath(servicesPath + "/" + internal.getMetadata().getName())
                .andReturn(403, new StatusBuilder().withCode(403).build()).times(3);

        assertFalse(handler.sessionDeleted(session, "failed-release"));
        assertOwners(stored(external), owner(app));
        assertOwners(stored(internal), owner(app), owner(session));
    }

    @Test
    void competingSessionsCannotBothClaimTheSamePair() throws Exception {
        Service external = service(1, false);
        Service internal = service(1, true);
        workload(1);
        Session other = new Session();
        other.getMetadata().setName("other");
        other.setSpec(new SessionSpec("other", "editor", "other@example.com"));
        Session competitor = kube.resource(other).create();
        // Give both callers the same unowned snapshot, regardless of when their writes arrive.
        server.expect().get().withPath(servicesPath).andReturn(200,
                new ServiceListBuilder().withItems(external, internal).build()).times(2);
        var start = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Callable<Boolean>> attempts = List.of(() -> {
                start.await();
                return handler.sessionAdded(session, "first");
            }, () -> {
                start.await();
                return handler.sessionAdded(competitor, "second");
            });
            var results = executor.invokeAll(attempts);
            boolean firstWon = results.getFirst().get();
            assertEquals(!firstWon, results.getLast().get());
            Session winner = firstWon ? session : competitor;
            assertOwners(stored(external), owner(app), owner(winner));
            assertOwners(stored(internal), owner(app), owner(winner));
            kube.resource(winner).waitUntilCondition(s -> s != null && s.getNonNullStatus().hasUrl(),
                    20, TimeUnit.SECONDS);
        }
    }

    private void awaitPublication(int instance) {
        String expected = server.url("/editor-" + instance + "/");
        kube.resource(session).waitUntilCondition(s -> s != null && expected.equals(s.getNonNullStatus().getUrl()),
                20, TimeUnit.SECONDS);
    }
}
