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

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;
import static org.eclipse.theia.cloud.common.util.LogMessageUtil.generateCorrelationId;

import java.util.List;
import java.util.Optional;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.common.util.WorkspaceUtil;
import org.eclipse.theia.cloud.service.BaseResource;
import org.eclipse.theia.cloud.service.K8sUtil;
import org.jboss.logging.Logger;

@Path("/service/workspace")
public class WorkspaceResource extends BaseResource {
    private static final Logger LOGGER = Logger.getLogger(WorkspaceResource.class);

    @GET
    public List<UserWorkspace> listWorkspaces(WorkspacesListRequest request) {
	String correlationId = generateCorrelationId();
	if (!isValidRequest(request)) {
	    LOGGER.info(
		    formatLogMessage(correlationId, "List workspaces call without matching appId: " + request.appId));
	    return List.of();
	}
	return K8sUtil.listWorkspaces(request.user);
    }

    @POST
    public WorkspaceCreationResponse createWorkspace(WorkspaceCreationRequest request) {
	String correlationId = generateCorrelationId();
	if (!isValidRequest(request)) {
	    LOGGER.info(
		    formatLogMessage(correlationId, "Create workspace call without matching appId: " + request.appId));
	    return WorkspaceCreationResponse.error("Create workspace call without matching appId: " + request.appId);
	}
	UserWorkspace newWorkspace = new UserWorkspace(
		WorkspaceUtil.generateWorkspaceName(request.user, request.appDefinition),
		Optional.ofNullable(request.label)
			.orElseGet(() -> WorkspaceUtil.generateWorkspaceLabel(request.user, request.appDefinition)),
		request.appDefinition, request.user);

	LOGGER.info(formatLogMessage(correlationId, "Creating workspace " + request));
	Workspace workspace = K8sUtil.createWorkspace(correlationId, newWorkspace);
	if (workspace.getSpec().getError() != null) {
	    return WorkspaceCreationResponse.error(workspace.getSpec().getError());
	}
	return WorkspaceCreationResponse.ok(new UserWorkspace(workspace.getSpec()));
    }

    @DELETE
    public boolean deleteWorkspace(WorkspaceDeleteRequest request) {
	String correlationId = generateCorrelationId();
	if (!isValidRequest(request)) {
	    LOGGER.info(
		    formatLogMessage(correlationId, "Delete workspace call without matching appId: " + request.appId));
	    return false;
	}
	if (request.workspaceName == null) {
	    LOGGER.info(formatLogMessage(correlationId, "No workspace name given"));
	    return false;
	}
	return K8sUtil.deleteWorkspace(request.workspaceName);
    }
}