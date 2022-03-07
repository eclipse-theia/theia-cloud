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
package org.eclipse.theia.cloud.workspace;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;
import static org.eclipse.theia.cloud.common.util.LogMessageUtil.generateCorrelationId;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.jboss.logging.Logger;

@Path("/workspaces")
public class WorkspaceResource {

    private static final Logger LOGGER = Logger.getLogger(WorkspaceResource.class);

    private static final String THEIA_CLOUD_APP_ID = "theia.cloud.app.id";
    private static final String INIT = "INIT";

    private String appId;

    public WorkspaceResource() {
	appId = System.getProperty(THEIA_CLOUD_APP_ID, "");
	LOGGER.info(formatLogMessage(INIT, "App Id: " + appId));
    }

    @POST
    public Reply launchWorkspace(Workspace workspace) {
	String correlationId = generateCorrelationId();
	if (wrongAppId(workspace)) {
	    LOGGER.info(formatLogMessage(correlationId,
		    "Launching workspace call without matching appId: " + workspace.appId));
	    return new Reply(false, "", "AppId is not matching.");
	}
	LOGGER.info(formatLogMessage(correlationId, "Launching workspace " + workspace));
	return K8sUtil.launchWorkspace(correlationId, generateWorkspaceName(workspace), workspace.template,
		workspace.user);
    }

    private boolean wrongAppId(Workspace workspace) {
	return !appId.equals(workspace.appId);
    }

    private static String generateWorkspaceName(Workspace workspace) {
	return ("ws-" + workspace.template + "-" + workspace.user).replace("@", "at").toLowerCase();
    }
}