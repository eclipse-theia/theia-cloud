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
package org.eclipse.theia.cloud.service.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.core.Response.Status;

import org.eclipse.theia.cloud.common.k8s.resource.SessionSpec;
import org.eclipse.theia.cloud.common.util.TheiaCloudError;
import org.eclipse.theia.cloud.service.K8sUtil;
import org.eclipse.theia.cloud.service.TheiaCloudUser;
import org.eclipse.theia.cloud.service.TheiaCloudWebException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;

/**
 * Unit tests for
 * {@link org.eclipse.theia.cloud.service.session.SessionResource}.
 * 
 * Disable authorization via {@link TestSecurity} annotation as this is a unit
 * test of the resource itself. Thus, we do not want authentication interceptors
 * to trigger when calling the resource's methods.
 */
@QuarkusTest
@TestSecurity(authorizationEnabled = false)
class SessionResourceTests {

    private static final String APP_ID = "asdfghjkl";
    private static final String TEST_USER = "TestUser";
    private static final String TEST_SESSION = "TestSession";

    @InjectMock
    K8sUtil k8sUtil;

    @InjectMock
    TheiaCloudUser user;

    @Inject
    SessionResource fixture;

    /**
     * Test method for
     * {@link org.eclipse.theia.cloud.service.session.SessionResource#stop(org.eclipse.theia.cloud.service.session.SessionStopRequest)}.
     */
    @Test
    void stop_matchingUser_true() {
	// Prepare
	mockUser(false, TEST_USER);
	SessionSpec session = mockDefaultSession();
	SessionStopRequest request = new SessionStopRequest(APP_ID, TEST_USER, TEST_SESSION);
	Mockito.when(k8sUtil.findSession(TEST_SESSION)).thenReturn(Optional.of(session));
	Mockito.when(k8sUtil.stopSession(anyString(), eq(TEST_SESSION), eq(TEST_USER))).thenReturn(true);

	// Execute
	boolean result = fixture.stop(request);

	// Assert
	Mockito.verify(k8sUtil).stopSession(anyString(), eq(TEST_SESSION), eq(TEST_USER));
	assertEquals(true, result);
    }

    /**
     * Test method for
     * {@link org.eclipse.theia.cloud.service.session.SessionResource#stop(org.eclipse.theia.cloud.service.session.SessionStopRequest)}.
     */
    @Test
    void stop_otherUser_throwForbidden() {
	// Prepare
	mockUser(false, "OtherTestUser");
	SessionSpec session = mockDefaultSession();
	Mockito.when(k8sUtil.findSession(TEST_SESSION)).thenReturn(Optional.of(session));

	// We leave the matching user in the request to verify that the stop is
	// denied even if the correct user is specified in the request.
	// After all, an attacker could do this.
	SessionStopRequest request = new SessionStopRequest(APP_ID, TEST_USER, TEST_SESSION);

	// Execute
	TheiaCloudWebException exception = assertThrows(TheiaCloudWebException.class, () -> {
	    fixture.stop(request);
	});

	// Assert
	Mockito.verify(k8sUtil, never()).stopSession(anyString(), anyString(), anyString());
	assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getResponse().getStatus());
    }

    /**
     * Test method for
     * {@link org.eclipse.theia.cloud.service.session.SessionResource#stop(org.eclipse.theia.cloud.service.session.SessionStopRequest)}.
     */
    @Test
    void stop_otherUserWithNullName_throwForbidden() {
	// Prepare
	mockUser(false, null);
	SessionSpec session = mockDefaultSession();
	Mockito.when(k8sUtil.findSession(TEST_SESSION)).thenReturn(Optional.of(session));

	// We leave the matching user in the request to verify that the stop is
	// denied even if the correct user is specified in the request.
	// After all, an attacker could do this.
	SessionStopRequest request = new SessionStopRequest(APP_ID, TEST_USER, TEST_SESSION);

	// Execute
	TheiaCloudWebException exception = assertThrows(TheiaCloudWebException.class, () -> {
	    fixture.stop(request);
	});

	// Assert
	Mockito.verify(k8sUtil, never()).stopSession(anyString(), anyString(), anyString());
	assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getResponse().getStatus());
    }

    /**
     * Test method for
     * {@link org.eclipse.theia.cloud.service.session.SessionResource#stop(org.eclipse.theia.cloud.service.session.SessionStopRequest)}.
     */
    @Test
    void stop_sessionNotExisting_true() {
	// Prepare
	mockUser(false, TEST_USER);
	Mockito.when(k8sUtil.findSession(TEST_SESSION)).thenReturn(Optional.empty());
	SessionStopRequest request = new SessionStopRequest(APP_ID, TEST_USER, TEST_SESSION);

	// Execute
	boolean result = fixture.stop(request);

	// Assert
	Mockito.verify(k8sUtil, never()).stopSession(anyString(), anyString(), anyString());
	assertEquals(true, result);
    }

    @Test
    void stop_noRequestSessionName_throwMissingSessionName() {
	// Prepare
	mockUser(false, TEST_USER);
	SessionStopRequest request = new SessionStopRequest(APP_ID, TEST_USER, null);

	// Execute
	TheiaCloudWebException exception = assertThrows(TheiaCloudWebException.class, () -> {
	    fixture.stop(request);
	});

	// Assert
	Mockito.verify(k8sUtil, never()).stopSession(anyString(), anyString(), anyString());
	assertEquals(TheiaCloudError.MISSING_SESSION_NAME.getCode(), exception.getResponse().getStatus());
    }

    /**
     * Test method for
     * {@link org.eclipse.theia.cloud.service.session.SessionResource#stop(org.eclipse.theia.cloud.service.session.SessionStopRequest)}.
     */
    @Test
    void stop_anonymousUser_throwForbidden() {
	// Prepare
	mockUser(true, null);
	SessionSpec session = mockDefaultSession();
	Mockito.when(k8sUtil.findSession(TEST_SESSION)).thenReturn(Optional.of(session));

	// We leave the matching user in the request to verify that the stop is
	// denied even if the correct user is specified in the request.
	// After all, an attacker could do this.
	SessionStopRequest request = new SessionStopRequest(APP_ID, TEST_USER, TEST_SESSION);

	// Execute
	TheiaCloudWebException exception = assertThrows(TheiaCloudWebException.class, () -> {
	    fixture.stop(request);
	});

	// Assert
	Mockito.verify(k8sUtil, never()).stopSession(anyString(), anyString(), anyString());
	assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getResponse().getStatus());
    }

    // ---
    // Utility methods
    // ---

    private void mockUser(boolean anonymous, String name) {
	Mockito.when(user.isAnonymous()).thenReturn(anonymous);
	Mockito.when(user.getIdentifier()).thenReturn(name);
    }

    private SessionSpec mockDefaultSession() {
	SessionSpec session = Mockito.mock(SessionSpec.class);
	Mockito.when(session.getName()).thenReturn(TEST_SESSION);
	Mockito.when(session.getUser()).thenReturn(TEST_USER);
	return session;
    }
}
