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

import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.eclipse.theia.cloud.service.session.SessionLaunchResponse;
import org.eclipse.theia.cloud.service.session.SessionResource;
import org.eclipse.theia.cloud.service.session.SessionStartRequest;
import org.eclipse.theia.cloud.service.workspace.WorkspaceCreationRequest;
import org.eclipse.theia.cloud.service.workspace.WorkspaceCreationResponse;
import org.eclipse.theia.cloud.service.workspace.WorkspaceResource;

@Path("/service")
public class RootResource extends BaseResource {
    @POST
    public SessionLaunchResponse createAndLaunchSession(WorkspaceCreationRequest request) {
	WorkspaceCreationResponse response = new WorkspaceResource().createWorkspace(request);
	if (response.error != null) {
	    return SessionLaunchResponse.error(response.error);
	}
	SessionLaunchResponse launchResponse = new SessionResource()
		.launchSession(new SessionStartRequest(request.appId, request.user, response.workspace.name));
	if (!launchResponse.success) {
	    K8sUtil.deleteWorkspace(response.workspace.name);
	}
	return launchResponse;
    }
}
