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
package org.eclipse.theia.cloud.common.k8s.resource;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize
public class SessionStatus extends ResourceStatus {

    @JsonProperty()
    private StatusStep serviceCreation;

    @JsonProperty()
    private StatusStep configMapCreation;

    @JsonProperty()
    private StatusStep deploymentCreation;

    @JsonProperty()
    private StatusStep ingressUpdate;

    public Optional<StatusStep> getServiceCreation() {
	return Optional.ofNullable(serviceCreation);
    }

    public void setServiceCreation(StatusStep serviceCreation) {
	this.serviceCreation = serviceCreation;
    }

    public Optional<StatusStep> getConfigMapCreation() {
	return Optional.ofNullable(configMapCreation);
    }

    public void setConfigMapCreation(StatusStep configMapCreation) {
	this.configMapCreation = configMapCreation;
    }

    public Optional<StatusStep> getDeploymentCreation() {
	return Optional.ofNullable(deploymentCreation);
    }

    public void setDeploymentCreation(StatusStep deploymentCreation) {
	this.deploymentCreation = deploymentCreation;
    }

    public Optional<StatusStep> getIngressUpdate() {
	return Optional.ofNullable(ingressUpdate);
    }

    public void setIngressUpdate(StatusStep ingressUpdate) {
	this.ingressUpdate = ingressUpdate;
    }

}
