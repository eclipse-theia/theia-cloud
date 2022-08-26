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

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.generateCorrelationId;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.theia.cloud.common.k8s.resource.SessionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.service.BaseResource;
import org.eclipse.theia.cloud.service.K8sUtil;
import org.eclipse.theia.cloud.service.workspace.UserWorkspace;

@Path("/service/session")
public class SessionResource extends BaseResource {

    @Operation(summary = "List sessions", description = "List sessions of a user.")
    @GET
    @Path("/{appId}/{user}")
    public List<SessionSpec> list(@PathParam("appId") String appId, @PathParam("user") String user) {
	SessionListRequest request = new SessionListRequest(appId, user);
	String correlationId = generateCorrelationId();
	if (!isValidRequest(request)) {
	    info(correlationId, "List sessions call without matching appId: " + request.appId);
	    return Collections.emptyList();
	}
	info(correlationId, "Listing sessions " + request);
	return K8sUtil.listSessions(request.user);
    }

    @Operation(summary = "Start a new session", description = "Starts a new session for an existing workspace.")
    @POST
    public SessionLaunchResponse start(SessionStartRequest request) {
	String correlationId = generateCorrelationId();
	if (!isValidRequest(request)) {
	    info(correlationId, "Launching session call without matching appId: " + request.appId);
	    return SessionLaunchResponse.error("AppId is not matching.");
	}
	info(correlationId, "Launching session " + request);
	if (request.isEphemeral()) {
	    return K8sUtil.launchEphemeralSession(correlationId, request.appDefinition, request.user, request.timeout);
	}

	Optional<Workspace> workspace = K8sUtil.getWorkspace(request.user,
		org.eclipse.theia.cloud.common.util.NamingUtil.asValidName(request.workspaceName));
	if (workspace.isEmpty()) {
	    info(correlationId, "No workspace for given workspace name: " + request);
	    return SessionLaunchResponse.error("No workspace for given name: " + request.workspaceName);
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
	String correlationId = generateCorrelationId();
	if (!isValidRequest(request)) {
	    info(correlationId, "Stop session call without matching appId: " + request.appId);
	    return false;
	}
	if (request.sessionName == null) {
	    // check if we are allowed to launch another workspace
	    info(correlationId, "No session name");
	    return false;
	}
	info(correlationId, "Stop session: " + request);
	return K8sUtil.stopSession(correlationId, request.sessionName, request.user);
    }

    @Operation(summary = "Report session activity", description = "Updates the last activity timestamp for a session to monitor activity.")
    @PATCH
    public boolean activity(SessionActivityRequest request) {
	String correlationId = generateCorrelationId();
	if (!isValidRequest(request)) {
	    info(correlationId, "Report activity call without matching appId: " + request.appId);
	    return false;
	}
	if (request.sessionName == null) {
	    info(correlationId, "No session name given");
	    return false;
	}
	info(correlationId, "Report session activity: " + request);
	return K8sUtil.reportSessionActivity(correlationId, request.sessionName);
    }
}
