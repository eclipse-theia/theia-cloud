/********************************************************************************
 * Copyright (C) 2022 EclipseSource and others.
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
package org.eclipse.theia.cloud.service;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "Launch Request", description = "A request to launch a new session.")
public class LaunchRequest extends ServiceRequest {
    public static final String KIND = "launchRequest";

    @Schema(title = "The user identification, usually the email address.", required = true)
    public String user;

    @Schema(title = "The app to launch.", required = true)
    public String appDefinition;

    @Schema(title = "The name of the workspace to mount/create.", required = false)
    public String workspaceName;

    @Schema(title = "The label of the workspace to mount/create.", required = false)
    public String label;

    @Schema(title = "If true no workspace will be created. ", required = true)
    public boolean ephemeral;
    public int timeout = 3;

    public LaunchRequest() {
	super(KIND);
    }

    @Schema(hidden = true)
    public boolean isEphemeral() {
	return ephemeral;
    }

    @Schema(hidden = true)
    public boolean isExistingWorkspace() {
	return workspaceName != null && !workspaceName.isBlank();
    }

    @Schema(hidden = true)
    public boolean isCreateWorkspace() {
	return !isExistingWorkspace() && !isEphemeral();
    }

    @Override
    public String toString() {
	return "LaunchRequest [user=" + user + ", appDefinition=" + appDefinition + ", workspaceName=" + workspaceName
		+ ", label=" + label + ", ephemeral=" + ephemeral + ", appId=" + appId + ", kind=" + kind + ", timeout="
		+ timeout + "]";
    }

}
