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
package org.eclipse.theia.cloud.operator.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.eclipse.theia.cloud.common.k8s.client.SessionResourceClient;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sun.net.httpserver.HttpServer;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;

class AuthenticatedSessionAvailabilityTests {
    private final URI publicUrl = URI.create("http://public.example/instances/session/");
    private final URI internalUrl = URI.create("http://app-int.sessions.svc:3001/");
    private Session session;
    private SessionResourceClient sessions;
    private Service service;
    private final List<HttpServer> servers = new ArrayList<>();
    private final List<HttpURLConnection> opened = new ArrayList<>();

    @BeforeEach
    void setUp() {
        session = new Session();
        session.getMetadata().setName("session");
        session.getMetadata().setUid("session-uid");
        session.setSpec(new SessionSpec("session", "app", "user@example.com"));
        session.setStatus(new SessionStatus());
        service = new ServiceBuilder().withNewMetadata().withName("app-int").withNamespace("sessions")
                .endMetadata().withNewSpec().addNewPort().withName("http-internal").withPort(3001)
                .withNewTargetPort(8080).endPort().endSpec().build();
        sessions = mock(SessionResourceClient.class);
        when(sessions.get("session")).thenAnswer(invocation -> Optional.of(session));
        doAnswer(invocation -> {
            Consumer<Session> update = invocation.getArgument(2);
            update.accept(session);
            return session;
        }).when(sessions).editStatus(anyString(), eq("session"), any());
    }

    @AfterEach
    void tearDown() {
        servers.forEach(server -> server.stop(0));
        Thread.interrupted();
    }

    private AuthenticatedSessionAvailability checker(int attempts, int[] publicCodes, int[] internalCodes)
            throws Exception {
        AtomicInteger sleeps = new AtomicInteger();
        AtomicInteger publics = new AtomicInteger();
        AtomicInteger internals = new AtomicInteger();
        return new AuthenticatedSessionAvailability(Runnable::run, millis -> {
            if (sleeps.incrementAndGet() > attempts) {
                throw new InterruptedException("End of test attempts");
            }
        }, uri -> {
            boolean internal = internalUrl.equals(uri);
            assertEquals(internal ? internalUrl : publicUrl, uri);
            int[] codes = internal ? internalCodes : publicCodes;
            int index = (internal ? internals : publics).getAndIncrement();
            assertTrue(index < codes.length, "Unexpected request to " + uri);
            HttpURLConnection connection = mock(HttpURLConnection.class);
            if (codes[index] == -1) {
                when(connection.getResponseCode()).thenThrow(new SocketTimeoutException("stalled"));
            } else {
                when(connection.getResponseCode()).thenReturn(codes[index]);
            }
            opened.add(connection);
            return connection;
        });
    }

    private void run(AuthenticatedSessionAvailability checker) {
        checker.updateSessionURLAsync(sessions, session, "sessions", publicUrl.toString(), service, "test");
    }

    private void assertUnpublished() {
        assertFalse(session.getStatus().hasUrl());
        verify(sessions, never()).editStatus(anyString(), anyString(), any());
    }

