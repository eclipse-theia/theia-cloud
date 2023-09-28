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
package org.eclipse.theia.cloud.common.k8s.resource.appdefinition.hub;

import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.v1beta7.AppDefinitionV1beta7;

import io.fabric8.kubernetes.api.model.ObjectMeta;

@SuppressWarnings("deprecation")
public class AppDefinitionHub {

    private ObjectMeta metadata = new ObjectMeta();
    private AppDefinitionHubSpec spec;
    private AppDefinitionHubStatus status;

    public ObjectMeta getMetadata() {
	return metadata;
    }

    public void setMetadata(ObjectMeta metadata) {
	this.metadata = metadata;
    }

    public AppDefinitionHubSpec getSpec() {
	return spec;
    }

    public void setSpec(AppDefinitionHubSpec spec) {
	this.spec = spec;
    }

    public AppDefinitionHubStatus getStatus() {
	return status;
    }

    public void setStatus(AppDefinitionHubStatus status) {
	this.status = status;
    }

    public AppDefinitionHub(AppDefinition toHub) {
	this.setMetadata(toHub.getMetadata());
	this.spec = new AppDefinitionHubSpec(toHub.getSpec());
	if (toHub.getStatus() != null) {
	    this.status = new AppDefinitionHubStatus(toHub.getStatus());
	}
    }

    public AppDefinitionHub(AppDefinitionV1beta7 toHub) {
	this.setMetadata(toHub.getMetadata());
	this.spec = new AppDefinitionHubSpec(toHub.getSpec());
	if (toHub.getStatus() != null) {
	    this.status = new AppDefinitionHubStatus(toHub.getStatus());
	}
    }
}
