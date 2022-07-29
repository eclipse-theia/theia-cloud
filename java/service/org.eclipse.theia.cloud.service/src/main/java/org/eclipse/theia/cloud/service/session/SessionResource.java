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

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;
import static org.eclipse.theia.cloud.common.util.LogMessageUtil.generateCorrelationId;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.eclipse.theia.cloud.common.k8s.resource.SessionSpec;
import org.eclipse.theia.cloud.service.BaseResource;
import org.eclipse.theia.cloud.service.K8sUtil;
import org.eclipse.theia.cloud.service.workspace.UserWorkspace;
import org.jboss.logging.Logger;

@Path("/service/session")
public class SessionResource extends BaseResource {
    private static final Logger LOGGER = Logger.getLogger(SessionResource.class);

    @GET
    public List<SessionSpec> listSessions(SessionsListRequest request) {
	String correlationId = generateCorrelationId();
	if (!isValidRequest(request)) {
	    LOGGER.info(formatLogMessage(correlationId, "List sessions call without matching appId: " + request.appId));
	    return Collections.emptyList();
	}
	return K8sUtil.listSessions(request.user);
    }

    @POST
    public SessionLaunchResponse launchSession(SessionStartRequest request) {
	String correlationId = generateCorrelationId();
	if (!isValidRequest(request)) {
	    LOGGER.info(
		    formatLogMessage(correlationId, "Launching session call without matching appId: " + request.appId));
	    return SessionLaunchResponse.error("AppId is not matching.");
	}
	if (request.workspaceName == null) {
	    LOGGER.info(formatLogMessage(correlationId, "No workspace name given"));
	    return SessionLaunchResponse.error("No workspace name given.");
	}
	Optional<UserWorkspace> existingWorkspace = K8sUtil.listWorkspaces(request.user).stream()
		.filter(workspace -> Objects.equals(workspace.name, request.workspaceName)).findAny();
	if (existingWorkspace.isEmpty()) {
	    LOGGER.info(formatLogMessage(correlationId, "No workspace for given name: " + request.workspaceName));
	    return SessionLaunchResponse.error("No workspace for given name: " + request.workspaceName);
	}

	LOGGER.info(formatLogMessage(correlationId, "Launching session " + request));
	return K8sUtil.launchSession(correlationId, existingWorkspace.get());
    }

    @DELETE
    public boolean stopSession(SessionStopRequest request) {
	String correlationId = generateCorrelationId();
	if (!isValidRequest(request)) {
	    LOGGER.info(formatLogMessage(correlationId, "Stop session call without matching appId: " + request.appId));
	    return false;
	}
	if (request.sessionName == null) {
	    // check if we are allowed to launch another workspace
	    LOGGER.info(formatLogMessage(correlationId, "No session name"));
	    return false;
	}
	return K8sUtil.stopSession(request.sessionName, request.user);
    }

    @PATCH
    public boolean reportActivity(SessionActivityRequest request) {
	String correlationId = generateCorrelationId();
	if (!isValidRequest(request)) {
	    LOGGER.info(
		    formatLogMessage(correlationId, "Report activity call without matching appId: " + request.appId));
	    return false;
	}
	if (request.sessionName == null) {
	    LOGGER.info(formatLogMessage(correlationId, "No session name given"));
	    return false;
	}
	return K8sUtil.reportSessionActivity(correlationId, request.sessionName);
    }
}
