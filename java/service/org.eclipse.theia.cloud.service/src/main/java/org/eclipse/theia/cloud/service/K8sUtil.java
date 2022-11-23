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

import static org.eclipse.theia.cloud.common.util.WorkspaceUtil.getSessionName;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.theia.cloud.common.k8s.client.DefaultTheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.Session;
import org.eclipse.theia.cloud.common.k8s.resource.SessionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.common.k8s.resource.WorkspaceSpec;
import org.eclipse.theia.cloud.common.util.CustomResourceUtil;
import org.eclipse.theia.cloud.service.session.SessionPerformance;
import org.eclipse.theia.cloud.service.workspace.UserWorkspace;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.ContainerMetrics;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;

public final class K8sUtil {
    private static NamespacedKubernetesClient KUBERNETES = CustomResourceUtil.createClient();
    private static TheiaCloudClient CLIENT = new DefaultTheiaCloudClient(KUBERNETES);

    private K8sUtil() {
    }

    public static Workspace createWorkspace(String correlationId, UserWorkspace data) {
	WorkspaceSpec spec = new WorkspaceSpec(data.name, data.label, data.appDefinition, data.user);
	return CLIENT.workspaces().launch(correlationId, spec);
    }

    public static boolean deleteWorkspace(String correlationId, String workspaceName) {
	return CLIENT.workspaces().delete(correlationId, workspaceName);
    }

    public static List<SessionSpec> listSessions(String user) {
	return CLIENT.sessions().specs(user);
    }

    public static Optional<SessionSpec> findExistingSession(SessionSpec spec) {
	return CLIENT.sessions().specs().stream().filter(sessionSpec -> sessionSpec.equals(spec)).findAny();
    }

    public static String launchEphemeralSession(String correlationId, String appDefinition, String user, int timeout) {
	SessionSpec sessionSpec = new SessionSpec(getSessionName(user, appDefinition), appDefinition, user);
	return launchSession(correlationId, sessionSpec, timeout);
    }

    public static String launchWorkspaceSession(String correlationId, UserWorkspace workspace, int timeout) {
	SessionSpec sessionSpec = new SessionSpec(getSessionName(workspace.name), workspace.appDefinition,
		workspace.user, workspace.name);
	return launchSession(correlationId, sessionSpec, timeout);
    }

    private static String launchSession(String correlationId, SessionSpec sessionSpec, int timeout) {
	SessionSpec spec = CLIENT.sessions().launch(correlationId, sessionSpec, timeout).getSpec();
	TheiaCloudWebException.throwIfErroneous(spec);
	return spec.getUrl();
    }

    public static boolean reportSessionActivity(String correlationId, String sessionName) {
	return CLIENT.sessions().reportActivity(correlationId, sessionName);
    }

    public static boolean stopSession(String correlationId, String sessionName, String user) {
	return CLIENT.sessions().delete(correlationId, sessionName);
    }

    public static Optional<Workspace> getWorkspace(String user, String workspaceName) {
	return CLIENT.workspaces().get(workspaceName)
		.filter(workspace -> Objects.equals(workspace.getSpec().getUser(), user));
    }

    public static List<UserWorkspace> listWorkspaces(String user) {
	List<Workspace> workspaces = CLIENT.workspaces().list(user);

	List<UserWorkspace> userWorkspaces = workspaces.stream()
		.map(workspace -> new UserWorkspace(workspace.getSpec())).collect(Collectors.toList());

	for (UserWorkspace userWorkspace : userWorkspaces) {
	    String sessionName = getSessionName(userWorkspace.name);
	    userWorkspace.active = CLIENT.sessions().has(sessionName);
	}
	return userWorkspaces;
    }

    public static SessionPerformance reportPerformance(String sessionName) {
	try {
	    Optional<Session> optionalSession = CLIENT.sessions().get(sessionName);
	    if (optionalSession.isEmpty()) {
		return null;
	    }
	    Session session = optionalSession.get();
	    Optional<Pod> optionalPod = getPodForSession(session);
	    if (optionalPod.isEmpty()) {
		return null;
	    }
	    PodMetrics test = CLIENT.kubernetes().top().pods().metrics("theiacloud",
		    optionalPod.get().getMetadata().getName());
	    Optional<ContainerMetrics> optionalContainer = test.getContainers().stream()
		    .filter(con -> con.getName().equals(session.getSpec().getAppDefinition())).findFirst();
	    if (optionalContainer.isEmpty()) {
		return null;
	    }
	    ContainerMetrics container = optionalContainer.get();
	    return new SessionPerformance(container.getUsage().get("cpu").getAmount(),
		    container.getUsage().get("cpu").getFormat(),
		    String.valueOf(Quantity.getAmountInBytes(container.getUsage().get("memory"))), "B");
	} catch (Exception e) {
	    return null;
	}
    }

    public static Optional<Pod> getPodForSession(Session session) {
	PodList podlist = CLIENT.kubernetes().pods().list();
	return podlist.getItems().stream().filter(pod -> isPodFromSession(pod, session)).findFirst();
    }

    private static boolean isPodFromSession(Pod pod, Session session) {
	Optional<Container> optionalContainer = pod.getSpec().getContainers().stream()
		.filter(con -> con.getName().equals(session.getSpec().getAppDefinition())).findFirst();
	if (optionalContainer.isEmpty()) {
	    return false;
	}
	Container container = optionalContainer.get();
	Optional<EnvVar> optionalEnv = container.getEnv().stream()
		.filter(env -> env.getName().equals("THEIA_CLOUD_SESSION_NAME")).findFirst();
	if (optionalEnv.isEmpty()) {
	    return false;
	}
	EnvVar env = optionalEnv.get();
	return env.getValue().equals(session.getSpec().getName()) ? true : false;
    }

}
