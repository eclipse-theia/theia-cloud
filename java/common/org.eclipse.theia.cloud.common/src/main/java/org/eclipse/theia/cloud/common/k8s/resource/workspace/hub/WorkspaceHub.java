/********************************************************************************
 * Copyright (C) 2023 EclipseSource and others.
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
package org.eclipse.theia.cloud.common.k8s.resource.workspace.hub;

import org.eclipse.theia.cloud.common.k8s.resource.workspace.Workspace;

import io.fabric8.kubernetes.api.model.ObjectMeta;

public class WorkspaceHub {

    private ObjectMeta metadata = new ObjectMeta();
    private WorkspaceHubSpec spec;
    private WorkspaceHubStatus status;

    public ObjectMeta getMetadata() {
	return metadata;
    }

    public void setMetadata(ObjectMeta metadata) {
	this.metadata = metadata;
    }

    public WorkspaceHubSpec getSpec() {
	return spec;
    }

    public void setSpec(WorkspaceHubSpec spec) {
	this.spec = spec;
    }

    public WorkspaceHubStatus getStatus() {
	return status;
    }

    public void setStatus(WorkspaceHubStatus status) {
	this.status = status;
    }

    public WorkspaceHub(Workspace toHub) {
	this.setMetadata(toHub.getMetadata());
	this.spec = new WorkspaceHubSpec(toHub.getSpec());
	if (toHub.getStatus() != null) {
	    this.status = new WorkspaceHubStatus(toHub.getStatus());
	}
    }

    @SuppressWarnings("deprecation")
    public WorkspaceHub(org.eclipse.theia.cloud.common.k8s.resource.workspace.v1beta2.WorkspaceV1beta2 toHub) {
	this.setMetadata(toHub.getMetadata());
	this.spec = new WorkspaceHubSpec(toHub.getSpec());
	if (toHub.getStatus() != null) {
	    this.status = new WorkspaceHubStatus(toHub.getStatus());
	}
    }
}
