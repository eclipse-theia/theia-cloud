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
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionStatus;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.Workspace;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.WorkspaceSpec;
import org.eclipse.theia.cloud.common.util.CustomResourceUtil;
import org.eclipse.theia.cloud.service.session.SessionPerformance;
import org.eclipse.theia.cloud.service.workspace.UserWorkspace;
import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.ContainerMetrics;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public final class K8sUtil {
    private NamespacedKubernetesClient KUBERNETES = CustomResourceUtil.createClient();
    public TheiaCloudClient CLIENT = new DefaultTheiaCloudClient(KUBERNETES);

    protected final Logger logger = Logger.getLogger(getClass());

    public Workspace createWorkspace(String correlationId, UserWorkspace data) {
        WorkspaceSpec spec = new WorkspaceSpec(data.name, data.label, data.appDefinition, data.user);
        return CLIENT.workspaces().launch(correlationId, spec);
    }

    public boolean deleteWorkspace(String correlationId, String workspaceName) {
        try {
            CLIENT.workspaces().delete(correlationId, workspaceName);
        } catch (KubernetesClientException e) {
            return false;
        }
        return true;
    }

    public List<SessionSpec> listSessions(String user) {
        return CLIENT.sessions().specs(user);
    }

    public Optional<SessionSpec> findExistingSession(SessionSpec spec) {
        return CLIENT.sessions().specs().stream().filter(sessionSpec -> sessionSpec.equals(spec)).findAny();
    }

    public Optional<SessionSpec> findSession(String sessionName) {
        return CLIENT.sessions().get(sessionName).map(Session::getSpec);
    }

    public String launchEphemeralSession(String correlationId, String appDefinition, String user, int timeout,
            EnvironmentVars env) {
        SessionSpec sessionSpec = new SessionSpec(getSessionName(user, appDefinition, false), appDefinition, user);
        sessionSpec = sessionSpecWithEnv(sessionSpec, env);

        return launchSession(correlationId, sessionSpec, timeout);
    }

    public String launchWorkspaceSession(String correlationId, UserWorkspace workspace, int timeout,
            EnvironmentVars env) {
        SessionSpec sessionSpec = new SessionSpec(getSessionName(workspace.name), workspace.appDefinition,
                workspace.user, workspace.name);
        sessionSpec = sessionSpecWithEnv(sessionSpec, env);

        return launchSession(correlationId, sessionSpec, timeout);
    }

    private String launchSession(String correlationId, SessionSpec sessionSpec, int timeout) {
        SessionStatus status = CLIENT.sessions().launch(correlationId, sessionSpec, timeout).getNonNullStatus();
        TheiaCloudWebException.throwIfErroneous(status);
        return status.getUrl();
    }

    private SessionSpec sessionSpecWithEnv(SessionSpec spec, EnvironmentVars env) {
        if (env == null)
            return spec;

        return new SessionSpec(spec.getName(), spec.getAppDefinition(), spec.getUser(), spec.getWorkspace(),
                env.fromMap, env.fromConfigMaps, env.fromSecrets);
    }

    public boolean reportSessionActivity(String correlationId, String sessionName) {
        return CLIENT.sessions().reportActivity(correlationId, sessionName);
    }

    public boolean stopSession(String correlationId, String sessionName, String user) {
        try {
            CLIENT.sessions().delete(correlationId, sessionName);
        } catch (KubernetesClientException e) {
            return false;
        }
        return true;
    }

    public Optional<WorkspaceSpec> findWorkspace(String workspaceName) {
        return CLIENT.workspaces().get(workspaceName).map(Workspace::getSpec);
    }

    public Optional<Workspace> getWorkspace(String user, String workspaceName) {
        return CLIENT.workspaces().get(workspaceName)
                .filter(workspace -> Objects.equals(workspace.getSpec().getUser(), user));
    }

    public List<UserWorkspace> listWorkspaces(String user) {
        List<Workspace> workspaces = CLIENT.workspaces().list(user);

        List<UserWorkspace> userWorkspaces = workspaces.stream()
                .map(workspace -> new UserWorkspace(workspace.getSpec())).collect(Collectors.toList());

        for (UserWorkspace userWorkspace : userWorkspaces) {
            String sessionName = getSessionName(userWorkspace.name);
            userWorkspace.active = CLIENT.sessions().has(sessionName);
        }
        return userWorkspaces;
    }

    public SessionPerformance reportPerformance(String sessionName) {
        Optional<Session> optionalSession = CLIENT.sessions().get(sessionName);
        if (optionalSession.isEmpty()) {
            return null;
        }
        Session session = optionalSession.get();
        Optional<Pod> optionalPod = getPodForSession(session);
        if (optionalPod.isEmpty()) {
            return null;
        }
        PodMetrics test = CLIENT.kubernetes().top().pods().metrics(CLIENT.namespace(),
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
    }

    public Optional<Pod> getPodForSession(Session session) {
        PodList podlist = CLIENT.kubernetes().pods().list();
        return podlist.getItems().stream().filter(pod -> isPodFromSession(pod, session)).findFirst();
    }

    private boolean isPodFromSession(Pod pod, Session session) {
        Optional<Container> optionalContainer = pod.getSpec().getContainers().stream()
                .filter(con -> con.getName().equals(session.getSpec().getAppDefinition())).findFirst();
        if (optionalContainer.isEmpty()) {
            return false;
        }
        Container container = optionalContainer.get();
        Optional<EnvVar> optionalEnv = container.getEnv().stream()
                .filter(env -> env.getName().equals("THEIACLOUD_SESSION_NAME")).findFirst();
        if (optionalEnv.isEmpty()) {
            return false;
        }
        EnvVar env = optionalEnv.get();
        return env.getValue().equals(session.getSpec().getName());
    }

    public boolean hasAppDefinition(String appDefinition) {
        return CLIENT.appDefinitions().get(appDefinition).isPresent();
    }

    public boolean isMaxInstancesReached(String appDefString) {
        Optional<AppDefinition> optAppDef = CLIENT.appDefinitions().get(appDefString);
        if (!optAppDef.isPresent()) {
            return true; // appDef does not exist, so we already reached the maximum number of instances
        }
        AppDefinition appDef = optAppDef.get();
        if (appDef.getSpec().getMaxInstances() == null) {
            return false; // max instances is not set, so we do not have a limit
        }
        long maxInstances = appDef.getSpec().getMaxInstances();
        if (maxInstances < 0) {
            return false; // max instances is set to negative, so we can ignore it
        }
        long podsOfAppDef = CLIENT.sessions().list().stream() // All sessions
                .filter(s -> s.getSpec().getAppDefinition().equals(appDefString)) // That are from the appDefinition
                .filter(s -> getPodForSession(s).isPresent()).count(); // That already have a pod
        return podsOfAppDef >= maxInstances;
    }
}
