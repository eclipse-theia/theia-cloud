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

import org.eclipse.theia.cloud.common.k8s.resource.ResourceStatus;
import org.eclipse.theia.cloud.common.k8s.resource.StatusStep;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.WorkspaceStatus;

public class WorkspaceHubStatus extends ResourceStatus {

    private final StatusStep volumeClaim;
    private final StatusStep volumeAttach;

    public WorkspaceHubStatus(WorkspaceStatus toHub) {
	if (toHub.getOperatorMessage() != null) {
	    this.setOperatorMessage(toHub.getOperatorMessage());
	}
	if (toHub.getOperatorStatus() != null) {
	    this.setOperatorStatus(toHub.getOperatorStatus());
	}
	if (toHub.getVolumeClaim() != null) {
	    this.volumeClaim = toHub.getVolumeClaim();
	} else {
	    this.volumeClaim = new StatusStep();
	}
	if (toHub.getVolumeAttach() != null) {
	    this.volumeAttach = toHub.getVolumeAttach();
	} else {
	    this.volumeAttach = new StatusStep();
	}
    }

    @SuppressWarnings("deprecation")
    public WorkspaceHubStatus(
	    org.eclipse.theia.cloud.common.k8s.resource.workspace.v1beta2.WorkspaceV1beta2Status toHub) {
	if (toHub.getOperatorMessage() != null) {
	    this.setOperatorMessage(toHub.getOperatorMessage());
	}
	if (toHub.getOperatorStatus() != null) {
	    this.setOperatorStatus(toHub.getOperatorStatus());
	}
	if (toHub.getVolumeClaim() != null) {
	    this.volumeClaim = toHub.getVolumeClaim();
	} else {
	    this.volumeClaim = new StatusStep();
	}
	if (toHub.getVolumeAttach() != null) {
	    this.volumeAttach = toHub.getVolumeAttach();
	} else {
	    this.volumeAttach = new StatusStep();
	}
    }

    public StatusStep getVolumeClaim() {
	return volumeClaim;
    }

    public StatusStep getVolumeAttach() {
	return volumeAttach;
    }

    @Override
    public String toString() {
	return "WorkspaceHubStatus [volumeClaim=" + volumeClaim + ", volumeAttach=" + volumeAttach + "]";
    }

}
