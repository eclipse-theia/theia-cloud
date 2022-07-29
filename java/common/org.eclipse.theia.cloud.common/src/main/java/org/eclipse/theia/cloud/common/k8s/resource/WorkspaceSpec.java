/********************************************************************************
 * Copyright (C) 2022 EclipseSource, Lockular, Ericsson, STMicroelectronics and 
 * others.
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize()
public class WorkspaceSpec implements UserScopedSpec {

    public static final String API = "theia.cloud/v1beta";
    public static final String KIND = "Workspace";
    public static final String CRD_NAME = "workspaces.theia.cloud";

    @JsonProperty("name")
    private String name;

    @JsonProperty("label")
    private String label;

    @JsonProperty("appDefinition")
    private String appDefinition;

    @JsonProperty("user")
    private String user;

    @JsonProperty("storage")
    private String storage;

    @JsonProperty("error")
    private String error;

    public WorkspaceSpec() {
    }

    public WorkspaceSpec(String name, String label, String appDefinition, String user) {
	this.name = name;
	this.appDefinition = appDefinition;
	this.user = user;
	this.label = label;
    }

    public WorkspaceSpec(String error) {
	this.error = error;
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

    @Override
    public String getUser() {
	return user;
    }

    public void setLabel(String label) {
	this.label = label;
    }

    public String getStorage() {
	return storage;
    }

    public void setStorage(String storage) {
	this.storage = storage;
    }

    public String getError() {
	return error;
    }

    public void setError(String error) {
	this.error = error;
    }

    public boolean hasStorage() {
	return getStorage() != null && !getStorage().isBlank();
    }

    public boolean hasError() {
	return getError() != null && !getError().isBlank();
    }

    @Override
    public String toString() {
	return "WorkspaceSpec [name=" + name + ", label=" + label + ", appDefinition=" + appDefinition + ", user="
		+ user + ", error=" + error + "]";
    }

}
