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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionSpec;
import org.eclipse.theia.cloud.common.util.TheiaCloudError;
import org.eclipse.theia.cloud.service.ApplicationProperties;
import org.eclipse.theia.cloud.service.K8sUtil;
import org.eclipse.theia.cloud.service.TheiaCloudUser;
import org.eclipse.theia.cloud.service.TheiaCloudWebException;
import org.eclipse.theia.cloud.service.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;

/**
 * Unit tests for {@link org.eclipse.theia.cloud.service.session.SessionResource}. Disable authorization via
 * {@link TestSecurity} annotation as this is a unit test of the resource itself. Thus, we do not want authentication
 * interceptors to trigger when calling the resource's methods.
 */
@QuarkusTest
@TestSecurity(authorizationEnabled = false)
class SessionResourceTests {

    private static final String APP_ID = "asdfghjkl";
    private static final String TEST_APP_DEFINITION = "TestAppDefinition";
    private static final String TEST_USER = "TestUser";
    private static final String OTHER_TEST_USER = "OtherTestUser";
    private static final String TEST_SESSION = "TestSession";

    @InjectMock
    ApplicationProperties applicationProperties;

    @InjectMock
    K8sUtil k8sUtil;

    @InjectMock
    TheiaCloudUser user;

    @Inject
    SessionResource fixture;

    @BeforeEach
    void mockApplicationProperties() {
        Mockito.when(applicationProperties.isUseKeycloak()).thenReturn(true);
    }

    /**
     * Test method for
     * {@link org.eclipse.theia.cloud.service.session.SessionResource#stop(org.eclipse.theia.cloud.service.session.SessionStopRequest)}.
     */
    @Test
    void stop_matchingUser_true() {
        // Prepare
        mockUser(false, TEST_USER);
        Session session = mockDefaultSession();
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
        mockUser(false, OTHER_TEST_USER);
        Session session = mockDefaultSession();
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
        Session session = mockDefaultSession();
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
        Session session = mockDefaultSession();
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
     * {@link org.eclipse.theia.cloud.service.workspace.WorkspaceResource#delete(org.eclipse.theia.cloud.service.workspace.WorkspaceDeletionRequest)}.
     */
    @Test()
    void stop_noKeycloak_throwForbidden() {
        // Prepare
        Mockito.when(applicationProperties.isUseKeycloak()).thenReturn(false);
        mockUser(true, null);
        Session session = mockDefaultSession();
        Mockito.when(k8sUtil.findSession(TEST_SESSION)).thenReturn(Optional.of(session));

        // We leave the matching user in the request to verify that the deletion is
        // denied even if the correct user is specified in the request.
        // After all, an attacker could know this.
        SessionStopRequest request = new SessionStopRequest(APP_ID, TEST_USER, TEST_SESSION);

        // Execute
        TheiaCloudWebException exception = assertThrows(TheiaCloudWebException.class, () -> {
            fixture.stop(request);
        });

        // Assert
        Mockito.verify(k8sUtil, never()).stopSession(anyString(), anyString(), anyString());
        assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getResponse().getStatus());
    }

    @Test
    void stop_hasNoAnonymousAccessAnnotations() throws NoSuchMethodException, SecurityException {
        Method method = SessionResource.class.getMethod("stop", SessionStopRequest.class);
        TestUtil.assertNoAnonymousAccessAnnotations(method);
    }

    @Test
    void list_matchingUser_SessionSpecs() {
        // Prepare
        mockUser(false, TEST_USER);
        List<SessionSpec> resultList = List.of();
        Mockito.when(k8sUtil.listSessions(TEST_USER)).thenReturn(resultList);

        List<SessionSpec> result = fixture.list(APP_ID, TEST_USER);

        assertSame(resultList, result);
    }

    @Test
    void list_otherUser_throwForbidden() {
        // Prepare
        mockUser(false, TEST_USER);

        // Execute
        TheiaCloudWebException exception = assertThrows(TheiaCloudWebException.class, () -> {
            fixture.list(APP_ID, OTHER_TEST_USER);

        });

        // Assert
        Mockito.verify(k8sUtil, never()).listSessions(anyString());
        assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getResponse().getStatus());
    }