    @ParameterizedTest
    @ValueSource(ints = {404, 503, -1})
    void failedPublicAttemptClearsFallback(int failure) throws Exception {
        run(checker(3, new int[] {401, failure, 401}, new int[] {}));
        assertUnpublished();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 500, 503, -1})
    void publicSuccessCannotPublishUnavailableApplication(int code) throws Exception {
        run(checker(1, new int[] {200}, new int[] {code}));
        assertUnpublished();
        for (HttpURLConnection connection : opened) {
            verify(connection).setConnectTimeout(5000);
            verify(connection).setReadTimeout(5000);
            verify(connection).disconnect();
        }
        verify(opened.get(0), never()).setInstanceFollowRedirects(anyBoolean());
        verify(opened.get(1)).setInstanceFollowRedirects(false);
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 204, 301, 302, 399})
    void directApplicationSuccessPublishesPublicURL(int code) throws Exception {
        run(checker(1, new int[] {200}, new int[] {code}));
        assertEquals(publicUrl.toString(), session.getStatus().getUrl());
        opened.forEach(connection -> verify(connection).disconnect());
        verify(sessions, times(1)).editStatus(anyString(), eq("session"), any());
    }

    @Test
    void publicFallbackRequiresTwoEligibleResponses() throws Exception {
        run(checker(2, new int[] {401, 401}, new int[] {200}));
        assertEquals(publicUrl.toString(), session.getStatus().getUrl());
    }

    @Test
    void applicationFailureClearsPendingFallback() throws Exception {
        run(checker(3, new int[] {401, 401, 401}, new int[] {503}));
        assertUnpublished();
    }

    @Test
    void recoveryUsesFreshInternalResponse() throws Exception {
        run(checker(4, new int[] {200, 503, 401, 401}, new int[] {503, 200}));
        assertEquals(publicUrl.toString(), session.getStatus().getUrl());
        opened.forEach(connection -> verify(connection).disconnect());
        verify(sessions, times(1)).editStatus(anyString(), eq("session"), any());
    }

    @Test
    void publicFailureDoesNotProbeApplication() throws Exception {
        run(checker(2, new int[] {404, 503}, new int[] {}));
        assertUnpublished();
    }

    @Test
    void invalidInternalServiceDoesNotFallBackToPublicOnly() throws Exception {
        service.getSpec().getPorts().clear();
        run(checker(1, new int[] {}, new int[] {}));
        assertUnpublished();
        assertTrue(opened.isEmpty());
    }

    @Test
    void completedSessionDoesNotStartWorker() throws Exception {
        session.getStatus().setUrl(publicUrl.toString());
        run(checker(1, new int[] {}, new int[] {}));
        assertTrue(opened.isEmpty());
    }

    @Test
    void deduplicatesWorkersAndReleasesMarkerAfterFailure() throws Exception {
        List<Runnable> tasks = new ArrayList<>();
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getResponseCode()).thenReturn(503);
        AuthenticatedSessionAvailability checker = new AuthenticatedSessionAvailability(tasks::add,
                millis -> {}, uri -> connection);
        run(checker);
        run(checker);
        assertEquals(1, tasks.size());
        tasks.get(0).run();
        assertUnpublished();
        run(checker);
        assertEquals(2, tasks.size());
    }

    @Test
    void rejectedSchedulingReleasesMarker() {
        AtomicInteger count = new AtomicInteger();
        Executor executor = task -> {
            count.incrementAndGet();
            throw new RejectedExecutionException();
        };
        var checker = new AuthenticatedSessionAvailability(executor, millis -> {}, uri -> null);
        assertThrows(RejectedExecutionException.class, () -> run(checker));
        assertThrows(RejectedExecutionException.class, () -> run(checker));
        assertEquals(2, count.get());
    }

    @Test
    void preservesRetryScheduleAndStopsAfterOneHundredAttempts() throws Exception {
        List<Long> delays = new ArrayList<>();
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getResponseCode()).thenReturn(503);
        var checker = new AuthenticatedSessionAvailability(Runnable::run, delays::add, uri -> connection);
        run(checker);
        assertEquals(100, delays.size());
        assertEquals(15, delays.stream().filter(delay -> delay == 2500).count());
        assertEquals(15, delays.stream().filter(delay -> delay == 5000).count());
        assertEquals(15, delays.stream().filter(delay -> delay == 10000).count());
        assertEquals(55, delays.stream().filter(delay -> delay == 60000).count());
        assertUnpublished();
    }

    @Test
    void disconnectsWhenConnectFails() throws Exception {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        doThrow(new IOException("refused")).when(connection).connect();
        AtomicInteger attempts = new AtomicInteger();
        var checker = new AuthenticatedSessionAvailability(Runnable::run, millis -> {
            if (attempts.incrementAndGet() > 1) {
                throw new InterruptedException();
            }
        }, uri -> connection);
        run(checker);
        assertUnpublished();
        verify(connection).disconnect();
    }

    @Test
    void deletedSessionStopsWithoutProbing() throws Exception {
        when(sessions.get("session")).thenReturn(Optional.empty());
        run(checker(1, new int[] {}, new int[] {}));
        assertUnpublished();
        assertTrue(opened.isEmpty());
    }

    @Test
    void transientSessionReadFailureRecoversAndClearsFallback() throws Exception {
        when(sessions.get("session")).thenReturn(Optional.of(session))
                .thenThrow(new KubernetesClientException("Temporary API outage"))
                .thenReturn(Optional.of(session));
        run(checker(4, new int[] {401, 401, 401}, new int[] {200}));
        assertEquals(publicUrl.toString(), session.getStatus().getUrl());
        verify(sessions, times(4)).get("session");
    }

    @Test
    void transientStatusWriteFailureRequiresFreshProbes() throws Exception {
        AtomicInteger updates = new AtomicInteger();
        doAnswer(invocation -> {
            if (updates.incrementAndGet() == 1) {
                throw new KubernetesClientException("Temporary API outage");
            }
            Consumer<Session> edit = invocation.getArgument(2);
            edit.accept(session);
            return session;
        }).when(sessions).editStatus(anyString(), eq("session"), any());
        run(checker(3, new int[] {200, 200, 200}, new int[] {200, 503, 200}));
        assertEquals(publicUrl.toString(), session.getStatus().getUrl());
        assertEquals(2, updates.get());
    }

    @Test
    void replacementDuringRequestsCannotReceiveOldURL() throws Exception {
        Session replacement = new Session();
        replacement.getMetadata().setName("session");
        replacement.getMetadata().setUid("replacement-uid");
        doAnswer(invocation -> {
            Consumer<Session> edit = invocation.getArgument(2);
            edit.accept(replacement);
            return replacement;
        }).when(sessions).editStatus(anyString(), eq("session"), any());
        run(checker(1, new int[] {200}, new int[] {200}));
        assertFalse(session.getNonNullStatus().hasUrl());
        assertFalse(replacement.getNonNullStatus().hasUrl());
    }

    private HttpServer server() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servers.add(server);
        return server;
    }

    private URI url(HttpServer server, String path) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }

    @Test
    void realLoginRedirectCannotHideUnavailableApplication() throws Exception {
        HttpServer proxy = server();
        AtomicInteger loginRequests = new AtomicInteger();
        proxy.createContext("/", exchange -> {
            exchange.getResponseHeaders().add("Location", "/login");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        proxy.createContext("/login", exchange -> {
            loginRequests.incrementAndGet();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        proxy.start();
        HttpServer app = server();
        AtomicInteger applicationCode = new AtomicInteger(503);
        app.createContext("/", exchange -> {
            exchange.sendResponseHeaders(applicationCode.get(), -1);
            exchange.close();
        });
        app.start();
        AtomicInteger attempts = new AtomicInteger();
        var checker = new AuthenticatedSessionAvailability(Runnable::run, millis -> {
            if (attempts.incrementAndGet() == 2) {
                assertUnpublished();
                applicationCode.set(200);
            }
        }, uri -> (HttpURLConnection) (uri.equals(internalUrl) ? url(app, "/") : url(proxy, "/"))
                .toURL().openConnection());
        run(checker);
        assertEquals(2, loginRequests.get());
        assertEquals(publicUrl.toString(), session.getStatus().getUrl());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void realConnectionRefusalAndReadTimeoutPreventPublication(boolean timeout) throws Exception {
        HttpServer app = server();
        CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        app.createContext("/", exchange -> {
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        app.start();
        URI applicationUrl = url(app, "/");
        if (!timeout) {
            app.stop(0);
        }
        HttpURLConnection publicConnection = mock(HttpURLConnection.class);
        when(publicConnection.getResponseCode()).thenReturn(200);
        AtomicInteger attempts = new AtomicInteger();
        var checker = new AuthenticatedSessionAvailability(Runnable::run, millis -> {
            if (attempts.incrementAndGet() > 1) {
                throw new InterruptedException("End of test attempts");
            }
        }, uri -> uri.equals(internalUrl) ? (HttpURLConnection) applicationUrl.toURL().openConnection()
                : publicConnection);
        try {
            run(checker);
            assertUnpublished();
        } finally {
            release.countDown();
        }
    }

    @Test
    void applicationBecomingUnavailableDuringPublicFallbackCannotPublish() throws Exception {
        HttpURLConnection publicConnection = mock(HttpURLConnection.class);
        when(publicConnection.getResponseCode()).thenReturn(401);
        AtomicInteger appCode = new AtomicInteger(200);
        AtomicInteger attempts = new AtomicInteger();
        AtomicInteger applicationRequests = new AtomicInteger();
        var checker = new AuthenticatedSessionAvailability(Runnable::run, millis -> {
            int attempt = attempts.incrementAndGet();
            if (attempt > 2) {
                throw new InterruptedException("End of test attempts");
            }
            if (attempt == 2) {
                appCode.set(503);
            }
        }, uri -> {
            if (!uri.equals(internalUrl)) {
                return publicConnection;
            }
            applicationRequests.incrementAndGet();
            HttpURLConnection connection = mock(HttpURLConnection.class);
            when(connection.getResponseCode()).thenReturn(appCode.get());
            return connection;
        });
        run(checker);
        assertUnpublished();
        assertEquals(1, applicationRequests.get());
    }

    @Test
    void realInternalRedirectIsNotFollowed() throws Exception {
        HttpServer app = server();
        AtomicInteger redirectedRequests = new AtomicInteger();
        app.createContext("/", exchange -> {
            exchange.getResponseHeaders().add("Location", "/login");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        app.createContext("/login", exchange -> {
            redirectedRequests.incrementAndGet();
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });
        app.start();
        HttpURLConnection publicConnection = mock(HttpURLConnection.class);
        when(publicConnection.getResponseCode()).thenReturn(200);
        var checker = new AuthenticatedSessionAvailability(Runnable::run, millis -> {}, uri ->
                uri.equals(internalUrl) ? (HttpURLConnection) url(app, "/").toURL().openConnection()
                        : publicConnection);
        run(checker);
        assertEquals(0, redirectedRequests.get());
        assertEquals(publicUrl.toString(), session.getStatus().getUrl());
    }
}
