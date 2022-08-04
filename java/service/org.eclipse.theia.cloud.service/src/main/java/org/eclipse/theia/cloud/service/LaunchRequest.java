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

public class LaunchRequest extends ServiceRequest {
    public static final String KIND = "launchRequest";

    public String user;
    public String appDefinition;
    public String workspaceName;
    public String label;
    public boolean ephemeral;

    public LaunchRequest() {
	super(KIND);
    }

    public boolean isEphemeral() {
	return ephemeral;
    }

    public boolean isExistingWorkspace() {
	return workspaceName != null && !workspaceName.isBlank();
    }

    public boolean isCreateWorkspace() {
	return !isExistingWorkspace() && !isEphemeral();
    }

    @Override
    public String toString() {
	return "LaunchRequest [user=" + user + ", appDefinition=" + appDefinition + ", workspaceName=" + workspaceName
		+ ", label=" + label + ", ephemeral=" + ephemeral + ", appId=" + appId + ", kind=" + kind + "]";
    }

}
