/********************************************************************************
 * Copyright (C) 2026 Gaurav Chaudhary and others.
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
package org.eclipse.theia.cloud.common.k8s.resource.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.theia.cloud.common.k8s.resource.workspace.hub.WorkspaceHub;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.v1beta3.WorkspaceV1beta3;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.v1beta4.WorkspaceV1beta4;
import org.junit.jupiter.api.Test;

@SuppressWarnings("deprecation")
class WorkspaceUsageConversionTests {

    @Test
    void usageSurvivesRoundTripThroughAllServedVersions() {
        Workspace original = new Workspace();
        original.setSpec(new WorkspaceSpec("test", "Test", "app", "user"));
        WorkspaceStatus status = new WorkspaceStatus();
        status.setInUse(true);
        status.setLastUsed("2026-01-01T00:00:00Z");
        original.setStatus(status);
        WorkspaceV1beta4 v4 = new WorkspaceV1beta4(new WorkspaceHub(original));
        WorkspaceV1beta3 v3 = new WorkspaceV1beta3(new WorkspaceHub(v4));
        Workspace converted = new Workspace(new WorkspaceHub(v3));
        assertTrue(converted.getStatus().isInUse());
        assertEquals(status.getLastUsed(), converted.getStatus().getLastUsed());
    }

    @Test
    void legacyWorkspaceWithoutStatusHasNoInventedTimestamp() {
        Workspace original = new Workspace();
        original.setSpec(new WorkspaceSpec("test", "Test", "app", "user"));
        Workspace converted = new Workspace(new WorkspaceHub(original));
        assertFalse(converted.getStatus().isInUse());
        assertNull(converted.getStatus().getLastUsed());
    }
}
