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
package org.eclipse.theia.cloud.service;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.theia.cloud.common.k8s.client.DefaultTheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.Session;
import org.eclipse.theia.cloud.common.k8s.resource.SessionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.common.k8s.resource.WorkspaceSpec;
import org.eclipse.theia.cloud.common.util.CustomResourceUtil;
import org.eclipse.theia.cloud.common.util.WorkspaceUtil;
import org.eclipse.theia.cloud.service.session.SessionLaunchResponse;
import org.eclipse.theia.cloud.service.workspace.UserWorkspace;

import io.fabric8.kubernetes.client.NamespacedKubernetesClient;

public final class K8sUtil {
    private static NamespacedKubernetesClient CLIENT = CustomResourceUtil.createClient();
    private static TheiaCloudClient CUSTOM_CLIENT = new DefaultTheiaCloudClient(CLIENT).inNamespace("theiacloud");

    private K8sUtil() {
    }

    public static Workspace createWorkspace(String correlationId, UserWorkspace data) {
	WorkspaceSpec spec = new WorkspaceSpec(data.name, data.label, data.appDefinition, data.user);
	return CUSTOM_CLIENT.workspaces().interaction(correlationId).launch(spec);
    }

    public static boolean deleteWorkspace(String workspaceName) {
	return CUSTOM_CLIENT.workspaces().delete(workspaceName);
    }

    public static List<SessionSpec> listSessions(String user) {
	return CUSTOM_CLIENT.sessions().specs();
    }

    public static SessionLaunchResponse launchSession(String correlationId, UserWorkspace workspace) {
	String sessionName = WorkspaceUtil.getSessionName(workspace.name);
	SessionSpec sessionSpec = new SessionSpec(sessionName, workspace.appDefinition, workspace.user, workspace.name);
	Session session = CUSTOM_CLIENT.sessions().interaction(correlationId).launch(sessionSpec);
	return SessionLaunchResponse.from(session.getSpec());
    }

    public static boolean reportSessionActivity(String correlationId, String sessionName) {
	return CUSTOM_CLIENT.sessions().interaction(correlationId).reportActivity(sessionName);
    }

    public static boolean stopSession(String sessionName, String user) {
	return CUSTOM_CLIENT.sessions().delete(sessionName);
    }

    public static List<UserWorkspace> listWorkspaces(String user) {
	List<Workspace> workspaces = CUSTOM_CLIENT.workspaces().list(user);

	List<UserWorkspace> userWorkspaces = workspaces.stream()
		.map(workspace -> new UserWorkspace(workspace.getSpec())).collect(Collectors.toList());

	for (UserWorkspace userWorkspace : userWorkspaces) {
	    String sessionName = WorkspaceUtil.getSessionName(userWorkspace.name);
	    userWorkspace.active = CUSTOM_CLIENT.sessions().has(sessionName);
	}
	return userWorkspaces;
    }
}
