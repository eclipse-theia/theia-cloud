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
package org.eclipse.theia.cloud.common.k8s.resource.session;

import org.eclipse.theia.cloud.common.k8s.resource.ResourceStatus;
import org.eclipse.theia.cloud.common.k8s.resource.session.hub.SessionHub;
import org.eclipse.theia.cloud.common.util.TheiaCloudError;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize
public class SessionStatus extends ResourceStatus {
    // This class is empty as only the common properties of the super class are
    // used. Already define a specific class to allow easier extension, properly
    // type the resources and resource clients.
    // It is planned to extend this later with Session specific status steps.

    /**
     * Default constructor.
     */

    @JsonProperty("url")
    private String url;

    @JsonProperty("error")
    private String error;

    @JsonProperty("lastActivity")
    private long lastActivity;

    public SessionStatus() {
    }

    public SessionStatus(SessionHub fromHub) {
	if (fromHub.getOperatorMessage().isPresent()) {
	    this.setOperatorMessage(fromHub.getOperatorMessage().get());
	}
	if (fromHub.getOperatorStatus().isPresent()) {
	    this.setOperatorStatus(fromHub.getOperatorStatus().get());
	}
	this.url = fromHub.getUrl().orElse(null);
	this.error = fromHub.getError().orElse(null);
	this.lastActivity = fromHub.getLastActivity().orElse((long) 0);
    }

    public String getUrl() {
	return url;
    }

    public boolean hasUrl() {
	return getUrl() != null && !getUrl().isBlank();
    }

    public String getError() {
	return error;
    }

    public boolean hasError() {
	return TheiaCloudError.isErrorString(getError());
    }

    public long getLastActivity() {
	return lastActivity;
    }

    public void setUrl(String url) {
	this.url = url;
    }

    public void setError(TheiaCloudError error) {
	setError(error.asString());
    }

    public void setError(String error) {
	this.error = error;
    }

    public void setLastActivity(long lastActivity) {
	this.lastActivity = lastActivity;
    }

    @Override
    public String toString() {
	return "SessionStatus [url=" + url + ", error=" + error + ", lastActivity=" + lastActivity
		+ ", getOperatorStatus()=" + getOperatorStatus() + ", getOperatorMessage()=" + getOperatorMessage()
		+ "]";
    }

}
