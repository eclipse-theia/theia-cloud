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

import org.eclipse.theia.cloud.common.k8s.resource.workspace.WorkspaceSpec;

public class WorkspaceHubSpec {

    private final String name;
    private final String label;
    private final String appDefinition;
    private final String user;
    private final String storage;
    private final String error;

    public WorkspaceHubSpec(WorkspaceSpec spec) {
	this.name = spec.getName();
	this.label = spec.getLabel();
	this.appDefinition = spec.getAppDefinition();
	this.user = spec.getUser();
	this.storage = spec.getStorage();
	this.error = spec.getError();
    }

    @SuppressWarnings("deprecation")
    public WorkspaceHubSpec(org.eclipse.theia.cloud.common.k8s.resource.workspace.v1beta2.WorkspaceV1beta2Spec spec) {
	this.name = spec.getName();
	this.label = spec.getLabel();
	this.appDefinition = spec.getAppDefinition();
	this.user = spec.getUser();
	this.storage = spec.getStorage();
	this.error = spec.getError();
    }

    public String getName() {
	return name;
    }

    public String getLabel() {
	return label;
    }

    public String getAppDefinition() {
	return appDefinition;
    }

    public String getUser() {
	return user;
    }

    public String getStorage() {
	return storage;
    }

    public String getError() {
	return error;
    }

    @Override
    public String toString() {
	return "WorkspaceHubSpec [name=" + name + ", label=" + label + ", appDefinition=" + appDefinition + ", user="
		+ user + ", storage=" + storage + ", error=" + error + "]";
    }

}
