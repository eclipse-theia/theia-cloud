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

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.generateCorrelationId;
import static org.eclipse.theia.cloud.common.util.NamingUtil.asValidName;

import java.util.Optional;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.service.session.SessionLaunchResponse;
import org.eclipse.theia.cloud.service.workspace.UserWorkspace;

@Path("/service")
public class RootResource extends BaseResource {

    @Operation(summary = "Launch Session", description = "Launches a session and creates a workspace if required.")
    @POST
    public SessionLaunchResponse launch(LaunchRequest request) {
	String correlationId = generateCorrelationId();
	if (!isValidRequest(request)) {
	    info(correlationId, "Launch call without matching appId: " + request.appId);
	    return SessionLaunchResponse.error("Launch call without matching appId: " + request.appId);
	}

	if (request.isEphemeral()) {
	    info(correlationId, "Launching ephemeral session " + request);
	    return K8sUtil.launchEphemeralSession(correlationId, request.appDefinition, request.user);
	}

	if (request.isExistingWorkspace()) {
	    Optional<Workspace> workspace = K8sUtil.getWorkspace(request.user, asValidName(request.workspaceName));
	    if (workspace.isPresent()) {
		info(correlationId, "Launching existing workspace session " + request);
		return K8sUtil.launchWorkspaceSession(correlationId, new UserWorkspace(workspace.get().getSpec()));
	    }
	}

	info(correlationId, "Create workspace " + request);
	Workspace workspace = K8sUtil.createWorkspace(correlationId,
		new UserWorkspace(request.appDefinition, request.user, request.workspaceName, request.label));
	if (workspace.getSpec().getError() != null) {
	    K8sUtil.deleteWorkspace(correlationId, workspace.getSpec().getName());
	    return SessionLaunchResponse.error(workspace.getSpec().getError());
	}
	info(correlationId, "Launch workspace session " + request);
	SessionLaunchResponse response = K8sUtil.launchWorkspaceSession(correlationId,
		new UserWorkspace(workspace.getSpec()));
	if (response.error != null) {
	    info(correlationId, "Delete workspace due to launch error " + request);
	    K8sUtil.deleteWorkspace(correlationId, workspace.getSpec().getName());
	}
	return response;
    }
}
