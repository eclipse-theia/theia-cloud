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
import javax.ws.rs.Path;
import javax.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

/**
 * Tests for {@link NoAnonymousAccessInterceptor}.
 */
@QuarkusTest
public class NoAnonymousAccessInterceptorTests {

    @InjectMock
    ApplicationProperties applicationProperties;

    @InjectMock
    TheiaCloudUser user;

    @Inject
    NoAnonymousAccessWithMethodAnnotationTestResource methodFixture;

    @Inject
    NoAnonymousAccessWithMethodAnnotationTestResource clazzFixture;

    @Test
    void intercept_useKeyloakWithIdentifiedUser_proceed() {
	Mockito.when(applicationProperties.isUseKeycloak()).thenReturn(true);
	mockUser("someuser");

	methodFixture.execute();
	clazzFixture.execute();
    }

    @Test
    void intercept_useKeyloakWithoutIdentifiedUser_throwForbidden() throws Exception {
	Mockito.when(applicationProperties.isUseKeycloak()).thenReturn(true);
	mockUser(null);

	TheiaCloudWebException methodException = assertThrows(TheiaCloudWebException.class, () -> {
	    methodFixture.execute();
	});
	assertEquals(Status.FORBIDDEN.getStatusCode(), methodException.getResponse().getStatus());

	TheiaCloudWebException clazzException = assertThrows(TheiaCloudWebException.class, () -> {
	    clazzFixture.execute();
	});
	assertEquals(Status.FORBIDDEN.getStatusCode(), clazzException.getResponse().getStatus());
    }

    @Test
    void intercept_noKeyloak_throwForbidden() {
	Mockito.when(applicationProperties.isUseKeycloak()).thenReturn(false);
	mockUser(null);

	TheiaCloudWebException methodException = assertThrows(TheiaCloudWebException.class, () -> {
	    methodFixture.execute();
	});
	assertEquals(Status.FORBIDDEN.getStatusCode(), methodException.getResponse().getStatus());

	TheiaCloudWebException clazzException = assertThrows(TheiaCloudWebException.class, () -> {
	    clazzFixture.execute();
	});
	assertEquals(Status.FORBIDDEN.getStatusCode(), clazzException.getResponse().getStatus());
    }

    private void mockUser(String name) {
	Mockito.when(user.getIdentifier()).thenReturn(name);
	Mockito.when(user.isAnonymous()).thenReturn(name == null || name.isBlank());
    }

    /*
     * Configure a test bean like a resource that uses the @NoAnonymousAccess
     * annotation: With a Path annotation and an annotated method.
     */
    @Path("/some/path")
    static class NoAnonymousAccessWithMethodAnnotationTestResource {
	@NoAnonymousAccess
	void execute() {
	}
    }

    /*
     * Configure a test bean like a resource that uses the @NoAnonymousAccess
     * annotation: With a Path annotation and the annotation placed on the resource
     * class.
     */
    @Path("/someother/path")
    @NoAnonymousAccess
    static class NoAnonymousAccessWithClassAnnotationTestResource {
	void execute() {
	}
    }
}
