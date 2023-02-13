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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.security.runtime.QuarkusSecurityIdentity.Builder;
import io.smallrye.mutiny.Uni;

/**
 * <p>
 * Augments the security identity with appropriate roles based on the service's
 * configuration and the user's groups.
 * </p>
 * <p>
 * Three configuration cases are handled:
 * <ol>
 * <li>Keycloak is disabled altogether. Any user gets the user role.</li>
 * <li>Keycloak is enabled but the service runs in authentication only mode. Any
 * <strong>authenticated</strong> user is grated the user role.</li>
 * <li>Keycloak is enabled and the service uses authorization. Only users that
 * have the {@link ApplicationProperties#getUserGroup() configured user group}
 * are granted the user role.</li>
 * </ol>
 * </p>
 * 
 */
@ApplicationScoped
public class RolesMapperAugmentor implements SecurityIdentityAugmentor {

    @Inject
    private ApplicationProperties applicationProperties;

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
	if (!applicationProperties.isUseKeycloak()) {
	    // Keycloak is not used. Grant user role
	    return withUserRole(identity);
	} else if (!identity.isAnonymous() && applicationProperties.isAuthenticationOnly()) {
	    // Only authentication is used. Grant any authenticated user the user role
	    return withUserRole(identity);
	} else if (identity.hasRole(applicationProperties.getUserGroup())) {
	    // The if check uses hasRole because Quarkus automatically adds groups from
	    // MicroProfile's groups claim to the security identity's roles.
	    return withUserRole(identity);
	}

	// None of the conditions are fulfilled. Thus, return the identity unchanged.
	return Uni.createFrom().item(identity);
    }

    /**
     * Adds the user role to the given {@link SecurityIdentity}.
     * 
     * @param identity The {@link SecurityIdentity} to agument with the user role
     * @return The augmented {@link SecurityIdentity} wrapped in a {@link Uni} for
     *         lazy evaluation
     */
    protected Uni<SecurityIdentity> withUserRole(SecurityIdentity identity) {
	Builder builder = QuarkusSecurityIdentity.builder(identity).addRole(AccessRoles.USER);
	return Uni.createFrom().item(builder::build);
    }
}
