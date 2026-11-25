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

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.Workspace;

import com.google.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClientException;

/** Tracks ready sessions by UID, including sessions whose names differ from their workspace. */
final class WorkspaceUsageTracker {
    private static final Logger LOGGER = LogManager.getLogger(WorkspaceUsageTracker.class);

    private final TheiaCloudClient client;
    private final Map<String, String> workspaceBySession = new HashMap<>();
    private final Map<String, Instant> pending = new HashMap<>();

    @Inject
    WorkspaceUsageTracker(TheiaCloudClient client) {
        this.client = client;
    }

    synchronized void sessionChanged(Session session, boolean deleted, String correlationId) {
        String uid = session.getMetadata().getUid();
        String previous = workspaceBySession.get(uid);
        String current = !deleted && !session.getSpec().isEphemeral() && session.getNonNullStatus().hasUrl()
                ? session.getSpec().getWorkspace() : null;
        if (Objects.equals(current, previous)) {
            return;
        }

        if (previous != null) {
            workspaceBySession.remove(uid);
            if (!workspaceBySession.containsValue(previous)) {
                reconcile(previous, Instant.now(), correlationId);
            }
        }
        if (current != null) {
            boolean alreadyInUse = workspaceBySession.containsValue(current);
            workspaceBySession.put(uid, current);
            if (!alreadyInUse) {
                reconcile(current, Instant.now(), correlationId);
            }
        }
    }

    synchronized void reconcileAfterRestart(Collection<Workspace> workspaces, String correlationId) {
        // A session may have stopped while the operator was offline.
        for (Workspace workspace : workspaces) {
            String name = workspace.getMetadata().getName();
            if (workspace.getNonNullStatus().isInUse() && !workspaceBySession.containsValue(name)) {
                reconcile(name, Instant.now(), correlationId);
            }
        }
    }

    synchronized void retryPending(String correlationId) {
        Map.copyOf(pending).forEach((name, time) -> reconcile(name, time, correlationId));
    }

    private void reconcile(String name, Instant observedAt, String correlationId) {
        boolean inUse = workspaceBySession.containsValue(name);
        try {
            var workspaces = client.workspaces();
            var workspace = workspaces.get(name);
            if (workspace.isPresent() && (workspace.get().getNonNullStatus().isInUse() != inUse
                    || pending.containsKey(name))) {
                workspaces.updateStatus(correlationId, workspace.get(), status -> {
                    status.setInUse(inUse);
                    status.setLastUsed(observedAt.toString());
                });
            }
            pending.remove(name);
        } catch (KubernetesClientException e) {
            pending.put(name, observedAt);
            LOGGER.warn(formatLogMessage(correlationId, "Will retry usage update for workspace " + name), e);
        }
    }
}
