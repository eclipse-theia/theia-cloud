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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
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
	LOGGER.info(formatLogMessage(correlationId, "Handling " + workspace.getSpec()));

	String storageName = WorkspaceUtil.getStorageName(workspace);
	if (!client.persistentVolumes().has(storageName)) {
	    LOGGER.trace(formatLogMessage(correlationId, "Creating new persistent volume named " + storageName));
	    persistentVolumeHandler.createAndApplyPersistentVolume(correlationId, workspace);
	}

	if (!client.persistentVolumeClaims().has(storageName)) {
	    LOGGER.trace(formatLogMessage(correlationId, "Creating new persistent volume claim named " + storageName));
	    persistentVolumeHandler.createAndApplyPersistentVolumeClaim(correlationId, workspace);
	}

	LOGGER.trace(formatLogMessage(correlationId, "Set workspace storage " + storageName));
	client.workspaces().edit(correlationId, workspace.getSpec().getId(),
		toEdit -> toEdit.getSpec().setStorage(storageName));

	return true;
    }

    @Override
    public boolean workspaceDeleted(Workspace workspace, String correlationId) {
	String sessionName = WorkspaceUtil.getSessionName(workspace.getSpec().getId());
	client.sessions().delete(correlationId, sessionName);

	String storageName = WorkspaceUtil.getStorageName(workspace);
	client.persistentVolumeClaims().delete(correlationId, storageName);
	client.persistentVolumes().delete(correlationId, storageName);
	return true;
    }
}
