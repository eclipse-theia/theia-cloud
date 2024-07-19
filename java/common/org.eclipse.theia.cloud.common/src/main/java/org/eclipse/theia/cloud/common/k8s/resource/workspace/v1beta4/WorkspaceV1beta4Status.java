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
package org.eclipse.theia.cloud.common.k8s.resource.workspace.v1beta4;

import org.eclipse.theia.cloud.common.k8s.resource.ResourceStatus;
import org.eclipse.theia.cloud.common.k8s.resource.StatusStep;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.hub.WorkspaceHub;
import org.eclipse.theia.cloud.common.util.TheiaCloudError;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Deprecated
@JsonDeserialize
public class WorkspaceV1beta4Status extends ResourceStatus {

    @JsonProperty("volumeClaim")
    private StatusStep volumeClaim;

    @JsonProperty("volumeAttach")
    private StatusStep volumeAttach;

    @JsonProperty("error")
    private String error;

    /**
     * Default constructor.
     */
    public WorkspaceV1beta4Status() {
    }

    public WorkspaceV1beta4Status(WorkspaceHub fromHub) {
        if (fromHub.getOperatorMessage().isPresent()) {
            this.setOperatorMessage(fromHub.getOperatorMessage().get());
        }
        if (fromHub.getOperatorStatus().isPresent()) {
            this.setOperatorStatus(fromHub.getOperatorStatus().get());
        }
        if (fromHub.getVolumeAttachMessage().isPresent() && fromHub.getVolumeAttachStatus().isPresent()) {
            this.volumeAttach = new StatusStep();
            this.volumeAttach.setStatus(fromHub.getVolumeAttachStatus().get());
            this.volumeAttach.setMessage(fromHub.getVolumeAttachMessage().get());
        }
        if (fromHub.getVolumeClaimMessage().isPresent() && fromHub.getVolumeClaimStatus().isPresent()) {
            this.volumeClaim = new StatusStep();
            this.volumeClaim.setStatus(fromHub.getVolumeClaimStatus().get());
            this.volumeClaim.setMessage(fromHub.getVolumeClaimMessage().get());
        }
        this.error = fromHub.getError().orElse(null);
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

    public String getError() {
        return error;
    }

    public void setError(TheiaCloudError error) {
        setError(error.asString());
    }

    public void setError(String error) {
        this.error = error;
    }

    public boolean hasError() {
        return TheiaCloudError.isErrorString(getError());
    }

    @Override
    public String toString() {
        return "WorkspaceStatus [volumeClaim=" + volumeClaim + ", volumeAttach=" + volumeAttach + ", error=" + error
                + ", getOperatorStatus()=" + getOperatorStatus() + ", getOperatorMessage()=" + getOperatorMessage()
                + "]";
    }

}
