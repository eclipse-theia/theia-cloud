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
package org.eclipse.theia.cloud.service.session;

import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.Workspace;
import org.eclipse.theia.cloud.common.util.TheiaCloudError;
import org.eclipse.theia.cloud.service.ApplicationProperties;
import org.eclipse.theia.cloud.service.BaseResource;
import org.eclipse.theia.cloud.service.EvaluatedRequest;
import org.eclipse.theia.cloud.service.K8sUtil;
import org.eclipse.theia.cloud.service.NoAnonymousAccess;
import org.eclipse.theia.cloud.service.TheiaCloudWebException;
import org.eclipse.theia.cloud.service.workspace.UserWorkspace;

import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response.Status;

@Authenticated
@Path("/service/session")
public class SessionResource extends BaseResource {

    @Inject
    private K8sUtil k8sUtil;

    @Inject
    public SessionResource(ApplicationProperties applicationProperties) {
        super(applicationProperties);
    }

    @Operation(summary = "List sessions", description = "List sessions of a user.")
    @GET
    @Path("/{appId}/{user}")
    @NoAnonymousAccess
    public List<SessionSpec> list(@PathParam("appId") String appId, @PathParam("user") String user) {
        SessionListRequest request = new SessionListRequest(appId, user);
        final EvaluatedRequest evaluatedRequest = evaluateRequest(request);
        info(evaluatedRequest.getCorrelationId(), "Listing sessions " + request);
        return k8sUtil.listSessions(evaluatedRequest.getUser());
    }

    @Operation(summary = "Start a new session", description = "Starts a new session for an existing workspace and responds with the URL of the started session.")
    @POST
    @NoAnonymousAccess
    public String start(SessionStartRequest request) {
        final EvaluatedRequest evaluatedRequest = evaluateRequest(request);
        final String correlationId = evaluatedRequest.getCorrelationId();
        final String user = evaluatedRequest.getUser();

        info(correlationId, "Launching session " + request);
        if (request.isEphemeral()) {
            return k8sUtil.launchEphemeralSession(correlationId, request.appDefinition, user, request.timeout,
                    request.env);
        }

        Optional<Workspace> workspace = k8sUtil.getWorkspace(user,
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
        return k8sUtil.launchWorkspaceSession(correlationId, new UserWorkspace(workspace.get().getSpec()),
                request.timeout, request.env);
    }

    @Operation(summary = "Stop session", description = "Stops a session.")
    @DELETE
    @NoAnonymousAccess
    public boolean stop(SessionStopRequest request) {
        final EvaluatedRequest evaluatedRequest = evaluateRequest(request);
        String correlationId = evaluatedRequest.getCorrelationId();

        if (request.sessionName == null) {
            throw new TheiaCloudWebException(TheiaCloudError.MISSING_SESSION_NAME);
        }

        SessionSpec existingSession = k8sUtil.findSession(request.sessionName).orElse(null);
        if (existingSession == null) {
            info(correlationId, "Session " + request.sessionName + " does not exist.");
            // Return true because the goal of not having a running session of the
            // given name is reached
            return true;
        }
        if (!isOwner(evaluatedRequest.getUser(), existingSession)) {
            info(correlationId, "User " + evaluatedRequest.getUser() + " does not own session " + request.sessionName);
            trace(correlationId, "Session: " + existingSession);
            throw new TheiaCloudWebException(Status.FORBIDDEN);
        }

        info(correlationId, "Stop session: " + request);
        return k8sUtil.stopSession(correlationId, request.sessionName, evaluatedRequest.getUser());
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
        return k8sUtil.reportSessionActivity(correlationId, request.sessionName);
    }

    @Operation(summary = "Get performance metrics", description = "Returns the current CPU and memory usage of the session's pod.")
    @GET
    @Path("/performance/{appId}/{sessionName}")
    @NoAnonymousAccess
    public SessionPerformance performance(@PathParam("appId") String appId,
            @PathParam("sessionName") String sessionName) {
        SessionPerformanceRequest request = new SessionPerformanceRequest(appId, sessionName);
        String correlationId = evaluateRequest(request);
        final String user = theiaCloudUser.getIdentifier();

        // Ensure session belongs to the requesting user.
        SessionSpec existingSession = k8sUtil.findSession(request.sessionName).orElse(null);
        if (existingSession == null) {
            info(correlationId, "Session " + request.sessionName + " does not exist.");
            throw new TheiaCloudWebException(TheiaCloudError.INVALID_SESSION_NAME);
        } else if (!isOwner(theiaCloudUser.getIdentifier(), existingSession)) {
            info(correlationId, "User " + user + " does not own session " + request.sessionName);
            throw new TheiaCloudWebException(Status.FORBIDDEN);
        }

        SessionPerformance performance;
        try {
            performance = k8sUtil.reportPerformance(sessionName);
        } catch (Exception e) {
            warn(correlationId, "", e);
            performance = null;
        }
        if (performance == null) {
            throw new TheiaCloudWebException(TheiaCloudError.METRICS_SERVER_UNAVAILABLE);
        }
        return performance;
    }

    protected boolean isOwner(String user, SessionSpec session) {
        if (session.getUser() == null || session.getUser().isBlank()) {
            logger.warnv("Session does not have a user. {0}", session);
            return false;
        }

        return session.getUser().equals(user);
    }
}
