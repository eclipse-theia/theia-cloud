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

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.Workspace;
import org.eclipse.theia.cloud.common.util.SessionUtil;
import org.eclipse.theia.cloud.common.util.TheiaCloudError;
import org.eclipse.theia.cloud.service.ApplicationProperties;
import org.eclipse.theia.cloud.service.BaseResource;
import org.eclipse.theia.cloud.service.EvaluatedRequest;
import org.eclipse.theia.cloud.service.K8sUtil;
import org.eclipse.theia.cloud.service.NoAnonymousAccess;
import org.eclipse.theia.cloud.service.TheiaCloudWebException;
import org.eclipse.theia.cloud.service.workspace.UserWorkspace;

import io.quarkus.security.Authenticated;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

@Authenticated
@Path("/service/session")
public class SessionResource extends BaseResource {

    private static final String CONFIG_STORE_PATH = "/theia-cloud/config-store";
    /** Timeout in milliseconds for calls to a session's config store. */
    private static final long CONFIG_STORE_HTTP_TIMEOUT = 5000L;

    @Inject
    private K8sUtil k8sUtil;

    protected final WebClient webClient;

    @Inject
    public SessionResource(Vertx vertx, ApplicationProperties applicationProperties) {
        super(applicationProperties);
        WebClientOptions options = new WebClientOptions().setFollowRedirects(false);
        webClient = WebClient.create(vertx, options);
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

        SessionSpec existingSession = k8sUtil.findSession(request.sessionName).map(Session::getSpec).orElse(null);
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
        SessionSpec existingSession = k8sUtil.findSession(request.sessionName).map(Session::getSpec).orElse(null);
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

    @POST
    @Path("/{session}/config")
    @Operation(summary = "Set config value", description = "Sets a config value in the config store if it is available. This requires the @eclipse-theiacloud/config-store Theia extension to be present in the application.")
    @Parameter(name = "session", description = "The name of the session")
    @Consumes(MediaType.APPLICATION_JSON)
    @APIResponse(responseCode = "204", description = "Config value set successfully.")
    @APIResponse(responseCode = "403", description = "User does not own the session.")
    @APIResponse(responseCode = "474", description = "Session not found.")
    @APIResponse(responseCode = "500", description = "Internal server error while setting config value or other unexpected errors.")
    @APIResponse(responseCode = "580", description = "Config store not available.")
    @NoAnonymousAccess
    public void setConfigValue(@PathParam("session") String sessionName, SessionSetConfigValueRequest request) {
        String correlationId = evaluateRequest(request);
        String user = theiaCloudUser.getIdentifier();

        // It would be preferable to ping the pod first to see if it supports the config store.
        // However, we need to know the session to get the pod IP and port.
        Session session = k8sUtil.findSession(sessionName).orElseThrow(() -> {
            info(correlationId, "Session " + sessionName + " does not exist.");
            return new TheiaCloudWebException(TheiaCloudError.INVALID_SESSION_NAME);
        });
        if (!isOwner(theiaCloudUser.getIdentifier(), session.getSpec())) {
            info(correlationId, "User " + user + " does not own session " + sessionName);
            throw new TheiaCloudWebException(Status.FORBIDDEN);
        }

        // Use internal service URL for service-to-service communication to bypass OAuth2 proxy

        Optional<String> sessionPodInternalUrl = SessionUtil.getInternalClusterURL(k8sUtil.CLIENT, session);
        if (sessionPodInternalUrl.isEmpty()) {
            error(correlationId, "Could not determine internal cluster URL for Session " + session);
            throw new InternalServerErrorException("Could not get session internal service URL");
        }

        String configStoreUrl = sessionPodInternalUrl.get() + CONFIG_STORE_PATH;
        logger.info("Config store URL (internal): " + configStoreUrl);

        try {
            // First, ping the config store to see if it is available.
            var pingResponse = webClient.getAbs(configStoreUrl).timeout(CONFIG_STORE_HTTP_TIMEOUT).send().await()
                    .indefinitely();

            // Handle ping response. Theia returns a 404 if the config store is not installed.
            if (pingResponse.statusCode() < 200 || pingResponse.statusCode() >= 300) {
                String message = MessageFormat.format(
                        "Failed to reach config store of session '{0}' with app definition '{1}'. Config store is probably not installed in application.",
                        sessionName, session.getSpec().getAppDefinition());
                error(correlationId, message);
                throw new TheiaCloudWebException(TheiaCloudError.CONFIG_STORE_NOT_AVAILABLE);
            }

            trace(correlationId, MessageFormat.format("Successfully pinged config store at: %s", configStoreUrl));

            // Second, send the request to set the config value.
            JsonObject body = new JsonObject().put("key", request.key).put("value", request.value);
            var setValueResponse = webClient.postAbs(configStoreUrl).timeout(CONFIG_STORE_HTTP_TIMEOUT)
                    .sendJsonObject(body).await().indefinitely();

            if (setValueResponse.statusCode() < 200 || setValueResponse.statusCode() >= 300) {
                error(correlationId,
                        "Failed to set config value with HTTP status code: " + setValueResponse.statusCode());
                throw new InternalServerErrorException("Failed to set config value.");
            }

            info(correlationId, "Config value set successfully for key: " + request.key);

        } catch (TheiaCloudWebException | InternalServerErrorException e) {
            // Re-throw expected exceptions
            throw e;
        } catch (Exception e) {
            error(correlationId, "Unexpected error while trying to set config value.", e);
            throw new InternalServerErrorException("Unexpected error while trying to set config value.");
        }
    }

    protected boolean isOwner(String user, SessionSpec session) {
        if (session.getUser() == null || session.getUser().isBlank()) {
            logger.warnv("Session does not have a user. {0}", session);
            return false;
        }

        return session.getUser().equals(user);
    }
}
