/********************************************************************************
 * Copyright (C) 2026 EclipseSource and others.
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.theia.cloud.common.k8s.client.SessionResourceClient;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.client.WorkspaceResourceClient;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.Workspace;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.WorkspaceSpec;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.WorkspaceStatus;
import org.eclipse.theia.cloud.common.util.NamingUtil;
import org.eclipse.theia.cloud.common.util.WorkspaceUtil;
import org.eclipse.theia.cloud.service.K8sUtil;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link UserWorkspace}.
 */
class UserWorkspaceTests {

    @Test
    void listIncludesPendingSessionWithIndependentResourceName() {
        Workspace workspace = new Workspace();
        workspace.setSpec(new WorkspaceSpec("ws", "Label", "app", "user"));
        K8sUtil service = mock(K8sUtil.class, CALLS_REAL_METHODS);
        service.CLIENT = mock(TheiaCloudClient.class);
        WorkspaceResourceClient workspaces = mock(WorkspaceResourceClient.class);
        SessionResourceClient sessions = mock(SessionResourceClient.class);
        when(service.CLIENT.workspaces()).thenReturn(workspaces);
        when(service.CLIENT.sessions()).thenReturn(sessions);
        when(workspaces.list("user")).thenReturn(List.of(workspace));
        when(sessions.specs("user")).thenReturn(List.of(new SessionSpec("another-name", "app", "user", "ws")));

        UserWorkspace result = service.listWorkspaces("user").get(0);
        assertTrue(result.active);
        assertFalse(result.inUse);
    }

    @Test
    void fromWorkspaceIncludesObservedUsage() {
        Workspace workspace = new Workspace();
        workspace.setSpec(new WorkspaceSpec("ws", "Label", "app", "user"));
        WorkspaceStatus status = new WorkspaceStatus();
        status.setInUse(true);
        status.setLastUsed("2026-01-01T10:00:00Z");
        workspace.setStatus(status);

        UserWorkspace result = UserWorkspace.fromWorkspace(workspace);
        assertTrue(result.inUse);
        assertEquals(status.getLastUsed(), result.lastUsed);
    }

    @Test
    void constructor_longExternalName_cappedAtWorkspaceNameLimit() {
        // Simulate a long externally-provided workspace name
        String longName = "ws-asdfghjkl-theia-cloud-monitor-theia-popup-foo@theia-cloud.io";
        UserWorkspace workspace = new UserWorkspace("theia-cloud-monitor-theia-popup", "foo@theia-cloud.io", longName,
                "Test Label");

        assertTrue(workspace.name.length() <= WorkspaceUtil.WORKSPACE_NAME_LIMIT,
                "Workspace name length " + workspace.name.length() + " exceeds WORKSPACE_NAME_LIMIT "
                        + WorkspaceUtil.WORKSPACE_NAME_LIMIT + ": " + workspace.name);
    }

    @Test
    void constructor_longExternalName_sessionNameWithinValidNameLimit() {
        String longName = "ws-asdfghjkl-theia-cloud-monitor-theia-popup-foo@theia-cloud.io";
        UserWorkspace workspace = new UserWorkspace("theia-cloud-monitor-theia-popup", "foo@theia-cloud.io", longName,
                "Test Label");

        String sessionName = WorkspaceUtil.getSessionName(workspace.name);

        assertTrue(sessionName.length() <= NamingUtil.VALID_NAME_LIMIT, "Session name length " + sessionName.length()
                + " exceeds VALID_NAME_LIMIT " + NamingUtil.VALID_NAME_LIMIT + ": " + sessionName);
    }

    @Test
    void constructor_shortName_unchanged() {
        String shortName = "ws-short";
        UserWorkspace workspace = new UserWorkspace("app-def", "user@example.com", shortName, "Test Label");

        assertTrue(workspace.name.equals("ws-short"));
    }

    @Test
    void constructor_nameAtWorkspaceNameLimit_unchanged() {
        // Create a valid name exactly at the WORKSPACE_NAME_LIMIT
        String name = "a".repeat(WorkspaceUtil.WORKSPACE_NAME_LIMIT);
        UserWorkspace workspace = new UserWorkspace("app-def", "user@example.com", name, "Test Label");

        assertTrue(workspace.name.length() <= WorkspaceUtil.WORKSPACE_NAME_LIMIT);
    }

    @Test
    void constructor_nullName_generatesName() {
        UserWorkspace workspace = new UserWorkspace("app-def", "user@example.com", null, "Test Label");

        assertTrue(workspace.name != null && !workspace.name.isEmpty());
        assertTrue(workspace.name.length() <= NamingUtil.VALID_NAME_LIMIT);
    }

    @Test
    void constructorFromSpec_preservesLongExistingWorkspaceName() {
        // Simulate an existing workspace in k8s with a 62-char name (created before WORKSPACE_NAME_LIMIT was
        // introduced)
        String existingName = "ws-" + "a".repeat(59); // 62 chars total
        assertEquals(62, existingName.length());
        WorkspaceSpec spec = new WorkspaceSpec(existingName, "Old Label", "app-def", "user@example.com");

        UserWorkspace workspace = new UserWorkspace(spec);

        assertEquals(existingName, workspace.name,
                "UserWorkspace from WorkspaceSpec must preserve the existing name without re-truncation");
        assertEquals("Old Label", workspace.label);
        assertEquals("app-def", workspace.appDefinition);
        assertEquals("user@example.com", workspace.user);
    }

    @Test
    void constructorFromSpec_preservesNameLongerThanWorkspaceNameLimit() {
        // A name between WORKSPACE_NAME_LIMIT and VALID_NAME_LIMIT should not be re-truncated
        int nameLength = WorkspaceUtil.WORKSPACE_NAME_LIMIT + 5;
        String existingName = "a" + "b".repeat(nameLength - 1);
        assertTrue(existingName.length() > WorkspaceUtil.WORKSPACE_NAME_LIMIT);
        assertTrue(existingName.length() <= NamingUtil.VALID_NAME_LIMIT);
        WorkspaceSpec spec = new WorkspaceSpec(existingName, "Label", "app-def", "user@example.com");

        UserWorkspace workspace = new UserWorkspace(spec);

        assertEquals(existingName, workspace.name,
                "UserWorkspace from WorkspaceSpec must not re-truncate names that exceed WORKSPACE_NAME_LIMIT");
    }

    @Test
    void constructorFromInput_capsNewNameAtWorkspaceNameLimit() {
        // New workspace from user input should be capped at WORKSPACE_NAME_LIMIT
        String longName = "ws-" + "a".repeat(60); // 63 chars, exceeds WORKSPACE_NAME_LIMIT
        UserWorkspace workspace = new UserWorkspace("app-def", "user@example.com", longName, "Label");

        assertTrue(workspace.name.length() <= WorkspaceUtil.WORKSPACE_NAME_LIMIT,
                "New workspace name from user input should be capped at WORKSPACE_NAME_LIMIT but was "
                        + workspace.name.length());
    }
}
