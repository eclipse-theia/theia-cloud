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

import java.util.Optional;

import org.eclipse.theia.cloud.common.util.WorkspaceUtil;
import org.eclipse.theia.cloud.service.ServiceRequest;

public class WorkspaceCreationRequest extends ServiceRequest {

    public String user;
    public String appDefinition;
    public String label;

    public WorkspaceCreationRequest() {
    }

    public WorkspaceCreationRequest(String appId, String appDefinition, String user, String label) {
	super(appId);
	this.appDefinition = appDefinition;
	this.user = user;
	this.label = label;
    }

    public UserWorkspace toUserWorkspace() {
	return new UserWorkspace(WorkspaceUtil.generateWorkspaceName(user, appDefinition),
		Optional.ofNullable(label).orElseGet(() -> WorkspaceUtil.generateWorkspaceLabel(user, appDefinition)),
		appDefinition, user);
    }

    @Override
    public String toString() {
	return "WorkspaceCreationRequest [user=" + user + ", appDefinition=" + appDefinition + ", label=" + label + "]";
    }

}
