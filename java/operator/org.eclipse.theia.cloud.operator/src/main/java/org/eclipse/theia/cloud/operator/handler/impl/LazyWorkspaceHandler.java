/********************************************************************************
 * Copyright (C) 2022 EclipseSource and others.
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
package org.eclipse.theia.cloud.operator.handler.impl;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.OperatorStatus;
import org.eclipse.theia.cloud.common.k8s.resource.ResourceStatus;
import org.eclipse.theia.cloud.common.k8s.resource.StatusStep;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.common.k8s.resource.WorkspaceStatus;
import org.eclipse.theia.cloud.common.util.WorkspaceUtil;
import org.eclipse.theia.cloud.operator.handler.PersistentVolumeCreator;
import org.eclipse.theia.cloud.operator.handler.WorkspaceHandler;

import com.google.inject.Inject;

public class LazyWorkspaceHandler implements WorkspaceHandler {
    private static final Logger LOGGER = LogManager.getLogger(LazyWorkspaceHandler.class);

    @Inject
    protected TheiaCloudClient client;

    @Inject
    protected PersistentVolumeCreator persistentVolumeHandler;

    @Override
    public boolean workspaceAdded(Workspace workspace, String correlationId) {
	LOGGER.info(formatLogMessage(correlationId, "Handling " + workspace));

	// Check current session status and ignore if handling failed before
	Optional<WorkspaceStatus> status = Optional.ofNullable(workspace.getStatus());
	String operatorStatus = status.map(ResourceStatus::getOperatorStatus).orElse(OperatorStatus.NEW);
	if (OperatorStatus.ERROR.equals(operatorStatus) || OperatorStatus.HANDLING.equals(operatorStatus)) {
	    LOGGER.warn(formatLogMessage(correlationId,
		    "Workspace could not be handled before and is skipped now. Current status: " + operatorStatus
			    + ". Workspace: " + workspace));
	    return false;
	}

	// Set workspace status to being handled
	client.workspaces().updateStatus(correlationId, workspace, s -> {
	    s.setOperatorStatus(OperatorStatus.HANDLING);
	});

	String storageName = WorkspaceUtil.getStorageName(workspace);
	client.workspaces().updateStatus(correlationId, workspace,
		s -> s.setVolumeClaim(new StatusStep("started", null)));

	if (!client.persistentVolumes().has(storageName)) {
	    LOGGER.trace(formatLogMessage(correlationId, "Creating new persistent volume named " + storageName));
	    persistentVolumeHandler.createAndApplyPersistentVolume(correlationId, workspace);
	}

	client.workspaces().updateStatus(correlationId, workspace, s -> {
	    s.setVolumeClaim(new StatusStep("finished", null));
	    s.setVolumeAttach(new StatusStep("started", null));
	});

	if (!client.persistentVolumeClaims().has(storageName)) {
	    LOGGER.trace(formatLogMessage(correlationId, "Creating new persistent volume claim named " + storageName));
	    persistentVolumeHandler.createAndApplyPersistentVolumeClaim(correlationId, workspace);
	}

	client.workspaces().updateStatus(correlationId, workspace, s -> {
	    s.setVolumeAttach(new StatusStep("claimed", null));
	});

	LOGGER.trace(formatLogMessage(correlationId, "Set workspace storage " + storageName));
	client.workspaces().edit(correlationId, workspace.getSpec().getName(),
		toEdit -> toEdit.getSpec().setStorage(storageName));

	client.workspaces().updateStatus(correlationId, workspace, s -> {
	    s.setVolumeAttach(new StatusStep("finished", null));
	});

	client.workspaces().updateStatus(correlationId, workspace, s -> {
	    s.setOperatorStatus(OperatorStatus.HANDLED);
	});
	return true;
    }

    @Override
    public boolean workspaceDeleted(Workspace workspace, String correlationId) {
	String sessionName = WorkspaceUtil.getSessionName(workspace.getSpec().getName());
	client.sessions().delete(correlationId, sessionName);

	String storageName = WorkspaceUtil.getStorageName(workspace);
	client.persistentVolumeClaims().delete(correlationId, storageName);
	client.persistentVolumes().delete(correlationId, storageName);
	return true;
    }
}
