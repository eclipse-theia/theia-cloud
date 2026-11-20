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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.sun.net.httpserver.HttpServer;

class SessionAvailabilityRegressionTests {
    @ParameterizedTest
    @CsvSource({"404,503,302", "503,403,200"})
    void waitsForTheApplicationAndPublishesThePublicUrl(int publicFailure, int internalFailure, int internalSuccess)
            throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger publicRequests = new AtomicInteger();
        AtomicInteger internalRequests = new AtomicInteger();
        AtomicInteger followedInternalRedirects = new AtomicInteger();
        server.createContext("/public", exchange -> {
            int attempt = publicRequests.incrementAndGet();
            // Reach the login page first, then exercise the public fallback around a failed request.
            int code = switch (attempt) {
                case 1 -> 302;
                case 2, 4, 5 -> 401;
                case 3 -> publicFailure;
                default -> 200;
            };
            exchange.getResponseHeaders().add("Location", "/login");
            exchange.sendResponseHeaders(code, -1);
            exchange.close();
        });
        server.createContext("/login", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.createContext("/internal", exchange -> {
            int code = internalRequests.incrementAndGet() == 1 ? internalFailure : internalSuccess;
            exchange.getResponseHeaders().add("Location", "/redirect-target");
            exchange.sendResponseHeaders(code, -1);
            exchange.close();
        });
        server.createContext("/redirect-target", exchange -> {
            followedInternalRedirects.incrementAndGet();
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });
        server.start();
        try {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            String publicUrl = base.resolve("/public").toString();
            Session session = new Session();
            session.setSpec(new SessionSpec("session", "app", "user@example.com"));
            SessionStatus status = new SessionStatus();
            CountDownLatch published = new CountDownLatch(1);
            SessionResourceClient sessions = mock(SessionResourceClient.class);
            doAnswer(invocation -> {
                Consumer<SessionStatus> update = invocation.getArgument(2);
                update.accept(status);
                published.countDown();
                return session;
            }).when(sessions).updateStatus(eq("test"), eq(session), any());

            AddedHandlerUtil.updateSessionURLAsync(sessions, session, "sessions", publicUrl,
                    Optional.of(base.resolve("/internal")), "test");

            assertTrue(published.await(25, TimeUnit.SECONDS), "Timed out waiting for URL publication");
            assertEquals(5, publicRequests.get());
            assertEquals(2, internalRequests.get());
            assertEquals(0, followedInternalRedirects.get());
            assertEquals(publicUrl, status.getUrl());
        } finally {
            server.stop(0);
        }
    }
}
