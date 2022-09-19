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

import org.eclipse.theia.cloud.common.util.TheiaCloudError;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize()
public class SessionSpec implements UserScopedSpec {

    public static final String API = "theia.cloud/v2beta";
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

    public SessionSpec(String name, String appDefinition, String user) {
	this(name, appDefinition, user, null);
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

    public boolean hasAppDefinition() {
	return getAppDefinition() != null && !getAppDefinition().isBlank();
    }

    @Override
    public String getUser() {
	return user;
    }

    public String getUrl() {
	return url;
    }

    public void setUrl(String url) {
	this.url = url;
    }

    public boolean hasUrl() {
	return getUrl() != null && !getUrl().isBlank();
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

    public long getLastActivity() {
	return lastActivity;
    }

    public void setLastActivity(long lastActivity) {
	this.lastActivity = lastActivity;
    }

    public String getWorkspace() {
	return workspace;
    }

    @JsonIgnore
    public boolean isEphemeral() {
	return isEphemeral(workspace);
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((appDefinition == null) ? 0 : appDefinition.hashCode());
	result = prime * result + ((user == null) ? 0 : user.hashCode());
	result = prime * result + ((workspace == null) ? 0 : workspace.hashCode());
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	SessionSpec other = (SessionSpec) obj;
	if (appDefinition == null) {
	    if (other.appDefinition != null)
		return false;
	} else if (!appDefinition.equals(other.appDefinition))
	    return false;
	if (user == null) {
	    if (other.user != null)
		return false;
	} else if (!user.equals(other.user))
	    return false;
	if (workspace == null) {
	    if (other.workspace != null)
		return false;
	} else if (!workspace.equals(other.workspace))
	    return false;
	return true;
    }

    @Override
    public String toString() {
	return "SessionSpec [name=" + name + ", appDefinition=" + appDefinition + ", user=" + user + ", url=" + url
		+ ", error=" + error + ", workspace=" + workspace + ", lastActivity=" + lastActivity + "]";
    }

    public static boolean isEphemeral(String workspace) {
	return workspace == null || workspace.isBlank();
    }

}
