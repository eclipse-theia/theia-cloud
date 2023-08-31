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

import static org.eclipse.theia.cloud.common.util.NamingUtil.asValidName;

import java.util.Optional;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.Workspace;
import org.eclipse.theia.cloud.common.util.TheiaCloudError;
import org.eclipse.theia.cloud.service.workspace.UserWorkspace;

import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/service")
@Authenticated
public class RootResource extends BaseResource {

    @Inject
    public RootResource(ApplicationProperties applicationProperties) {
	super(applicationProperties);
    }

    @Inject
    private K8sUtil k8sUtil;

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
	final EvaluatedRequest evaluatedRequest = evaluateRequest(request);
	final String correlationId = evaluatedRequest.getCorrelationId();
	final String user = evaluatedRequest.getUser();

	if (!k8sUtil.hasAppDefinition(request.appDefinition)) {
	    error(correlationId,
		    "Failed to lauch session. App Definition '" + request.appDefinition + "' does not exist.");
	    throw new TheiaCloudWebException(TheiaCloudError.INVALID_APP_DEFINITION_NAME);
	}

	if (request.isEphemeral()) {
	    info(correlationId, "Launching ephemeral session " + request);
	    return k8sUtil.launchEphemeralSession(correlationId, request.appDefinition, user, request.timeout,
		    request.env);
	}

	if (request.isExistingWorkspace()) {
	    Optional<Workspace> workspace = k8sUtil.getWorkspace(user, asValidName(request.workspaceName));
	    if (workspace.isPresent()) {
		String workspaceAppDefinition = workspace.get().getSpec().getAppDefinition();
		if (!workspaceAppDefinition.equals(request.appDefinition)) {
		    error(correlationId,
			    "Failed to lauch session. Workspace App Definition '" + workspaceAppDefinition
				    + "' does not match Request App Definition '" + request.appDefinition
				    + "' does not exist.");
		    throw new TheiaCloudWebException(TheiaCloudError.APP_DEFINITION_NAME_MISMATCH);
		}

		info(correlationId, "Launching existing workspace session " + request);
		return k8sUtil.launchWorkspaceSession(correlationId, new UserWorkspace(workspace.get().getSpec()),
			request.timeout, request.env);
	    }
	}

	info(correlationId, "Create workspace " + request);
	Workspace workspace = k8sUtil.createWorkspace(correlationId,
		new UserWorkspace(request.appDefinition, user, request.workspaceName, request.label));
	TheiaCloudWebException.throwIfErroneous(workspace);

	info(correlationId, "Launch workspace session " + request);
	try {
	    return k8sUtil.launchWorkspaceSession(correlationId, new UserWorkspace(workspace.getSpec()),
		    request.timeout, request.env);
	} catch (Exception exception) {
	    info(correlationId, "Delete workspace due to launch error " + request);
	    k8sUtil.deleteWorkspace(correlationId, workspace.getSpec().getName());
	    throw exception;
	}
    }
}
