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
package org.eclipse.theia.cloud.common.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WorkspaceUtil}.
 */
class WorkspaceUtilTests {

    @Test
    void getSessionName_fromLongWorkspaceName_withinValidNameLimit() {
        // Simulate a long workspace name that has been truncated to WORKSPACE_NAME_LIMIT
        String longInput = "ws-asdfghjkl-theia-cloud-monitor-theia-popup-foo-theia-cloud-io";
        String workspaceName = NamingUtil.asValidName(longInput, WorkspaceUtil.WORKSPACE_NAME_LIMIT);

        // Session name adds "-session" suffix
        String sessionName = WorkspaceUtil.getSessionName(workspaceName);

        assertTrue(sessionName.length() <= NamingUtil.VALID_NAME_LIMIT,
                "Session name length " + sessionName.length() + " exceeds VALID_NAME_LIMIT "
                        + NamingUtil.VALID_NAME_LIMIT + ": " + sessionName);
    }

    @Test
    void workspaceNameLimit_isCorrectValue() {
        // WORKSPACE_NAME_LIMIT should be VALID_NAME_LIMIT minus the "-session" suffix length (8)
        assertTrue(WorkspaceUtil.WORKSPACE_NAME_LIMIT == NamingUtil.VALID_NAME_LIMIT - 8);
    }

    @Test
    void getSessionName_appendsSessionSuffix() {
        String sessionName = WorkspaceUtil.getSessionName("my-workspace");
        assertTrue(sessionName.endsWith("-session"));
    }
}
