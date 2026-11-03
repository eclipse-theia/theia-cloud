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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.eclipse.theia.cloud.common.k8s.client.SessionResourceClient;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionStatus;
import org.eclipse.theia.cloud.operator.util.TheiaCloudServiceUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sun.net.httpserver.HttpServer;

import io.fabric8.kubernetes.api.model.Service;

/** Exercises the public production entry point, its executor, HTTP requests and status publication. */
class SessionAvailabilityRegressionTests {
    @ParameterizedTest
    @ValueSource(ints = {200, 404, 503})
    void doesNotPublishBeforeBothEndpointsRecover(int failure) throws Exception {
        HttpServer proxy = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        HttpServer app = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger publicCode = new AtomicInteger(failure == 200 ? 302 : 401);
        AtomicInteger appCode = new AtomicInteger(failure == 200 ? 503 : 200);
        AtomicInteger publicRequests = new AtomicInteger();
        CountDownLatch exercised = new CountDownLatch(2);
        CountDownLatch published = new CountDownLatch(1);
        proxy.createContext("/", exchange -> {
            int code = publicCode.get();
            publicRequests.incrementAndGet();
            if (code == 302) exchange.getResponseHeaders().add("Location", "/login");
            exchange.sendResponseHeaders(code, -1);
            exchange.close();
            if (code == 401) publicCode.set(failure);
            exercised.countDown();
        });
        proxy.createContext("/login", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        app.createContext("/", exchange -> {
            exchange.sendResponseHeaders(appCode.get(), -1);
            exchange.close();
        });
        proxy.start();
        app.start();
        String publicUrl = "http://127.0.0.1:" + proxy.getAddress().getPort() + "/";
        URI internalUrl = URI.create("http://127.0.0.1:" + app.getAddress().getPort() + "/");
        Session session = new Session();
        session.getMetadata().setName("regression-" + failure);
        session.getMetadata().setUid("regression-uid-" + failure);
        session.setSpec(new SessionSpec("session", "app", "user@example.com"));
        session.setStatus(new SessionStatus());
        SessionResourceClient sessions = mock(SessionResourceClient.class);
        doAnswer(invocation -> {
            Consumer<SessionStatus> update = invocation.getArgument(2);
            update.accept(session.getStatus());
            published.countDown();
            return session;
        }).when(sessions).updateStatus(anyString(), eq(session), any());
        when(sessions.get(session.getMetadata().getName())).thenReturn(Optional.of(session));
        doAnswer(invocation -> {
            Consumer<Session> update = invocation.getArgument(2);
            update.accept(session);
            if (session.getNonNullStatus().hasUrl()) published.countDown();
            return session;
        }).when(sessions).editStatus(anyString(), eq(session.getMetadata().getName()), any());
        Service service = new Service();
        try {
            // Only replace cluster DNS discovery. The production polling and publication logic remain intact.
            try (var serviceUrls = mockStatic(TheiaCloudServiceUtil.class)) {
                serviceUrls.when(() -> TheiaCloudServiceUtil.getInternalServiceURL(service)).thenReturn(internalUrl);
                AddedHandlerUtil.updateSessionURLAsync(sessions, session, "regression", publicUrl, service, "test");
            }
            exercised.await(7, TimeUnit.SECONDS);
            // Give the response reader time to process the second response before examining publication.
            assertFalse(published.await(1, TimeUnit.SECONDS), "URL published while an endpoint was unavailable");
            assertTrue(publicRequests.get() >= 2);
            publicCode.set(200);
            appCode.set(200);
            assertTrue(published.await(8, TimeUnit.SECONDS), "URL was not published after recovery");
            assertEquals(publicUrl, session.getStatus().getUrl());
            verify(sessions, times(1)).editStatus(anyString(), eq(session.getMetadata().getName()), any());
        } finally {
            // Let a still-running production worker finish before closing the local servers.
            publicCode.set(200);
            appCode.set(200);
            published.await(8, TimeUnit.SECONDS);
            proxy.stop(0);
            app.stop(0);
        }
    }
}
