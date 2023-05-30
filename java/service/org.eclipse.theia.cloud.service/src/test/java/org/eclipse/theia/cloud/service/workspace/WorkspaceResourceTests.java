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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.core.Response.Status;

import org.eclipse.theia.cloud.common.k8s.resource.WorkspaceSpec;
import org.eclipse.theia.cloud.common.util.TheiaCloudError;
import org.eclipse.theia.cloud.service.ApplicationProperties;
import org.eclipse.theia.cloud.service.K8sUtil;
import org.eclipse.theia.cloud.service.TheiaCloudUser;
import org.eclipse.theia.cloud.service.TheiaCloudWebException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;

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
    private static final String TEST_USER = "TestUser";
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
	mockUser(false, "OtherTestUser");
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
}
