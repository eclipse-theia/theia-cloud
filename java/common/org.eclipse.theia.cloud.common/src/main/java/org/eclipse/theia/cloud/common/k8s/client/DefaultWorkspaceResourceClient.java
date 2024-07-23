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

import org.eclipse.theia.cloud.common.k8s.resource.workspace.Workspace;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.WorkspaceSpec;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.WorkspaceSpecResourceList;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.WorkspaceStatus;
import org.eclipse.theia.cloud.common.util.TheiaCloudError;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;

public class DefaultWorkspaceResourceClient extends BaseResourceClient<Workspace, WorkspaceSpecResourceList>
        implements WorkspaceResourceClient {

    public DefaultWorkspaceResourceClient(NamespacedKubernetesClient client) {
        super(client, Workspace.class, WorkspaceSpecResourceList.class);
    }

    @Override
    public Workspace create(String correlationId, WorkspaceSpec spec) {
        Workspace workspace = new Workspace();
        workspace.setSpec(spec);

        ObjectMeta metadata = new ObjectMeta();
        metadata.setName(spec.getName());
        workspace.setMetadata(metadata);

        info(correlationId, "Create Workspace " + workspace.getSpec());
        return operation().resource(workspace).create();
    }

    @Override
    public Workspace launch(String correlationId, WorkspaceSpec spec, long timeout, TimeUnit unit) {
        Workspace workspace = get(spec.getName()).orElseGet(() -> create(correlationId, spec));
        WorkspaceSpec workspaceSpec = workspace.getSpec();
        WorkspaceStatus workspaceStatus = workspace.getNonNullStatus();

        if (workspaceSpec.hasStorage()) {
            return workspace;
        }

        if (workspaceStatus.hasError()) {
            delete(correlationId, spec.getName());
            return workspace;
        }

        try {
            watchUntil((action, changedWorkspace) -> isWorkspaceComplete(correlationId, workspace, changedWorkspace),
                    timeout, unit);
        } catch (InterruptedException exception) {
            error(correlationId, "Timeout while waiting for workspace storage " + workspaceSpec.getName()
                    + ". Deleting workspace again.", exception);
            updateStatus(correlationId, workspace, status -> status.setError(TheiaCloudError.WORKSPACE_LAUNCH_TIMEOUT));
        }
        return workspace;
    }

    protected boolean isWorkspaceComplete(String correlationId, Workspace createdWorkspace,
            Workspace changedWorkspace) {
        if (createdWorkspace.getSpec().getName().equals(changedWorkspace.getSpec().getName())) {
            if (changedWorkspace.getSpec().hasStorage()) {
                info(correlationId, "Received URL for " + createdWorkspace);
                createdWorkspace.getSpec().setStorage(changedWorkspace.getSpec().getStorage());
                return true;
            }
            if (changedWorkspace.getNonNullStatus().hasError()) {
                info(correlationId, "Received Error for " + changedWorkspace + ". Deleting workspace again.");
                delete(correlationId, createdWorkspace.getSpec().getName());
                updateStatus(correlationId, createdWorkspace,
                        status -> status.setError(changedWorkspace.getNonNullStatus().getError()));

                return true;
            }
        }
        return false;
    }

    @Override
    public WorkspaceStatus createDefaultStatus() {
        return new WorkspaceStatus();
    }
}
