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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AdminOnlyFilterTests {

    @Mock
    ContainerRequestContext requestContext;

    @Mock
    UriInfo uriInfo;

    @Mock
    Logger logger;

    @InjectMock
    TheiaCloudUser theiaCloudUser;

    @Inject
    AdminOnlyFilter fixture;

    @BeforeEach
    void setUp() throws Exception {
        // Initialize mocks created above
        MockitoAnnotations.openMocks(this);

        // Manually inject the logger into the filter because using @InjectMock leads to an error in LoggerProducer.
        fixture.logger = logger;

        // Mock method and URI info
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("test/path");
    }

    /**
     * Verifies that when an admin user is injected into the filter, the filter does not abort the request.
     */
    @Test
    void intercept_adminUser_proceed() throws Exception {
        // Configure Theia Cloud user as admin.
        when(theiaCloudUser.isAdmin()).thenReturn(true);

        // Run the filter.
        fixture.filter(requestContext);

        // Verify that abortWith was never called.
        verify(requestContext, never()).abortWith(any());
    }

    /**
     * Verifies that when a non-admin user is injected into the filter, the filter aborts the request with a 403
     * Forbidden status.
     */
    @Test
    void intercept_nonAdminUser_throwForbidden() throws Exception {
        // Configure Theia Cloud user as non-admin.
        when(theiaCloudUser.isAdmin()).thenReturn(false);
        when(theiaCloudUser.getIdentifier()).thenReturn("test-user");

        // Run the filter.
        fixture.filter(requestContext);

        // Capture the Response passed to abortWith.
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(responseCaptor.capture());
        Response response = responseCaptor.getValue();

        // Verify that the response has status 403 and contains the expected error message.
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        assertEquals("Admin privileges required to access this resource.", response.getEntity());
        /// Verify something was logged
        verify(logger).infov(anyString(), anyString(), anyString(), eq("test-user"));
    }
}
