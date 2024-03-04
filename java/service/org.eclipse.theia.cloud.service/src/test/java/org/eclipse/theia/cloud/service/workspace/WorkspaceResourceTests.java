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
package org.eclipse.theia.cloud.service.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.eclipse.theia.cloud.common.k8s.resource.workspace.Workspace;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.WorkspaceSpec;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.WorkspaceStatus;
import org.eclipse.theia.cloud.common.util.TheiaCloudError;
import org.eclipse.theia.cloud.service.ApplicationProperties;
import org.eclipse.theia.cloud.service.K8sUtil;
import org.eclipse.theia.cloud.service.TheiaCloudUser;
import org.eclipse.theia.cloud.service.TheiaCloudWebException;
import org.eclipse.theia.cloud.service.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;

/**
 * Unit tests for
 * {@link org.eclipse.theia.cloud.service.workspace.WorkspaceResource}.
 * 
 * Disable authorization via {@link TestSecurity} annotation as this is a unit
 * test of the resource itself. Thus, we do not want authentication interceptors
 * to trigger when calling the resource's methods.
 */
@QuarkusTest
@TestSecurity(authorizationEnabled = false)
public class WorkspaceResourceTests {

    private static final String APP_ID = "asdfghjkl";
    private static final String APP_DEFINITION = "test-app-definition";
    private static final String TEST_USER = "TestUser";
    private static final String OTHER_TEST_USER = "OtherTestUser";
    private static final String TEST_WORKSPACE = "TestWorkspace";

    @InjectMock
    ApplicationProperties applicationProperties;

    @InjectMock
    K8sUtil k8sUtil;

    @InjectMock
    TheiaCloudUser user;

    @Inject
    WorkspaceResource fixture;

    @BeforeEach
    void mockApplicationProperties() {
        Mockito.when(applicationProperties.isUseKeycloak()).thenReturn(true);
    }

    /**
     * Test method for
     * {@link org.eclipse.theia.cloud.service.workspace.WorkspaceResource#delete(org.eclipse.theia.cloud.service.workspace.WorkspaceDeletionRequest)}.
     */
    @Test
    void delete_matchingUser_true() {
        // Prepare
        mockUser(false, TEST_USER);
        WorkspaceSpec workspace = mockDefaultWorkspace();

        Mockito.when(k8sUtil.findWorkspace(TEST_WORKSPACE)).thenReturn(Optional.of(workspace));
        Mockito.when(k8sUtil.deleteWorkspace(anyString(), eq(TEST_WORKSPACE))).thenReturn(true);

        WorkspaceDeletionRequest request = new WorkspaceDeletionRequest(APP_ID, TEST_USER, TEST_WORKSPACE);

        // Execute
        boolean result = fixture.delete(request);

        // Assert
        Mockito.verify(k8sUtil).deleteWorkspace(anyString(), eq(TEST_WORKSPACE));
        assertEquals(true, result);
    }

