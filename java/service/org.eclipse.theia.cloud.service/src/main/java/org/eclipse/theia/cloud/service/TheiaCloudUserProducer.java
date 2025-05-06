/********************************************************************************
 * Copyright (C) 2022-2025 EclipseSource, STMicroelectronics and others.
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

import java.util.Set;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

/**
 * <p>
 * Produces an injectable theia cloud user instance based on the authenticated user. This maps the user's
 * {@link SecurityIdentity} to an injectable {@link TheiaCloudUser} POJO.
 * </p>
 * <p>
 * With this, the {@link TheiaCloudUser} can directly be injected into any resource.
 * </p>
 * <p>
 * This producer derives the user identity from an OIDC token. Thereby, the following claims from the MicroProfile JWT
 * specification are used:
 * <ul>
 * <li>{@link Claims#email email} - The user's email address as their unique identifier</li>
 * <li>{@link Claims#groups groups} - The user's groups to determine additional permissions, i.e. admin users</li>
 * </ul>
 */
@RequestScoped
public class TheiaCloudUserProducer {

    private final Logger logger = Logger.getLogger(getClass());

    @Inject
    private SecurityIdentity identity;

    @Inject
    private ApplicationProperties applicationProperties;

    @Produces
    @RequestScoped
    TheiaCloudUser getTheiaCloudUser() {
        if (identity.isAnonymous()) {
            logger.debug("Did not create user identity: User is anonymous");
            return TheiaCloudUser.ANONYMOUS;
        }

        if (identity.getPrincipal() instanceof JsonWebToken) {
            JsonWebToken jwt = (JsonWebToken) identity.getPrincipal();
            String email = jwt.getClaim(Claims.email);
            if (email == null || email.isBlank()) {
                logger.error("Cannot create user identity: The email claim is not available. Treat user as anonymous.");
                return TheiaCloudUser.ANONYMOUS;
            }

            // Check if the user is an admin by looking for the configured admin group name in the groups claim.
            boolean isAdmin = false;
            Set<String> groupsClaim = jwt.getClaim(Claims.groups);
            if (groupsClaim != null) {
                if (groupsClaim.contains(applicationProperties.getAdminGroupName())) {
                    isAdmin = true;
                }
            } else {
                logger.warn("JWT groups claim is null. Cannot grant additional user privileges.");
            }

            return new TheiaCloudUser(email, isAdmin);
        } else if (identity.getPrincipal() instanceof AnonymousPrincipal) {
            // When keycloak is disabled, the security identity is authenticated, i.e. not
            // anonymous. However, the user still has no identity and, thus, is regarded as
            // anonymous for anything but being logged in. This means they can access
            // endpoints that require authentication but have no further privileges.
            logger.debug(
                    "Cannot create user identity: Keycloak is disabled resulting in an authenticated anonymous user.");
            return TheiaCloudUser.ANONYMOUS;
        }

        // Should never happen when using OpenID Connect but log just in case.
        logger.errorv("Cannot create user identity: Auth token is not a JWT but a {0}. Treat user as anonymous.",
                identity.getPrincipal() != null ? identity.getPrincipal().getClass().getName() : "<unknown>");
        return TheiaCloudUser.ANONYMOUS;
    }
}
