/********************************************************************************
 * Copyright (C) 2023 EclipseSource, STMicroelectronics and others.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.Principal;
import java.util.Set;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Unit tests for {@link org.eclipse.theia.cloud.service.TheiaCloudUserProducer}
 */
@QuarkusTest
class TheiaCloudUserProducerTests {

    private static final String TEST_EMAIL = "test@example.com";

    @InjectMock
    SecurityIdentity identity;

    @Inject
    TheiaCloudUserProducer fixture;

    @InjectMock
    ApplicationProperties applicationProperties;

    /**
     * Test method for {@link org.eclipse.theia.cloud.service.TheiaCloudUserProducer#getTheiaCloudUser()}.
     */
    @Test
    void getTheiaCloudUser_anonymousIdentity_anonymousTheiaCloudUser() {
        // Prepare
        Mockito.when(identity.isAnonymous()).thenReturn(true);

        // Execute
        TheiaCloudUser result = fixture.getTheiaCloudUser();

        // Assert
        assertTrue(result.isAnonymous());
        assertNull(result.getIdentifier());
    }

    /**
     * Test method for {@link org.eclipse.theia.cloud.service.TheiaCloudUserProducer#getTheiaCloudUser()}.
     */
    @Test
    void getTheiaCloudUser_authenticatedIdentity_TheiaCloudUser() {
        // Prepare
        Mockito.when(identity.isAnonymous()).thenReturn(false);
        JsonWebToken token = Mockito.mock(JsonWebToken.class);
        Mockito.when(token.getClaim(Claims.email)).thenReturn(TEST_EMAIL);
        Mockito.when(identity.getPrincipal()).thenReturn(token);

        // Execute
        TheiaCloudUser result = fixture.getTheiaCloudUser();

        // Assert
        assertFalse(result.isAnonymous());
        assertEquals(TEST_EMAIL, result.getIdentifier());
    }

    /**
     * Test method for {@link org.eclipse.theia.cloud.service.TheiaCloudUserProducer#getTheiaCloudUser()}.
     */
    @Test
    void getTheiaCloudUser_authenticatedIdentityJWTmissingEmailClaim_anonymousTheiaCloudUser() {
        // Prepare
        Mockito.when(identity.isAnonymous()).thenReturn(false);
        JsonWebToken token = Mockito.mock(JsonWebToken.class);
        Mockito.when(token.getClaim(Claims.email)).thenReturn(null);
        Mockito.when(identity.getPrincipal()).thenReturn(token);

        // Execute
        TheiaCloudUser result = fixture.getTheiaCloudUser();

        // Assert
        assertTrue(result.isAnonymous());
        assertNull(result.getIdentifier());
    }

    /**
     * Test method for {@link org.eclipse.theia.cloud.service.TheiaCloudUserProducer#getTheiaCloudUser()}.
     */
    @Test
    void getTheiaCloudUser_authenticatedIdentityJWTwhitespaceEmailClaim_anonymousTheiaCloudUser() {
        // Prepare
        Mockito.when(identity.isAnonymous()).thenReturn(false);
        JsonWebToken token = Mockito.mock(JsonWebToken.class);
        Mockito.when(token.getClaim(Claims.email)).thenReturn(" \t");
        Mockito.when(identity.getPrincipal()).thenReturn(token);

        // Execute
        TheiaCloudUser result = fixture.getTheiaCloudUser();

        // Assert
        assertTrue(result.isAnonymous());
        assertNull(result.getIdentifier());
    }

    /**
     * Test method for {@link org.eclipse.theia.cloud.service.TheiaCloudUserProducer#getTheiaCloudUser()}.
     */
    @Test
    void getTheiaCloudUser_authenticatedIdentityNoJWT_anonymousTheiaCloudUser() {
        // Prepare
        Mockito.when(identity.isAnonymous()).thenReturn(false);
        Principal principal = Mockito.mock(Principal.class);
        Mockito.when(identity.getPrincipal()).thenReturn(principal);

        // Execute
        TheiaCloudUser result = fixture.getTheiaCloudUser();

        // Assert
        assertTrue(result.isAnonymous());
        assertNull(result.getIdentifier());
    }