    /**
     * Test method for
     * {@link org.eclipse.theia.cloud.service.workspace.WorkspaceResource#delete(org.eclipse.theia.cloud.service.workspace.WorkspaceDeletionRequest)}.
     */
    @Test()
    void delete_otherUser_throwForbidden() {
        // Prepare
        mockUser(false, OTHER_TEST_USER);
        WorkspaceSpec workspace = mockDefaultWorkspace();
        Mockito.when(k8sUtil.findWorkspace(TEST_WORKSPACE)).thenReturn(Optional.of(workspace));

        // We leave the matching user in the request to verify that the deletion is
        // denied even if the correct user is specified in the request.
        // After all, an attacker could do this.
        WorkspaceDeletionRequest request = new WorkspaceDeletionRequest(APP_ID, TEST_USER, TEST_WORKSPACE);

        // Execute
        TheiaCloudWebException exception = assertThrows(TheiaCloudWebException.class, () -> {
            fixture.delete(request);
        });

        // Assert
        Mockito.verify(k8sUtil, never()).deleteWorkspace(anyString(), anyString());
        assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getResponse().getStatus());
    }

    /**
     * Test method for
     * {@link org.eclipse.theia.cloud.service.workspace.WorkspaceResource#delete(org.eclipse.theia.cloud.service.workspace.WorkspaceDeletionRequest)}.
     */
    @Test()
    void delete_otherUserWithNullName_throwForbidden() {
        // Prepare
        mockUser(false, null);
        WorkspaceSpec workspace = mockDefaultWorkspace();
        Mockito.when(k8sUtil.findWorkspace(TEST_WORKSPACE)).thenReturn(Optional.of(workspace));

        // We leave the matching user in the request to verify that the deletion is
        // denied even if the correct user is specified in the request.
        // After all, an attacker could now this.
        WorkspaceDeletionRequest request = new WorkspaceDeletionRequest(APP_ID, TEST_USER, TEST_WORKSPACE);

        // Execute
        TheiaCloudWebException exception = assertThrows(TheiaCloudWebException.class, () -> {
            fixture.delete(request);
        });

        // Assert
        Mockito.verify(k8sUtil, never()).deleteWorkspace(anyString(), anyString());
        assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getResponse().getStatus());
    }

    /**
     * Test method for
     * {@link org.eclipse.theia.cloud.service.workspace.WorkspaceResource#delete(org.eclipse.theia.cloud.service.workspace.WorkspaceDeletionRequest)}.
     */
    @Test()
    void delete_workspaceNotExisting_true() {
        // Prepare
        mockUser(false, TEST_USER);
        Mockito.when(k8sUtil.findWorkspace(TEST_WORKSPACE)).thenReturn(Optional.empty());
        WorkspaceDeletionRequest request = new WorkspaceDeletionRequest(APP_ID, TEST_USER, TEST_WORKSPACE);

        // Execute
        boolean result = fixture.delete(request);

        // Assert
        Mockito.verify(k8sUtil, never()).deleteWorkspace(anyString(), anyString());
        assertEquals(true, result);
    }

    /**
     * Test method for
     * {@link org.eclipse.theia.cloud.service.workspace.WorkspaceResource#delete(org.eclipse.theia.cloud.service.workspace.WorkspaceDeletionRequest)}.
     */
    @Test()
    void delete_noRequestWorkspaceName_throwMissingWorkspaceName() {
        // Prepare
        mockUser(false, TEST_USER);
        WorkspaceDeletionRequest request = new WorkspaceDeletionRequest(APP_ID, TEST_USER, null);

        // Execute
        TheiaCloudWebException exception = assertThrows(TheiaCloudWebException.class, () -> {
            fixture.delete(request);
        });

        // Assert
        Mockito.verify(k8sUtil, never()).deleteWorkspace(anyString(), anyString());
        assertEquals(TheiaCloudError.MISSING_WORKSPACE_NAME.getCode(), exception.getResponse().getStatus());
    }

    /**
     * Test method for
     * {@link org.eclipse.theia.cloud.service.workspace.WorkspaceResource#delete(org.eclipse.theia.cloud.service.workspace.WorkspaceDeletionRequest)}.
     */
    @Test()
    void delete_anonymousUser_throwForbidden() {
        // Prepare
        mockUser(true, null);
        WorkspaceSpec workspace = mockDefaultWorkspace();
        Mockito.when(k8sUtil.findWorkspace(TEST_WORKSPACE)).thenReturn(Optional.of(workspace));
        // We leave the matching user in the request to verify that the deletion is
        // denied even if the correct user is specified in the request.
        // After all, an attacker could now this.
        WorkspaceDeletionRequest request = new WorkspaceDeletionRequest(APP_ID, TEST_USER, TEST_WORKSPACE);

        // Execute
        TheiaCloudWebException exception = assertThrows(TheiaCloudWebException.class, () -> {
            fixture.delete(request);
        });

        // Assert
        Mockito.verify(k8sUtil, never()).deleteWorkspace(anyString(), anyString());
        assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getResponse().getStatus());
    }

    /**
     * Test method for
     * {@link org.eclipse.theia.cloud.service.workspace.WorkspaceResource#delete(org.eclipse.theia.cloud.service.workspace.WorkspaceDeletionRequest)}.
     */
    @Test()
    void delete_noKeycloak_throwForbidden() {
        // Prepare
        Mockito.when(applicationProperties.isUseKeycloak()).thenReturn(false);
        mockUser(true, null);
        WorkspaceSpec workspace = mockDefaultWorkspace();
        Mockito.when(k8sUtil.findWorkspace(TEST_WORKSPACE)).thenReturn(Optional.of(workspace));
        // We leave the matching user in the request to verify that the deletion is
        // denied even if the correct user is specified in the request.
        // After all, an attacker could know this.
        WorkspaceDeletionRequest request = new WorkspaceDeletionRequest(APP_ID, TEST_USER, TEST_WORKSPACE);

        // Execute
        TheiaCloudWebException exception = assertThrows(TheiaCloudWebException.class, () -> {
            fixture.delete(request);
        });

        // Assert
        Mockito.verify(k8sUtil, never()).deleteWorkspace(anyString(), anyString());
        assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getResponse().getStatus());
    }

    @Test
    void delete_hasNoAnonymousAccessAnnotations() throws NoSuchMethodException, SecurityException {
        Method method = WorkspaceResource.class.getMethod("delete", WorkspaceDeletionRequest.class);
        TestUtil.assertNoAnonymousAccessAnnotations(method);
    }

    @Test
    void create_hasNoAnonymousAccessAnnotations() throws NoSuchMethodException, SecurityException {
        Method method = WorkspaceResource.class.getMethod("create", WorkspaceCreationRequest.class);
        TestUtil.assertNoAnonymousAccessAnnotations(method);
    }

    @Test
    void create_matchingUser_UserWorkspace() {
        // Prepare
        mockUser(false, TEST_USER);
        WorkspaceCreationRequest request = new WorkspaceCreationRequest(APP_ID, APP_DEFINITION, TEST_USER,
                TEST_WORKSPACE);
        Workspace workspace = Mockito.mock(Workspace.class);
        WorkspaceSpec workspaceSpec = new WorkspaceSpec("abc", "def", APP_DEFINITION, TEST_USER);
        Mockito.when(workspace.getSpec()).thenReturn(workspaceSpec);
        Mockito.when(k8sUtil.createWorkspace(anyString(), argThat(new WorkspaceWithUser(TEST_USER))))
                .thenReturn(workspace);

        // Execute
        UserWorkspace result = fixture.create(request);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_USER, result.user);
    }

    @Test
    void create_erroneousWorkspace_throwTheiaCloudWebException() {
        // Prepare
        mockUser(false, TEST_USER);
        WorkspaceCreationRequest request = new WorkspaceCreationRequest(APP_ID, APP_DEFINITION, TEST_USER,
                TEST_WORKSPACE);
        Workspace workspace = Mockito.mock(Workspace.class);
        WorkspaceSpec workspaceSpec = new WorkspaceSpec("abc", "def", APP_DEFINITION, TEST_USER);
        WorkspaceStatus workspaceStatus = new WorkspaceStatus();
        workspaceStatus.setError("TestError");
        Mockito.when(workspace.getSpec()).thenReturn(workspaceSpec);
        Mockito.when(workspace.getStatus()).thenReturn(workspaceStatus);
        Mockito.when(k8sUtil.createWorkspace(anyString(), argThat(new WorkspaceWithUser(TEST_USER))))
                .thenReturn(workspace);

        // Execute
        TheiaCloudWebException exception = assertThrows(TheiaCloudWebException.class, () -> {
            fixture.create(request);
        });

        // Assert
        assertTrue(exception.getMessage().contains("TestError"));
    }

    @Test
    void create_otherUser_throwForbidden() {
        // Prepare
        mockUser(false, TEST_USER);
        WorkspaceCreationRequest request = new WorkspaceCreationRequest(APP_ID, APP_DEFINITION, OTHER_TEST_USER,
                TEST_WORKSPACE);

        // Execute
        TheiaCloudWebException exception = assertThrows(TheiaCloudWebException.class, () -> {
            fixture.create(request);

        });

        // Assert
        Mockito.verify(k8sUtil, never()).createWorkspace(anyString(), any());
        assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getResponse().getStatus());
    }

    @Test
    void create_noKeycloak_throwForbidden() {
        // Prepare
        mockUser(true, null);
        Mockito.when(applicationProperties.isUseKeycloak()).thenReturn(false);
        WorkspaceCreationRequest request = new WorkspaceCreationRequest(APP_ID, APP_DEFINITION, TEST_USER,
                TEST_WORKSPACE);

        // Execute
        TheiaCloudWebException exception = assertThrows(TheiaCloudWebException.class, () -> {
            fixture.create(request);

        });

        // Assert
        Mockito.verify(k8sUtil, never()).createWorkspace(anyString(), any());
        assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getResponse().getStatus());
    }

    @Test
    void list_matchingUser_workspaces() {
        // Prepare
        mockUser(false, TEST_USER);
        List<UserWorkspace> resultList = List.of();
        Mockito.when(k8sUtil.listWorkspaces(TEST_USER)).thenReturn(resultList);

        List<UserWorkspace> result = fixture.list(APP_ID, TEST_USER);

        assertSame(resultList, result);
    }

    @Test
    void list_otherUser_throwForbidden() {
        // Prepare
        mockUser(false, TEST_USER);
        List<UserWorkspace> resultList = List.of();
        Mockito.when(k8sUtil.listWorkspaces(TEST_USER)).thenReturn(resultList);

        // Execute
        TheiaCloudWebException exception = assertThrows(TheiaCloudWebException.class, () -> {
            fixture.list(APP_ID, OTHER_TEST_USER);

        });

        // Assert
        Mockito.verify(k8sUtil, never()).listWorkspaces(anyString());
        assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getResponse().getStatus());
    }

    @Test
    void list_noKeycloak_throwForbidden() {
        // Prepare
        mockUser(true, null);
        Mockito.when(applicationProperties.isUseKeycloak()).thenReturn(false);
        List<UserWorkspace> resultList = List.of();
        Mockito.when(k8sUtil.listWorkspaces(TEST_USER)).thenReturn(resultList);

        // Execute
        TheiaCloudWebException exception = assertThrows(TheiaCloudWebException.class, () -> {
            fixture.list(APP_ID, TEST_USER);

        });

        // Assert
        Mockito.verify(k8sUtil, never()).listWorkspaces(anyString());
        assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getResponse().getStatus());
    }

    @Test
    void list_hasNoAnonymousAccessAnnotations() throws NoSuchMethodException, SecurityException {
        Method method = WorkspaceResource.class.getMethod("list", String.class, String.class);
        TestUtil.assertNoAnonymousAccessAnnotations(method);
    }

    // ---
    // Utility methods
    // ---

    private void mockUser(boolean anonymous, String name) {
        Mockito.when(user.isAnonymous()).thenReturn(anonymous);
        Mockito.when(user.getIdentifier()).thenReturn(name);
    }

    private WorkspaceSpec mockDefaultWorkspace() {
        WorkspaceSpec workspace = Mockito.mock(WorkspaceSpec.class);
        Mockito.when(workspace.getName()).thenReturn(TEST_WORKSPACE);
        Mockito.when(workspace.getUser()).thenReturn(TEST_USER);
        return workspace;
    }

    class WorkspaceWithUser implements ArgumentMatcher<UserWorkspace> {

        private String user;

        WorkspaceWithUser(String user) {
            this.user = user;
        }

        @Override
        public boolean matches(UserWorkspace argument) {
            return user.equals(argument.user);
        }
    }
}