    @Test
    void list_noKeycloak_throwForbidden() {
        // Prepare
        mockUser(true, null);
        Mockito.when(applicationProperties.isUseKeycloak()).thenReturn(false);

        // Execute
        TheiaCloudWebException exception = assertThrows(TheiaCloudWebException.class, () -> {
            fixture.list(APP_ID, TEST_USER);

        });

        // Assert
        Mockito.verify(k8sUtil, never()).listSessions(anyString());
        assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getResponse().getStatus());
    }

    @Test
    void list_hasNoAnonymousAccessAnnotations() throws NoSuchMethodException, SecurityException {
        Method method = SessionResource.class.getMethod("list", String.class, String.class);
        TestUtil.assertNoAnonymousAccessAnnotations(method);
    }

    @Test
    void performance_matchingUser_SessionPerformance() {
        // Prepare
        mockUser(false, TEST_USER);
        Session session = mockSession(TEST_SESSION, TEST_APP_DEFINITION, TEST_USER);
        Mockito.when(k8sUtil.findSession(TEST_SESSION)).thenReturn(Optional.of(session));
        SessionPerformance sessionPerformance = Mockito.mock(SessionPerformance.class);
        Mockito.when(k8sUtil.reportPerformance(TEST_SESSION)).thenReturn(sessionPerformance);

        // Execute
        SessionPerformance result = fixture.performance(APP_ID, TEST_SESSION);

        // Assert
        assertSame(sessionPerformance, result);
    }

    @Test
    void performance_noPerformanceData_throwMetricsServerUnavailable() {
        // Prepare
        mockUser(false, TEST_USER);
        Session session = mockSession(TEST_SESSION, TEST_APP_DEFINITION, TEST_USER);
        Mockito.when(k8sUtil.findSession(TEST_SESSION)).thenReturn(Optional.of(session));
        Mockito.when(k8sUtil.reportPerformance(TEST_SESSION)).thenReturn(null);

        // Execute
        TheiaCloudWebException exception = assertThrows(TheiaCloudWebException.class, () -> {
            fixture.performance(APP_ID, TEST_SESSION);
        });

        // Assert
        Mockito.verify(k8sUtil).reportPerformance(TEST_SESSION);
        assertEquals(TheiaCloudError.METRICS_SERVER_UNAVAILABLE.getCode(), exception.getResponse().getStatus());
    }

    @Test
    void performance_noSessionFound_throwInvalidSessionName() {
        // Prepare
        mockUser(false, TEST_USER);
        Mockito.when(k8sUtil.findSession(TEST_SESSION)).thenReturn(Optional.empty());

        // Execute
        TheiaCloudWebException exception = assertThrows(TheiaCloudWebException.class, () -> {
            fixture.performance(APP_ID, TEST_SESSION);
        });

        // Assert
        Mockito.verify(k8sUtil, never()).reportPerformance(anyString());
        assertEquals(TheiaCloudError.INVALID_SESSION_NAME.getCode(), exception.getResponse().getStatus());
    }

    @Test
    void performance_otherUser_throwForbidden() {
        // Prepare
        mockUser(false, TEST_USER);
        Session session = mockSession(TEST_SESSION, TEST_APP_DEFINITION, OTHER_TEST_USER);
        Mockito.when(k8sUtil.findSession(TEST_SESSION)).thenReturn(Optional.of(session));

        // Execute
        TheiaCloudWebException exception = assertThrows(TheiaCloudWebException.class, () -> {
            fixture.performance(APP_ID, TEST_SESSION);
        });

        // Assert
        Mockito.verify(k8sUtil, never()).reportPerformance(anyString());
        assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getResponse().getStatus());
    }

    @Test
    void performance_noKeycloak_throwForbidden() {
        // Prepare
        mockUser(true, null);
        Mockito.when(applicationProperties.isUseKeycloak()).thenReturn(false);

        // Execute
        TheiaCloudWebException exception = assertThrows(TheiaCloudWebException.class, () -> {
            fixture.performance(APP_ID, TEST_SESSION);

        });

        // Assert
        Mockito.verify(k8sUtil, never()).listSessions(anyString());
        assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getResponse().getStatus());
    }

    @Test
    void performance_hasNoAnonymousAccessAnnotations() throws NoSuchMethodException, SecurityException {
        Method method = SessionResource.class.getMethod("performance", String.class, String.class);
        TestUtil.assertNoAnonymousAccessAnnotations(method);
    }

    @Test
    void start_noKeycloak_throwForbidden() {
        // Prepare
        mockUser(true, null);
        Mockito.when(applicationProperties.isUseKeycloak()).thenReturn(false);

        // Execute
        TheiaCloudWebException exception = assertThrows(TheiaCloudWebException.class, () -> {
            fixture.start(Mockito.mock(SessionStartRequest.class));

        });

        // Assert
        Mockito.verify(k8sUtil, never()).getWorkspace(anyString(), anyString());
        Mockito.verify(k8sUtil, never()).launchWorkspaceSession(anyString(), any(), anyInt(), any());
        assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getResponse().getStatus());
    }

    @Test
    void start_otherUser_throwForbidden() {
        // Prepare
        mockUser(false, TEST_USER);
        Session session = mockDefaultSession();
        Mockito.when(k8sUtil.findSession(TEST_SESSION)).thenReturn(Optional.of(session));

        // We leave the matching user in the request to verify that the stop is
        // denied even if the correct user is specified in the request.
        // After all, an attacker could do this.
        SessionStartRequest request = new SessionStartRequest(APP_ID, OTHER_TEST_USER, "abc");

        // Execute
        TheiaCloudWebException exception = assertThrows(TheiaCloudWebException.class, () -> {
            fixture.start(request);
        });

        // Assert
        Mockito.verify(k8sUtil, never()).getWorkspace(anyString(), anyString());
        Mockito.verify(k8sUtil, never()).launchWorkspaceSession(anyString(), any(), anyInt(), any());
        assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getResponse().getStatus());
    }

    @Test
    void start_hasNoAnonymousAccessAnnotations() throws NoSuchMethodException, SecurityException {
        Method method = SessionResource.class.getMethod("start", SessionStartRequest.class);
        TestUtil.assertNoAnonymousAccessAnnotations(method);
    }

    // ---
    // Utility methods
    // ---

    private void mockUser(boolean anonymous, String name) {
        Mockito.when(user.isAnonymous()).thenReturn(anonymous);
        Mockito.when(user.getIdentifier()).thenReturn(name);
    }

    private Session mockSession(String name, String appDef, String user) {
        SessionSpec spec = Mockito.mock(SessionSpec.class);
        Mockito.when(spec.getName()).thenReturn(name);
        Mockito.when(spec.getAppDefinition()).thenReturn(appDef);
        Mockito.when(spec.getUser()).thenReturn(user);
        return mockSession(spec);
    }

    private Session mockSession(SessionSpec spec) {
        Session session = Mockito.mock(Session.class);
        Mockito.when(session.getSpec()).thenReturn(spec);
        return session;
    }

    private Session mockDefaultSession() {
        SessionSpec spec = Mockito.mock(SessionSpec.class);
        Mockito.when(spec.getName()).thenReturn(TEST_SESSION);
        Mockito.when(spec.getUser()).thenReturn(TEST_USER);
        return mockSession(spec);
    }
}