    /**
     * Test method for {@link org.eclipse.theia.cloud.service.TheiaCloudUserProducer#getTheiaCloudUser()}.
     */
    @Test
    void getTheiaCloudUser_authenticatedIdentityWithAnonymousPrincipal_anonymousTheiaCloudUser() {
        // Prepare
        Mockito.when(identity.isAnonymous()).thenReturn(false);
        Mockito.when(identity.getPrincipal()).thenReturn(AnonymousPrincipal.getInstance());

        // Execute
        TheiaCloudUser result = fixture.getTheiaCloudUser();

        // Assert
        assertTrue(result.isAnonymous());
        assertNull(result.getIdentifier());
    }

    @Test
    void getTheiaCloudUser_authenticatedIdentityJWTwithAdminGroup_adminTheiaCloudUser() {
        // Prepare
        Mockito.when(identity.isAnonymous()).thenReturn(false);
        JsonWebToken token = Mockito.mock(JsonWebToken.class);
        Mockito.when(token.getClaim(Claims.email)).thenReturn("admin@example.com");
        Mockito.when(token.getClaim(Claims.groups)).thenReturn(Set.of("theia-cloud-admin"));
        Mockito.when(identity.getPrincipal()).thenReturn(token);
        Mockito.when(applicationProperties.getAdminGroupName()).thenReturn("theia-cloud-admin");

        // Execute
        TheiaCloudUser result = fixture.getTheiaCloudUser();

        // Assert
        assertFalse(result.isAnonymous());
        assertTrue(result.isAdmin());
        assertEquals("admin@example.com", result.getIdentifier());
    }

    @Test
    void getTheiaCloudUser_authenticatedIdentityJWTwithEmptyGroupsClaim_nonAdminTheiaCloudUser() {
        // Prepare
        Mockito.when(identity.isAnonymous()).thenReturn(false);
        JsonWebToken token = Mockito.mock(JsonWebToken.class);
        Mockito.when(token.getClaim(Claims.email)).thenReturn("user@example.com");
        Mockito.when(token.getClaim(Claims.groups)).thenReturn(Set.of());
        Mockito.when(identity.getPrincipal()).thenReturn(token);
        Mockito.when(applicationProperties.getAdminGroupName()).thenReturn("theia-cloud-admin");

        // Execute
        TheiaCloudUser result = fixture.getTheiaCloudUser();

        // Assert
        assertFalse(result.isAnonymous());
        assertFalse(result.isAdmin());
        assertEquals("user@example.com", result.getIdentifier());
    }

    @Test
    void getTheiaCloudUser_authenticatedIdentityJWTwithNullGroupsClaim_nonAdminTheiaCloudUser() {
        // Prepare
        Mockito.when(identity.isAnonymous()).thenReturn(false);
        JsonWebToken token = Mockito.mock(JsonWebToken.class);
        Mockito.when(token.getClaim(Claims.email)).thenReturn("user@example.com");
        Mockito.when(token.getClaim(Claims.groups)).thenReturn(null);
        Mockito.when(identity.getPrincipal()).thenReturn(token);
        Mockito.when(applicationProperties.getAdminGroupName()).thenReturn("theia-cloud-admin");

        // Execute
        TheiaCloudUser result = fixture.getTheiaCloudUser();

        // Assert
        assertFalse(result.isAnonymous());
        assertFalse(result.isAdmin());
        assertEquals("user@example.com", result.getIdentifier());
    }

    @Test
    void getTheiaCloudUser_authenticatedIdentityJWTwithDifferentGroup_nonAdminTheiaCloudUser() {
        // Prepare
        Mockito.when(identity.isAnonymous()).thenReturn(false);
        JsonWebToken token = Mockito.mock(JsonWebToken.class);
        Mockito.when(token.getClaim(Claims.email)).thenReturn("user@example.com");
        Mockito.when(token.getClaim(Claims.groups)).thenReturn(Set.of("some-other-group"));
        Mockito.when(identity.getPrincipal()).thenReturn(token);
        Mockito.when(applicationProperties.getAdminGroupName()).thenReturn("theia-cloud-admin");

        // Execute
        TheiaCloudUser result = fixture.getTheiaCloudUser();

        // Assert
        assertFalse(result.isAnonymous());
        assertFalse(result.isAdmin());
        assertEquals("user@example.com", result.getIdentifier());
    }
}
