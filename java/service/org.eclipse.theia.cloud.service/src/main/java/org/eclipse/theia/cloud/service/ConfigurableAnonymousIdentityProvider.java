/********************************************************************************
 * Copyright (C) 2022-2023 EclipseSource, STMicroelectronics and others.
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

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AnonymousAuthenticationRequest;
import io.quarkus.security.runtime.AnonymousIdentityProvider;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Custom {@link io.quarkus.security.identity.IdentityProvider IdentityProvider} that authenticates anonymous users if
 * the Theia Cloud service's usage of keycloak was disabled. This facilitates configuring resources as generally
 * requiring authentication while still being able to use the service without any authentication by setting the
 * {@value #THEIACLOUD_USE_KEYCLOAK} system property.
 */
@ApplicationScoped
public class ConfigurableAnonymousIdentityProvider extends AnonymousIdentityProvider {

    @Inject
    private ApplicationProperties applicationProperties;

    @Override
    public Class<AnonymousAuthenticationRequest> getRequestType() {
        return AnonymousAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(AnonymousAuthenticationRequest request,
            AuthenticationRequestContext context) {
        if (applicationProperties.isUseKeycloak()) {
            // If keycloak is used, anonymous requests are handled with their default
            // behavior. That is, the user will not be authenticated and won't be able to
            // access any resources requiring authentication.
            return super.authenticate(request, context);
        }

        // We don't use keycloak. Thus, anonymous requests must be treated as
        // authenticated users to allow access to the service.
        // Every authenticated identity needs a principal. We use the singleton
        // anonymous principal here.
        SecurityIdentity authenticatedIdentity = QuarkusSecurityIdentity.builder()
                .setPrincipal(AnonymousPrincipal.getInstance()).setAnonymous(false).build();
        return Uni.createFrom().item(authenticatedIdentity);
    }
}
