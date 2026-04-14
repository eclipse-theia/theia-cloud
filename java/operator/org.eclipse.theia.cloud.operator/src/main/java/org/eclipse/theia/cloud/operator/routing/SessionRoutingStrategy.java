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
package org.eclipse.theia.cloud.operator.routing;

import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;

import io.fabric8.kubernetes.api.model.Service;

/**
 * Strategy interface that abstracts session routing resource management.
 * <p>
 * Implementations are selected via the cloud provider configuration.
 */
public interface SessionRoutingStrategy {

    /**
     * Check if the base routing resource exists for an AppDefinition and add owner
     * references if missing.
     *
     * @param appDefinition the AppDefinition to check
     * @param correlationId the correlation ID for logging
     * @return true if the routing resource exists, false otherwise
     */
    boolean ensureRoutingResourceExists(AppDefinition appDefinition, String correlationId);

    /**
     * Create or update routing for a new session (lazy start). The routing path is
     * derived from the session's UID.
     *
     * @param session       the session being added
     * @param appDefinition the AppDefinition for the session
     * @param service       the Service created for the session
     * @param correlationId the correlation ID for logging
     * @return the session URL (host + path) for this session, or {@code null} if
     *         routing could not be established
     */
    String addSessionRouting(Session session, AppDefinition appDefinition, Service service, String correlationId);

    /**
     * Create or update routing for a new session (eager start). The routing path is
     * derived from the pre-allocated instance number.
     *
     * @param session       the session being added
     * @param appDefinition the AppDefinition for the session
     * @param service       the Service to route to
     * @param instance      the instance number for path computation
     * @param correlationId the correlation ID for logging
     * @return the session URL (host + path) for this session, or {@code null} if
     *         routing could not be established
     */
    String addSessionRouting(Session session, AppDefinition appDefinition, Service service, int instance,
            String correlationId);

    /**
     * Clean up routing when a session is deleted (lazy start). The routing path is
     * derived from the session.
     *
     * @param session       the session being deleted
     * @param appDefinition the AppDefinition for the session
     * @param correlationId the correlation ID for logging
     * @return true if cleanup succeeded, false otherwise
     */
    boolean removeSessionRouting(Session session, AppDefinition appDefinition, String correlationId);

    /**
     * Clean up routing when a session is deleted (eager start). The routing path is
     * derived from the instance number.
     *
     * @param session       the session being deleted
     * @param appDefinition the AppDefinition for the session
     * @param instance      the instance number for path computation
     * @param correlationId the correlation ID for logging
     * @return true if cleanup succeeded, false otherwise
     */
    boolean removeSessionRouting(Session session, AppDefinition appDefinition, int instance, String correlationId);

    /**
     * Compute the full session URL for a given session (lazy start). This does not
     * create any routing resources; it only computes the URL that the session will
     * be reachable at.
     *
     * @param appDefinition the AppDefinition for the session
     * @param session       the session
     * @return the full session URL (e.g.
     *         {@code https://host/path/} for Ingress or
     *         {@code https://uid.host/} for Routes)
     */
    String getSessionURL(AppDefinition appDefinition, Session session);

    /**
     * Compute the full session URL for a given instance (eager start). This does
     * not create any routing resources; it only computes the URL that the session
     * will be reachable at.
     *
     * @param appDefinition the AppDefinition for the session
     * @param instance      the instance number
     * @return the full session URL
     */
    String getSessionURL(AppDefinition appDefinition, int instance);
}
