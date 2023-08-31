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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import jakarta.ws.rs.core.Response.Status;

/**
 * Unit tests for {@link RootResource}.
 *
 * Disable authorization via {@link TestSecurity} annotation as this is a unit
 * test of the resource itself. Thus, we do not want authentication interceptors
 * to trigger when calling the resource's methods.
 */
@QuarkusTest
@TestSecurity(authorizationEnabled = false)
class RootResourceTests {

    private static final String APP_ID = "asdfghjkl";
    private static final String TEST_USER = "TestUser";
    private static final String OTHER_TEST_USER = "OtherTestUser";

    @InjectMock
    ApplicationProperties applicationProperties;

    @InjectMock
    TheiaCloudUser user;

    @Inject
    RootResource fixture;

    @BeforeEach
    void mockApplicationProperties() {
	Mockito.when(applicationProperties.isUseKeycloak()).thenReturn(true);
    }

    @Test
    void launch_otherUser_throwForbidden() {
	// Prepare
	mockUser(false, TEST_USER);
	LaunchRequest launchRequest = new LaunchRequest();
	launchRequest.appId = APP_ID;
	launchRequest.user = OTHER_TEST_USER;

	// Execute
	TheiaCloudWebException exception = assertThrows(TheiaCloudWebException.class, () -> {
	    fixture.launch(launchRequest);

	});

	// Assert
	assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getResponse().getStatus());
    }

    // ---
    // Utility methods
    // ---

    private void mockUser(boolean anonymous, String name) {
	Mockito.when(user.isAnonymous()).thenReturn(anonymous);
	Mockito.when(user.getIdentifier()).thenReturn(name);
    }
}
