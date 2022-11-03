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
package org.eclipse.theia.cloud.service.session;

import java.util.List;
import java.util.Optional;

import javax.annotation.security.PermitAll;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.theia.cloud.common.k8s.resource.SessionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.common.util.TheiaCloudError;
import org.eclipse.theia.cloud.service.BaseResource;
import org.eclipse.theia.cloud.service.K8sUtil;
import org.eclipse.theia.cloud.service.TheiaCloudWebException;
import org.eclipse.theia.cloud.service.workspace.UserWorkspace;

import io.quarkus.security.Authenticated;

@Authenticated
@Path("/service/session")
public class SessionResource extends BaseResource {

    @Operation(summary = "List sessions", description = "List sessions of a user.")
    @GET
    @Path("/{appId}/{user}")
    public List<SessionSpec> list(@PathParam("appId") String appId, @PathParam("user") String user) {
	SessionListRequest request = new SessionListRequest(appId, user);
	String correlationId = evaluateRequest(request);
	info(correlationId, "Listing sessions " + request);
	return K8sUtil.listSessions(request.user);
    }

    @Operation(summary = "Start a new session", description = "Starts a new session for an existing workspace and responds with the URL of the started session.")
    @POST
    public String start(SessionStartRequest request) {
	String correlationId = evaluateRequest(request);
	info(correlationId, "Launching session " + request);
	if (request.isEphemeral()) {
	    return K8sUtil.launchEphemeralSession(correlationId, request.appDefinition, request.user, request.timeout);
	}

	Optional<Workspace> workspace = K8sUtil.getWorkspace(request.user,
		org.eclipse.theia.cloud.common.util.NamingUtil.asValidName(request.workspaceName));
	if (workspace.isEmpty()) {
	    info(correlationId, "No workspace for given workspace name: " + request);
	    throw new TheiaCloudWebException(TheiaCloudError.INVALID_WORKSPACE_NAME);
	}

	if (request.appDefinition != null) {
	    // request can override default application definition stored in workspace
	    workspace.get().getSpec().setAppDefinition(request.appDefinition);
	}
	info(correlationId, "Launch workspace session: " + request);
	return K8sUtil.launchWorkspaceSession(correlationId, new UserWorkspace(workspace.get().getSpec()),
		request.timeout);
    }

    @Operation(summary = "Stop session", description = "Stops a session.")
    @DELETE
    public boolean stop(SessionStopRequest request) {
	String correlationId = evaluateRequest(request);
	if (request.sessionName == null) {
	    throw new TheiaCloudWebException(TheiaCloudError.MISSING_SESSION_NAME);
	}
	info(correlationId, "Stop session: " + request);
	return K8sUtil.stopSession(correlationId, request.sessionName, request.user);
    }

    @Operation(summary = "Report session activity", description = "Updates the last activity timestamp for a session to monitor activity.")
    @PATCH
    @PermitAll
    public boolean activity(SessionActivityRequest request) {
	// TODO activity reporting will be removed from this service
	// There will be a dedicated service that will have direct communication with
	// the pods
	// Permit All until this is implemented
	String correlationId = evaluateRequest(request);
	if (request.sessionName == null) {
	    throw new TheiaCloudWebException(TheiaCloudError.MISSING_SESSION_NAME);
	}
	info(correlationId, "Report session activity: " + request);
	return K8sUtil.reportSessionActivity(correlationId, request.sessionName);
    }
}
