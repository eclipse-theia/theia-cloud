/********************************************************************************
 * Copyright (C) 2022-2023 EclipseSource and others.
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

@Schema(name = "LaunchRequest", description = "A request to launch a new session.")
public class LaunchRequest extends UserScopedServiceRequest {
    public static final String KIND = "launchRequest";

    @Schema(description = "The app to launch. Needs to be set if a new or ephemeral session should be launched. For an existing workspace the last app definition will be used if none is given.", required = false)
    public String appDefinition;

    @Schema(description = "The name of the workspace to mount/create. Needs to be set if an existing workspace should be launched.", required = false)
    public String workspaceName;

    @Schema(description = "The label of the workspace to mount/create. If no label is given, a default label will be generated.", required = false)
    public String label;

    @Schema(description = "If true no workspace will be created for the session.", required = false)
    public boolean ephemeral;

    @Schema(description = "Number of minutes to wait for session launch. Default is 3 Minutes.", required = false)
    public int timeout = 3;

    @Schema(description = "Environment variables", required = false)
    public EnvironmentVars env = new EnvironmentVars();

    @Schema(description = "Git Init information", required = false)
    public GitInit gitInit;

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
		+ ", label=" + label + ", ephemeral=" + ephemeral + ", timeout=" + timeout + ", env=" + env
		+ ", gitInit=" + gitInit + "]";
    }

}
