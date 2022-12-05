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

import static org.eclipse.theia.cloud.common.util.NamingUtil.asValidName;

import java.util.Optional;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.common.util.TheiaCloudError;
import org.eclipse.theia.cloud.service.workspace.UserWorkspace;

import io.quarkus.security.Authenticated;

@Path("/service")
@Authenticated
public class RootResource extends BaseResource {

    @Operation(summary = "Ping", description = "Replies if the service is available.")
    @GET
    @Path("/{appId}")
    @PermitAll
    public boolean ping(@PathParam("appId") String appId) {
	evaluateRequest(new PingRequest(appId));
	return true;
    }

    @Operation(summary = "Launch Session", description = "Launches a session and creates a workspace if required. Responds with the URL of the launched session.")
    @POST
    public String launch(LaunchRequest request) {
	String correlationId = evaluateRequest(request);

	if (!K8sUtil.hasAppDefinition(request.appDefinition)) {
	    error(correlationId,
		    "Failed to lauch session. App Definition '" + request.appDefinition + "' does not exist.");
	    throw new TheiaCloudWebException(TheiaCloudError.INVALID_APP_DEFINITION_NAME);
	}

	if (request.isEphemeral()) {
	    info(correlationId, "Launching ephemeral session " + request);
	    return K8sUtil.launchEphemeralSession(correlationId, request.appDefinition, request.user, request.timeout);
	}

	if (request.isExistingWorkspace()) {
	    Optional<Workspace> workspace = K8sUtil.getWorkspace(request.user, asValidName(request.workspaceName));
	    if (workspace.isPresent()) {
		info(correlationId, "Launching existing workspace session " + request);
		return K8sUtil.launchWorkspaceSession(correlationId, new UserWorkspace(workspace.get().getSpec()),
			request.timeout);
	    }
	}

	info(correlationId, "Create workspace " + request);
	Workspace workspace = K8sUtil.createWorkspace(correlationId,
		new UserWorkspace(request.appDefinition, request.user, request.workspaceName, request.label));
	TheiaCloudWebException.throwIfErroneous(workspace);

	info(correlationId, "Launch workspace session " + request);
	try {
	    return K8sUtil.launchWorkspaceSession(correlationId, new UserWorkspace(workspace.getSpec()),
		    request.timeout);
	} catch (Exception exception) {
	    info(correlationId, "Delete workspace due to launch error " + request);
	    K8sUtil.deleteWorkspace(correlationId, workspace.getSpec().getName());
	    throw exception;
	}
    }
}
