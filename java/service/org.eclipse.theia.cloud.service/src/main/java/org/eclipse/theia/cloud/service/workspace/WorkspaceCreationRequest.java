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
package org.eclipse.theia.cloud.service.workspace;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.theia.cloud.service.ServiceRequest;

@Schema(name = "WorkspaceCreationRequest", description = "Request to create a new workspace.")
public class WorkspaceCreationRequest extends ServiceRequest {
    public static final String KIND = "workspaceCreationRequest";

    @Schema(description = "The user identification, usually the email address.", required = true)
    public String user;

    @Schema(description = "The app this workspace will be used with.", required = false)
    public String appDefinition;

    @Schema(description = "The label of the workspace", required = false)
    public String label;

    public WorkspaceCreationRequest() {
	super(KIND);
    }

    public WorkspaceCreationRequest(String appId, String appDefinition, String user, String label) {
	super(KIND, appId);
	this.appDefinition = appDefinition;
	this.user = user;
	this.label = label;
    }

    @Override
    public String toString() {
	return "WorkspaceCreationRequest [user=" + user + ", appDefinition=" + appDefinition + ", label=" + label
		+ ", appId=" + appId + ", kind=" + kind + "]";
    }

}
