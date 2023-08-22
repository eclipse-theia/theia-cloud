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
package org.eclipse.theia.cloud.common.k8s.client;

import java.util.concurrent.TimeUnit;

import org.eclipse.theia.cloud.common.k8s.resource.workspace.WorkspaceV3beta;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.WorkspaceV3betaSpec;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.WorkspaceV3betaSpecResourceList;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.WorkspaceV3betaStatus;
import org.eclipse.theia.cloud.common.util.TheiaCloudError;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;

public class DefaultWorkspaceResourceClient extends BaseResourceClient<WorkspaceV3beta, WorkspaceV3betaSpecResourceList>
	implements WorkspaceResourceClient {

    public DefaultWorkspaceResourceClient(NamespacedKubernetesClient client) {
	super(client, WorkspaceV3beta.class, WorkspaceV3betaSpecResourceList.class);
    }

    @Override
    public WorkspaceV3beta create(String correlationId, WorkspaceV3betaSpec spec) {
	WorkspaceV3beta workspace = new WorkspaceV3beta();
	workspace.setSpec(spec);

	ObjectMeta metadata = new ObjectMeta();
	metadata.setName(spec.getName());
	workspace.setMetadata(metadata);

	info(correlationId, "Create Workspace " + workspace.getSpec());
	return operation().resource(workspace).create();
    }

    @Override
    public WorkspaceV3beta launch(String correlationId, WorkspaceV3betaSpec spec, long timeout, TimeUnit unit) {
	WorkspaceV3beta workspace = get(spec.getName()).orElseGet(() -> create(correlationId, spec));
	WorkspaceV3betaSpec workspaceSpec = workspace.getSpec();

	if (workspaceSpec.hasStorage()) {
	    return workspace;
	}

	if (workspaceSpec.hasError()) {
	    delete(correlationId, spec.getName());
	    return workspace;
	}

	try {
	    watchUntil(
		    (action, changedWorkspace) -> isWorkspaceComplete(correlationId, workspaceSpec, changedWorkspace),
		    timeout, unit);
	} catch (InterruptedException exception) {
	    error(correlationId, "Timeout while waiting for workspace storage " + workspaceSpec.getName()
		    + ". Deleting workspace again.", exception);
	    workspaceSpec.setError(TheiaCloudError.WORKSPACE_LAUNCH_TIMEOUT);
	}
	return workspace;
    }

    protected boolean isWorkspaceComplete(String correlationId, WorkspaceV3betaSpec createdWorkspace,
	    WorkspaceV3beta changedWorkspace) {
	if (createdWorkspace.getName().equals(changedWorkspace.getSpec().getName())) {
	    if (changedWorkspace.getSpec().hasStorage()) {
		info(correlationId, "Received URL for " + createdWorkspace);
		createdWorkspace.setStorage(changedWorkspace.getSpec().getStorage());
		return true;
	    }
	    if (changedWorkspace.getSpec().hasError()) {
		info(correlationId, "Received Error for " + changedWorkspace + ". Deleting workspace again.");
		delete(correlationId, createdWorkspace.getName());
		createdWorkspace.setError(changedWorkspace.getSpec().getError());
		return true;
	    }
	}
	return false;
    }

    @Override
    public WorkspaceV3betaStatus createDefaultStatus() {
	return new WorkspaceV3betaStatus();
    }
}
