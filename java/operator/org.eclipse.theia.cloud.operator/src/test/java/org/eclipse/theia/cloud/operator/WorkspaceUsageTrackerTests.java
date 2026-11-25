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
package org.eclipse.theia.cloud.operator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.client.WorkspaceResourceClient;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionStatus;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.Workspace;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.WorkspaceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.client.KubernetesClientException;

class WorkspaceUsageTrackerTests {
    private final Workspace workspace = new Workspace();
    private final WorkspaceStatus status = new WorkspaceStatus();
    private final WorkspaceResourceClient workspaces = mock(WorkspaceResourceClient.class);
    private final TheiaCloudClient client = mock(TheiaCloudClient.class);
    private final WorkspaceUsageTracker tracker = new WorkspaceUsageTracker(client);

    @BeforeEach
    void setUp() {
        workspace.getMetadata().setName("workspace");
        workspace.setStatus(status);
        when(client.workspaces()).thenReturn(workspaces);
        when(workspaces.get("workspace")).thenReturn(Optional.of(workspace));
        when(workspaces.updateStatus(anyString(), same(workspace), any())).thenAnswer(call -> {
            call.<Consumer<WorkspaceStatus>>getArgument(2).accept(status);
            return workspace;
        });
    }

    @Test
    void onlyFirstReadyAndLastDeletedSessionChangeUsage() {
        Session first = session("one", "workspace");
        tracker.sessionChanged(first, false, "add");
        assertNull(status.getLastUsed());
        first.getStatus().setUrl("https://one.example");
        tracker.sessionChanged(first, false, "ready");
        assertTrue(status.isInUse());
        Instant launch = Instant.parse(status.getLastUsed());
        Session second = session("two", "workspace");
        second.getStatus().setUrl("https://two.example");
        tracker.sessionChanged(first, false, "heartbeat");
        tracker.sessionChanged(second, false, "ready");
        tracker.sessionChanged(first, true, "stop");
        assertTrue(status.isInUse());
        assertEquals(launch, Instant.parse(status.getLastUsed()));
        tracker.sessionChanged(second, true, "timeout-deletion");
        assertFalse(status.isInUse());
        assertFalse(Instant.parse(status.getLastUsed()).isBefore(launch));
        tracker.sessionChanged(second, true, "replay");
        verify(workspaces, times(2)).updateStatus(anyString(), same(workspace), any());
    }

    @Test
    void ignoresFailedAndEphemeralSessions() {
        Session failed = session("failed", "workspace");
        failed.getStatus().setError("launch failed");
        tracker.sessionChanged(failed, false, "failure");
        tracker.sessionChanged(failed, true, "deletion");
        Session ephemeral = session("ephemeral", " ");
        ephemeral.getStatus().setUrl("https://ephemeral.example");
        tracker.sessionChanged(ephemeral, false, "ready");
        verifyNoInteractions(client);
    }

    @Test
    void restartPreservesTimestampAndRepairsMissedDeletion() {
        status.setInUse(true);
        status.setLastUsed("2025-01-01T00:00:00Z");
        Session existing = session("existing", "workspace");
        existing.getStatus().setUrl("https://existing.example");
        tracker.sessionChanged(existing, false, "init");
        tracker.reconcileAfterRestart(List.of(workspace), "init");
        assertEquals("2025-01-01T00:00:00Z", status.getLastUsed());
        new WorkspaceUsageTracker(client).reconcileAfterRestart(List.of(workspace), "init");
        assertFalse(status.isInUse());
        assertTrue(Instant.parse(status.getLastUsed()).isAfter(Instant.parse("2025-01-01T00:00:00Z")));
    }

    @Test
    void retryRecordsUsageEvenIfSessionStopsBeforeFirstWriteSucceeds() {
        when(workspaces.get("workspace")).thenThrow(new KubernetesClientException("unavailable"))
                .thenReturn(Optional.of(workspace));
        Session first = session("one", "workspace");
        first.getStatus().setUrl("https://one.example");
        tracker.sessionChanged(first, false, "ready");
        tracker.sessionChanged(first, true, "stop");
        tracker.retryPending("retry");
        assertFalse(status.isInUse());
        assertNotNull(status.getLastUsed());
        verify(workspaces, times(1)).updateStatus(anyString(), same(workspace), any());
    }

    private Session session(String uid, String workspaceName) {
        Session session = new Session();
        session.getMetadata().setUid(uid);
        session.setSpec(new SessionSpec(uid, "app", "user", workspaceName));
        session.setStatus(new SessionStatus());
        return session;
    }
}
