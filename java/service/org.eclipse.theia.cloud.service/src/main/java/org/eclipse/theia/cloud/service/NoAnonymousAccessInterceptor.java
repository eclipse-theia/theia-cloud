/********************************************************************************
 * Copyright (C) 2023 EclipseSource and others.
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

import java.lang.reflect.Method;
import java.util.Optional;

import org.jboss.logging.Logger;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.core.Response.Status;

/**
 * Interceptor handling the {@link NoAnonymousAccess} annotation.
 */
@Interceptor
@NoAnonymousAccess
@Priority(value = Interceptor.Priority.APPLICATION)
public class NoAnonymousAccessInterceptor {

    @Inject
    Logger logger;

    @Inject
    ApplicationProperties applicationProperties;

    @Inject
    TheiaCloudUser user;

    /**
     * Verifies that the user is not anonymous and Theia Cloud does not run in anonymous mode. Throws an exception for
     * anonymous access.
     * 
     * @throws Exception              if following interceptors fail
     * @throws TheiaCloudWebException on anonymous access
     */
    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        if (!applicationProperties.isUseKeycloak()) {
            logger.infov("Blocked anonymous access to method {0}#{1}", ctx.getMethod().getDeclaringClass().getName(),
                    ctx.getMethod().getName());
            // Note: Another possibility is to throw a 401 (Unauthorized) because the user
            // is not authenticated.
            // However, there is also no possible user account in anonymous mode to achieve
            // this.
            throw new TheiaCloudWebException(Status.FORBIDDEN);
        } else if (user.isAnonymous()) {
            // Authentication is used and succeeded but the user is considered anonymous for
            // other reasons such as not having a user identifier
            final Optional<Method> method = Optional.ofNullable(ctx.getMethod());
            logger.infov("Blocked authenticated access with anonymous user profile to method {0}#{1}",
                    method.map(m -> m.getDeclaringClass().getName()).orElse("<unknown>"),
                    method.map(Method::getName).orElse("<unknown>"));
            throw new TheiaCloudWebException(Status.FORBIDDEN);
        }

        return ctx.proceed();
    }
}
