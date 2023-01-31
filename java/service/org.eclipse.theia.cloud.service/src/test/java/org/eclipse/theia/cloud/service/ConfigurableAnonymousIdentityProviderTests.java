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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Supplier;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AnonymousAuthenticationRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.mutiny.Uni;

/**
 * Unit tests for
 * {@link org.eclipse.theia.cloud.service.ConfigurableAnonymousIdentityProvider}
 */
@QuarkusTest
class ConfigurableAnonymousIdentityProviderTests {

    @InjectMock
    ApplicationProperties applicationProperties;

    @Inject
    ConfigurableAnonymousIdentityProvider fixture;

    @Test
    void authenticate_useKeycloak_anonymousIdentity() {
	Mockito.when(applicationProperties.isUseKeycloak()).thenReturn(true);

	Uni<SecurityIdentity> result = fixture.authenticate(AnonymousAuthenticationRequest.INSTANCE,
		new TestAuthenticationRequestContext());
	SecurityIdentity securityIdentity = result.await().indefinitely();

	assertTrue(securityIdentity.isAnonymous());
    }

    @Test
    void authenticate_noKeycloak_authenticatedIdentity() {
	Mockito.when(applicationProperties.isUseKeycloak()).thenReturn(false);

	Uni<SecurityIdentity> result = fixture.authenticate(AnonymousAuthenticationRequest.INSTANCE,
		new TestAuthenticationRequestContext());
	SecurityIdentity securityIdentity = result.await().indefinitely();

	assertFalse(securityIdentity.isAnonymous());
	assertNotNull(securityIdentity.getPrincipal());
	assertTrue(securityIdentity.getRoles().isEmpty());
    }

    class TestAuthenticationRequestContext implements AuthenticationRequestContext {

	@Override
	public Uni<SecurityIdentity> runBlocking(Supplier<SecurityIdentity> function) {
	    return Uni.createFrom().item(function);
	}

    }
}
