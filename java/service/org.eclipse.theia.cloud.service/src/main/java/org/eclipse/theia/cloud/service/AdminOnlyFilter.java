/********************************************************************************
 * Copyright (C) 2025 EclipseSource and others.
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
package org.eclipse.theia.cloud.service;

import org.jboss.logging.Logger;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.core.Response;

/**
 * ContainerRequestFilter aborting all requests from non admin users to resources annotated with {@link AdminOnly}.
 */
@Provider
@AdminOnly
@Priority(Priorities.AUTHORIZATION)
public class AdminOnlyFilter implements ContainerRequestFilter {

    @Inject
    Logger logger;

    @Inject
    TheiaCloudUser theiaCloudUser;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (!theiaCloudUser.isAdmin()) {
            logger.infov("Blocked access to {0} {1} for non-admin user: {2}", requestContext.getMethod(),
                    requestContext.getUriInfo().getPath(), theiaCloudUser.getIdentifier());
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .entity("Admin privileges required to access this resource.").build());
        }
    }
}
