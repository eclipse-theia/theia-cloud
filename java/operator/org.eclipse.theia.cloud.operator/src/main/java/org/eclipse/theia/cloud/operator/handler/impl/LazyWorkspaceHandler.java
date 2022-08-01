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

import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.common.util.WorkspaceUtil;
import org.eclipse.theia.cloud.operator.handler.PersistentVolumeCreator;
import org.eclipse.theia.cloud.operator.handler.WorkspaceHandler;

import com.google.inject.Inject;

public class LazyWorkspaceHandler implements WorkspaceHandler {
    @Inject
    protected TheiaCloudClient resourceClient;

    @Inject
    protected PersistentVolumeCreator persistentVolumeHandler;

    @Override
    public boolean workspaceAdded(Workspace workspace, String correlationId) {
	String storageName = WorkspaceUtil.getStorageName(workspace.getSpec().getName());
	if (!resourceClient.persistentVolumes().has(storageName)) {
	    persistentVolumeHandler.createAndApplyPersistentVolume(correlationId, workspace);
	}

	if (!resourceClient.persistentVolumeClaims().has(storageName)) {
	    persistentVolumeHandler.createAndApplyPersistentVolumeClaim(correlationId, workspace);
	}

	editWorkspaceStorage(workspace, storageName, correlationId);
	return true;
    }

    private void editWorkspaceStorage(Workspace workspace, String storage, String correlationId) {
	resourceClient.workspaces().edit(correlationId, workspace.getSpec().getName(),
		toEdit -> toEdit.getSpec().setStorage(storage));
    }

    @Override
    public boolean workspaceDeleted(Workspace workspace, String correlationId) {
	String sessionName = WorkspaceUtil.getSessionName(workspace.getSpec().getName());
	resourceClient.sessions().delete(correlationId, sessionName);

	String storageName = WorkspaceUtil.getStorageName(workspace.getSpec().getName());
	resourceClient.persistentVolumeClaims().delete(correlationId, storageName);
	resourceClient.persistentVolumes().delete(correlationId, storageName);
	return true;
    }

}
