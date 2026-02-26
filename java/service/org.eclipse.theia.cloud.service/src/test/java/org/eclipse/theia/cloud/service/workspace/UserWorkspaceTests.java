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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.theia.cloud.common.util.NamingUtil;
import org.eclipse.theia.cloud.common.util.WorkspaceUtil;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link UserWorkspace}.
 */
class UserWorkspaceTests {

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

        assertTrue(sessionName.length() <= NamingUtil.VALID_NAME_LIMIT,
                "Session name length " + sessionName.length() + " exceeds VALID_NAME_LIMIT "
                        + NamingUtil.VALID_NAME_LIMIT + ": " + sessionName);
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
}
