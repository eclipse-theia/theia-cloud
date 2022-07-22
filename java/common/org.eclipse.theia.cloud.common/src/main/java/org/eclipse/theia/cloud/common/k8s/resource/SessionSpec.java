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
public class SessionSpec {

    public static final String API = "theia.cloud/v1beta";
    public static final String KIND = "Session";
    public static final String CRD_NAME = "sessions.theia.cloud";

    @JsonProperty("name")
    private String name;

    @JsonProperty("appDefinition")
    private String appDefinition;

    @JsonProperty("user")
    private String user;

    @JsonProperty("url")
    private String url;

    @JsonProperty("error")
    private String error;

    @JsonProperty("workspace")
    private String workspace;

    @JsonProperty("lastActivity")
    private long lastActivity;

    public SessionSpec() {
    }

    public SessionSpec(String name, String appDefinition, String user, String workspace) {
	this.name = name;
	this.appDefinition = appDefinition;
	this.user = user;
	this.workspace = workspace;
    }

    public String getName() {
	return name;
    }

    public String getAppDefinition() {
	return appDefinition;
    }

    public String getUser() {
	return user;
    }

    public String getUrl() {
	return url;
    }

    public void setUrl(String url) {
	this.url = url;
    }

    public String getError() {
	return error;
    }

    public void setError(String error) {
	this.error = error;
    }

    public long getLastActivity() {
	return lastActivity;
    }

    public void setLastActivity(long lastActivity) {
	this.lastActivity = lastActivity;
    }

    public String getWorkspace() {
	return workspace;
    }

    public void setWorkspace(String workspace) {
	this.workspace = workspace;
    }

    @Override
    public String toString() {
	return "SessionSpec [name=" + name + ", appDefinition=" + appDefinition + ", user=" + user + ", url=" + url
		+ ", error=" + error + ", lastActivity=" + lastActivity + "]";
    }

}
