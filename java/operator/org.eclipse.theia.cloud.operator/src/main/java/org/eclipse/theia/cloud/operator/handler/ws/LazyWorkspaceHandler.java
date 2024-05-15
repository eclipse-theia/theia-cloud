/********************************************************************************
 * Copyright (C) 2022-2023 EclipseSource and others.
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
package org.eclipse.theia.cloud.operator.handler.ws;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.OperatorStatus;
import org.eclipse.theia.cloud.common.k8s.resource.ResourceStatus;
import org.eclipse.theia.cloud.common.k8s.resource.StatusStep;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.Workspace;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.WorkspaceStatus;
import org.eclipse.theia.cloud.common.util.WorkspaceUtil;
import org.eclipse.theia.cloud.operator.pv.PersistentVolumeCreator;

import com.google.inject.Inject;

public class LazyWorkspaceHandler implements WorkspaceHandler {
    private static final Logger LOGGER = LogManager.getLogger(LazyWorkspaceHandler.class);

    @Inject
    protected TheiaCloudClient client;

    @Inject
    protected PersistentVolumeCreator persistentVolumeHandler;

    @Override
    public boolean workspaceAdded(Workspace workspace, String correlationId) {
	try {
	    return doWorkspaceAdded(workspace, correlationId);
	} catch (Throwable ex) {
	    LOGGER.error(formatLogMessage(correlationId,
		    "An unexpected exception occurred while adding Workspace: " + workspace), ex);
	    client.workspaces().updateStatus(correlationId, workspace, status -> {
		status.setOperatorStatus(OperatorStatus.ERROR);
		status.setOperatorMessage(
			"Unexpected error. Please check the logs for correlationId: " + correlationId);
	    });
	    return false;
	}
    }

    protected boolean doWorkspaceAdded(Workspace workspace, String correlationId) {
	LOGGER.info(formatLogMessage(correlationId, "Handling " + workspace));

	// Check current session status and ignore if handling failed or finished before
	Optional<WorkspaceStatus> status = Optional.ofNullable(workspace.getStatus());
	String operatorStatus = status.map(ResourceStatus::getOperatorStatus).orElse(OperatorStatus.NEW);
	if (OperatorStatus.HANDLED.equals(operatorStatus)) {
	    LOGGER.trace(formatLogMessage(correlationId,
		    "Workspace was successfully handled before and is skipped now. Workspace: " + workspace));
	    return true;
	}
	if (OperatorStatus.HANDLING.equals(operatorStatus)) {
	    // TODO We should not return but continue where we left off.
	    LOGGER.warn(formatLogMessage(correlationId,
		    "Workspace handling was unexpectedly interrupted before. Workspace is skipped now and its status is set to ERROR. Workspace: "
			    + workspace));
	    client.workspaces().updateStatus(correlationId, workspace, s -> {
		s.setOperatorStatus(OperatorStatus.ERROR);
		s.setOperatorMessage("Handling was unexpectedly interrupted before. CorrelationId: " + correlationId);
	    });
	    return false;
	}
	if (OperatorStatus.ERROR.equals(operatorStatus)) {
	    LOGGER.warn(formatLogMessage(correlationId,
		    "Workspace could not be handled before and is skipped now. Workspace: " + workspace));
	    return false;
	}

	// Set workspace status to being handled
	client.workspaces().updateStatus(correlationId, workspace, s -> {
	    s.setOperatorStatus(OperatorStatus.HANDLING);
	});

	String storageName = WorkspaceUtil.getStorageName(workspace);
	client.workspaces().updateStatus(correlationId, workspace, s -> s.setVolumeClaim(new StatusStep("started")));

	if (!client.persistentVolumesClient().has(storageName)) {
	    LOGGER.trace(formatLogMessage(correlationId, "Creating new persistent volume named " + storageName));
	    persistentVolumeHandler.createAndApplyPersistentVolume(correlationId, workspace);
	}

	client.workspaces().updateStatus(correlationId, workspace, s -> {
	    s.setVolumeClaim(new StatusStep("finished"));
	    s.setVolumeAttach(new StatusStep("started"));
	});

	if (!client.persistentVolumeClaimsClient().has(storageName)) {
	    LOGGER.trace(formatLogMessage(correlationId, "Creating new persistent volume claim named " + storageName));
	    persistentVolumeHandler.createAndApplyPersistentVolumeClaim(correlationId, workspace);
	}

	client.workspaces().updateStatus(correlationId, workspace, s -> {
	    s.setVolumeAttach(new StatusStep("claimed"));
	});

	LOGGER.trace(formatLogMessage(correlationId, "Set workspace storage " + storageName));
	client.workspaces().edit(correlationId, workspace.getSpec().getName(),
		toEdit -> toEdit.getSpec().setStorage(storageName));

	client.workspaces().updateStatus(correlationId, workspace, s -> {
	    s.setVolumeAttach(new StatusStep("finished"));
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
	client.persistentVolumeClaimsClient().delete(correlationId, storageName);
	client.persistentVolumesClient().delete(correlationId, storageName);
	return true;
    }
}
