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
package org.eclipse.theia.cloud.common.k8s.resource.workspace;

import org.eclipse.theia.cloud.common.k8s.resource.ResourceStatus;
import org.eclipse.theia.cloud.common.k8s.resource.StatusStep;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize
public class WorkspaceV3betaStatus extends ResourceStatus {

    @JsonProperty("volumeClaim")
    private StatusStep volumeClaim;

    @JsonProperty("volumeAttach")
    private StatusStep volumeAttach;

    /**
     * Default constructor.
     */
    public WorkspaceV3betaStatus() {
    }

    /**
     * Migration constructor.
     * 
     * @param toMigrate
     */
    @SuppressWarnings("deprecation")
    public WorkspaceV3betaStatus(
	    org.eclipse.theia.cloud.common.k8s.resource.workspace.v2.WorkspaceV2betaStatus toMigrate) {
	setOperatorStatus(toMigrate.getOperatorStatus());
	setOperatorMessage(toMigrate.getOperatorMessage());

	this.volumeClaim = new StatusStep();
	if (toMigrate.getVolumeClaim() != null) {
	    this.volumeClaim.setStatus(toMigrate.getVolumeClaim().getStatus());
	    this.volumeClaim.setMessage(toMigrate.getVolumeClaim().getMessage());

	}

	this.volumeAttach = new StatusStep();
	if (toMigrate.getVolumeAttach() != null) {
	    this.volumeAttach.setStatus(toMigrate.getVolumeAttach().getStatus());
	    this.volumeAttach.setMessage(toMigrate.getVolumeAttach().getMessage());
	}
    }

    public StatusStep getVolumeClaim() {
	return volumeClaim;
    }

    public void setVolumeClaim(StatusStep volumeClaim) {
	this.volumeClaim = volumeClaim;
    }

    public StatusStep getVolumeAttach() {
	return volumeAttach;
    }

    public void setVolumeAttach(StatusStep volumeAttach) {
	this.volumeAttach = volumeAttach;
    }
}
