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

import java.util.Optional;

import org.eclipse.theia.cloud.common.k8s.resource.workspace.Workspace;

import io.fabric8.kubernetes.api.model.ObjectMeta;

public class WorkspaceHub {

    final Optional<ObjectMeta> metadata;
    final Optional<String> name;
    final Optional<String> label;
    final Optional<String> appDefinition;
    final Optional<String> user;
    final Optional<String> storage;
    final Optional<String> error;

    final Optional<String> volumeClaimStatus;
    final Optional<String> volumeClaimMessage;
    final Optional<String> volumeAttachStatus;
    final Optional<String> volumeAttachMessage;
    final Optional<String> operatorStatus;
    final Optional<String> operatorMessage;

    public WorkspaceHub(Workspace toHub) {
        this.metadata = Optional.ofNullable(toHub.getMetadata());
        this.name = Optional.ofNullable(toHub.getSpec().getName());
        this.label = Optional.ofNullable(toHub.getSpec().getLabel());
        this.appDefinition = Optional.ofNullable(toHub.getSpec().getAppDefinition());
        this.user = Optional.ofNullable(toHub.getSpec().getUser());
        this.storage = Optional.ofNullable(toHub.getSpec().getStorage());
        if (toHub.getStatus() != null) {
            this.error = Optional.ofNullable(toHub.getNonNullStatus().getError());
            if (toHub.getNonNullStatus().getVolumeClaim() != null) {
                this.volumeClaimStatus = Optional.ofNullable(toHub.getNonNullStatus().getVolumeClaim().getStatus());
                this.volumeClaimMessage = Optional.ofNullable(toHub.getNonNullStatus().getVolumeClaim().getMessage());
            } else {
                this.volumeClaimStatus = Optional.empty();
                this.volumeClaimMessage = Optional.empty();
            }
            if (toHub.getNonNullStatus().getVolumeAttach() != null) {
                this.volumeAttachStatus = Optional.ofNullable(toHub.getNonNullStatus().getVolumeAttach().getStatus());
                this.volumeAttachMessage = Optional.ofNullable(toHub.getNonNullStatus().getVolumeAttach().getMessage());
            } else {
                this.volumeAttachStatus = Optional.empty();
                this.volumeAttachMessage = Optional.empty();
            }
            this.operatorStatus = Optional.ofNullable(toHub.getNonNullStatus().getOperatorStatus());
            this.operatorMessage = Optional.ofNullable(toHub.getNonNullStatus().getOperatorMessage());
        } else {
            this.error = Optional.empty();
            this.volumeClaimStatus = Optional.empty();
            this.volumeClaimMessage = Optional.empty();
            this.volumeAttachStatus = Optional.empty();
            this.volumeAttachMessage = Optional.empty();
            this.operatorStatus = Optional.empty();
            this.operatorMessage = Optional.empty();

        }
    }

    @SuppressWarnings("deprecation")
    public WorkspaceHub(org.eclipse.theia.cloud.common.k8s.resource.workspace.v1beta3.WorkspaceV1beta3 toHub) {
        this.metadata = Optional.ofNullable(toHub.getMetadata());
        this.name = Optional.ofNullable(toHub.getSpec().getName());
        this.label = Optional.ofNullable(toHub.getSpec().getLabel());
        this.appDefinition = Optional.ofNullable(toHub.getSpec().getAppDefinition());
        this.user = Optional.ofNullable(toHub.getSpec().getUser());
        this.storage = Optional.ofNullable(toHub.getSpec().getStorage());
        this.error = Optional.ofNullable(toHub.getSpec().getError());
        if (toHub.getStatus() != null) {
            if (toHub.getNonNullStatus().getVolumeClaim() != null) {
                this.volumeClaimStatus = Optional.ofNullable(toHub.getNonNullStatus().getVolumeClaim().getStatus());
                this.volumeClaimMessage = Optional.ofNullable(toHub.getNonNullStatus().getVolumeClaim().getMessage());
            } else {
                this.volumeClaimStatus = Optional.empty();
                this.volumeClaimMessage = Optional.empty();
            }
            if (toHub.getNonNullStatus().getVolumeAttach() != null) {
                this.volumeAttachStatus = Optional.ofNullable(toHub.getNonNullStatus().getVolumeAttach().getStatus());
                this.volumeAttachMessage = Optional.ofNullable(toHub.getNonNullStatus().getVolumeAttach().getMessage());
            } else {
                this.volumeAttachStatus = Optional.empty();
                this.volumeAttachMessage = Optional.empty();
            }
            this.operatorStatus = Optional.ofNullable(toHub.getNonNullStatus().getOperatorStatus());
            this.operatorMessage = Optional.ofNullable(toHub.getNonNullStatus().getOperatorMessage());
        } else {
            this.volumeClaimStatus = Optional.empty();
            this.volumeClaimMessage = Optional.empty();
            this.volumeAttachStatus = Optional.empty();
            this.volumeAttachMessage = Optional.empty();
            this.operatorStatus = Optional.empty();
            this.operatorMessage = Optional.empty();

        }
    }

    public Optional<ObjectMeta> getMetadata() {
        return metadata;
    }

    public Optional<String> getName() {
        return name;
    }

    public Optional<String> getLabel() {
        return label;
    }

    public Optional<String> getAppDefinition() {
        return appDefinition;
    }

    public Optional<String> getUser() {
        return user;
    }

    public Optional<String> getStorage() {
        return storage;
    }

    public Optional<String> getError() {
        return error;
    }

    public Optional<String> getVolumeClaimStatus() {
        return volumeClaimStatus;
    }

    public Optional<String> getVolumeClaimMessage() {
        return volumeClaimMessage;
    }

    public Optional<String> getVolumeAttachStatus() {
        return volumeAttachStatus;
    }

    public Optional<String> getVolumeAttachMessage() {
        return volumeAttachMessage;
    }

    public Optional<String> getOperatorStatus() {
        return operatorStatus;
    }

    public Optional<String> getOperatorMessage() {
        return operatorMessage;
    }

}
