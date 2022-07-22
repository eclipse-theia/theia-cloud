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

import org.eclipse.theia.cloud.service.ServiceResponse;
import org.eclipse.theia.cloud.service.UserWorkspace;

public class WorkspaceCreationResponse extends ServiceResponse {
    public UserWorkspace workspace;

    public WorkspaceCreationResponse() {
    }

    public WorkspaceCreationResponse(boolean success, String error, UserWorkspace workspace) {
	super(success, error);
	this.workspace = workspace;
    }

    public static WorkspaceCreationResponse error(String error) {
	return new WorkspaceCreationResponse(false, error, null);
    }

    public static WorkspaceCreationResponse ok(UserWorkspace workspace) {
	return new WorkspaceCreationResponse(true, null, workspace);
    }
}
