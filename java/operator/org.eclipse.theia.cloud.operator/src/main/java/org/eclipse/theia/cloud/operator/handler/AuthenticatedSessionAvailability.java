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

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;
import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatMetric;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.SessionResourceClient;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.operator.util.TheiaCloudServiceUtil;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClientException;

/** Availability polling for sessions whose public URL is protected by oauth2-proxy. */
final class AuthenticatedSessionAvailability {
    private static final Logger LOGGER = LogManager.getLogger(AuthenticatedSessionAvailability.class);
    private static final int TIMEOUT_MILLIS = 5000;

    @FunctionalInterface
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    @FunctionalInterface
    interface Connections {
        HttpURLConnection open(URI uri) throws IOException;
    }

    private final Executor executor;
    private final Sleeper sleeper;
    private final Connections connections;
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();

    AuthenticatedSessionAvailability(Executor executor, Sleeper sleeper, Connections connections) {
        this.executor = executor;
        this.sleeper = sleeper;
        this.connections = connections;
    }

    void updateSessionURLAsync(SessionResourceClient sessions, Session session, String namespace, String url,
            Service internalService, String correlationId) {
        if (session.getNonNullStatus().hasUrl()) {
            return;
        }
        final URI internalUrl;
        final URI publicUrl;
        String uid = session.getMetadata().getUid();
        try {
            if (uid == null || uid.isBlank() || namespace == null || namespace.isBlank()) {
                throw new IllegalArgumentException("Session namespace and UID are required for availability polling");
            }
            internalUrl = TheiaCloudServiceUtil.getInternalServiceURL(internalService);
            publicUrl = URI.create(url);
            if (!"http".equals(publicUrl.getScheme()) && !"https".equals(publicUrl.getScheme())) {
                throw new IllegalArgumentException("Session URL must use HTTP or HTTPS");
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error(formatLogMessage(correlationId, "Cannot check authenticated session availability: "
                    + e.getMessage()));
            return;
        }
        String key = namespace + "/" + uid;
        if (!inFlight.add(key)) {
            return;
        }
        try {
            executor.execute(() -> {
                try {
                    poll(sessions, session, url, publicUrl, internalUrl, correlationId);
                } finally {
                    inFlight.remove(key);
                }
            });
        } catch (RuntimeException e) {
            inFlight.remove(key);
            throw e;
        }
    }

    private void poll(SessionResourceClient sessions, Session session, String url, URI publicUrl, URI internalUrl,
            String correlationId) {
        boolean pendingPublicResponse = false;
        for (int attempt = 1; attempt <= 100; attempt++) {
            try {
                sleeper.sleep(attempt <= 15 ? 2500 : attempt <= 30 ? 5000 : attempt <= 45 ? 10000 : 60000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            try {
                var current = sessions.get(session.getMetadata().getName());
                if (current.isEmpty() || !isPendingSession(current.get(), session.getMetadata().getUid())) {
                    return;
                }
                int publicCode = responseCode(publicUrl, false, correlationId);
                if (publicCode < 100 || publicCode == 404 || publicCode == 503) {
                    pendingPublicResponse = false;
                    continue;
                }
                if (publicCode != 200 && !pendingPublicResponse) {
                    pendingPublicResponse = true;
                    continue;
                }
                // Always check this instance afresh immediately before publishing the public URL.
                pendingPublicResponse = false;
                int internalCode = responseCode(internalUrl, true, correlationId);
                if (internalCode >= 200 && internalCode < 400) {
                    AtomicBoolean published = new AtomicBoolean();
                    sessions.editStatus(correlationId, session.getMetadata().getName(), currentSession -> {
                        // The name may have been reused while the requests were in flight or during a conflict retry.
                        published.set(false);
                        if (isPendingSession(currentSession, session.getMetadata().getUid())) {
                            var status = currentSession.getNonNullStatus();
                            status.setUrl(url);
                            currentSession.setStatus(status);
                            published.set(true);
                        }
                    });
                    if (published.get()) {
                        LOGGER.info(formatLogMessage(correlationId, url + " and its application are available."));
                        LOGGER.info(formatMetric(correlationId, "Running session for " + session.getSpec().getAppDefinition()));
                    }
                    return;
                }
            } catch (KubernetesClientException e) {
                pendingPublicResponse = false;
                LOGGER.warn(formatLogMessage(correlationId, "Could not read or update Session availability; retrying"), e);
            }
        }
        LOGGER.warn(formatLogMessage(correlationId, "Session availability checks exhausted for " + url));
    }

    private boolean isPendingSession(Session session, String uid) {
        return uid.equals(session.getMetadata().getUid()) && session.getMetadata().getDeletionTimestamp() == null
                && !session.getNonNullStatus().hasUrl() && !session.getNonNullStatus().hasError();
    }

    private int responseCode(URI uri, boolean internal, String correlationId) {
        HttpURLConnection connection = null;
        try {
            connection = connections.open(uri);
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            if (internal) {
                connection.setInstanceFollowRedirects(false);
            } else {
                AddedHandlerUtil.configurePublicConnection(connection);
            }
            connection.connect();
            int code = connection.getResponseCode();
            LOGGER.trace(formatLogMessage(correlationId, uri + " has response code " + code));
            return code;
        } catch (IOException | GeneralSecurityException | IllegalArgumentException e) {
            LOGGER.debug(formatLogMessage(correlationId, uri + " is NOT available yet."), e);
            return -1;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
