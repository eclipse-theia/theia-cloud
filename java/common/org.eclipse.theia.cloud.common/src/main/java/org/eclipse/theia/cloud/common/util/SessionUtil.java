/********************************************************************************
 * Copyright (C) 2022 EclipseSource, logi.cals and others.
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
package org.eclipse.theia.cloud.common.util;

import java.util.Optional;

import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;

public final class SessionUtil {

    private SessionUtil() {
        // util
    }

    /**
     * Get the cluster URL of the session pod, if available. This is the cluster internal URL of the pod.
     *
     * @param client  The Theia Cloud K8s client to use for the request.
     * @param session The session to get the cluster IP for.
     * @return The cluster IP of the session pod, if available.
     */
    public static Optional<String> getClusterURL(TheiaCloudClient client, Session session) {
        Optional<String> ip = getClusterIP(client, session);
        Optional<Integer> port = getPort(client, session);
        if (ip.isPresent() && port.isPresent()) {
            return Optional.of("http://" + ip.get() + ":" + port.get());
        }
        return Optional.empty();
    }

    /**
     * Get the internal cluster URL of the session pod, if available. This is the cluster internal URL of the internal
     * service that bypasses OAuth2 proxy for service-to-service communication.
     *
     * @param client  The Theia Cloud K8s client to use for the request.
     * @param session The session to get the internal cluster IP for.
     * @return The internal cluster URL of the session pod, if available.
     */
    public static Optional<String> getInternalClusterURL(TheiaCloudClient client, Session session) {
        Optional<String> ip = getInternalClusterIP(client, session);
        Optional<Integer> port = getInternalPort();
        if (ip.isPresent() && port.isPresent()) {
            return Optional.of("http://" + ip.get() + ":" + port.get());
        }
        return Optional.empty();
    }

    /**
     * Get the cluster IP of the session pod, if available. The cluster IP is the internal IP address of the pod.
     *
     * @param client  The Theia Cloud K8s client to use for the request.
     * @param session The session to get the cluster IP for.
     * @return The cluster IP of the session pod, if available.
     */
    public static Optional<String> getClusterIP(TheiaCloudClient client, Session session) {
        Optional<String> sessionIP = client.getClusterIPFromSessionName(session.getSpec().getName());
        return sessionIP;
    }

    /**
     * Get the internal cluster IP of the session pod, if available. The internal cluster IP is the IP address of the
     * internal service that bypasses OAuth2 proxy for service-to-service communication.
     *
     * @param client  The Theia Cloud K8s client to use for the request.
     * @param session The session to get the internal cluster IP for.
     * @return The internal cluster IP of the session pod, if available.
     */
    public static Optional<String> getInternalClusterIP(TheiaCloudClient client, Session session) {
        Optional<String> sessionIP = client.getInternalClusterIPFromSessionName(session.getSpec().getName());
        return sessionIP;
    }

    /**
     * Get the port that the session's Theia application is running on. This does not consider the port that the OAuth
     * proxy runs on which is target by external connections.
     *
     * @param client  The Theia Cloud K8s client to use for the request.
     * @param session The session to get the port for.
     * @return The port of the session pod, if available.
     */
    public static Optional<Integer> getPort(TheiaCloudClient client, Session session) {
        String appDefinitionId = session.getSpec().getAppDefinition();
        return client.appDefinitions().get(appDefinitionId)//
                .map(AppDefinition::getSpec)//
                .map(AppDefinitionSpec::getPort);
    }

    /**
     * Get the internal service port for service-to-service communication. This is a fixed port (3001) that the internal
     * services use to bypass OAuth2 proxy.
     *
     * @return The internal service port (3001).
     */
    public static Optional<Integer> getInternalPort() {
        return Optional.of(3001);
    }
}
