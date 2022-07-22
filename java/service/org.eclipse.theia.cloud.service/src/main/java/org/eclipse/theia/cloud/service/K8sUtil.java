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

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.eclipse.theia.cloud.common.k8s.resource.Session;
import org.eclipse.theia.cloud.common.k8s.resource.SessionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.SessionSpecResourceList;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.common.k8s.resource.WorkspaceSpec;
import org.eclipse.theia.cloud.common.k8s.resource.WorkspaceSpecResourceList;
import org.eclipse.theia.cloud.common.k8s.resource.WorkspaceUtil;
import org.eclipse.theia.cloud.common.k8s.resource.util.K8sResourceUtil;
import org.eclipse.theia.cloud.service.session.SessionLaunchResponse;
import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public final class K8sUtil {

    private static final Logger LOGGER = Logger.getLogger(K8sUtil.class);

    private static final String COR_ID_INIT = "init";

    private static String NAMESPACE = "";
    private static NamespacedKubernetesClient CLIENT = createClient();

    private K8sUtil() {
    }

    private static NamespacedKubernetesClient createClient() {
	Config config = new ConfigBuilder().build();

	/* don't close resource */
	DefaultKubernetesClient client = new DefaultKubernetesClient(config);

	String namespace = client.getNamespace();
	K8sUtil.NAMESPACE = namespace;
	LOGGER.info(formatLogMessage(COR_ID_INIT, "Namespace: " + namespace));

	K8sResourceUtil.registerSessionResource(client);
	K8sResourceUtil.registerWorkspaceResource(client);

	return client;
    }

    public static Workspace createWorkspace(String correlationId, UserWorkspace data) {
	NonNamespaceOperation<Workspace, WorkspaceSpecResourceList, Resource<Workspace>> workspaces = CLIENT
		.resources(Workspace.class, WorkspaceSpecResourceList.class).inNamespace(K8sUtil.NAMESPACE);
	Resource<Workspace> existingWorkspace = workspaces.withName(data.name);
	if (existingWorkspace.get() != null) {
	    return existingWorkspace.get();
	}

	Workspace workspace = new Workspace();

	ObjectMeta metadata = new ObjectMeta();
	metadata.setName(data.name);
	workspace.setMetadata(metadata);

	WorkspaceSpec spec = new WorkspaceSpec(data.name, data.label, data.appDefinition, data.user);
	workspace.setSpec(spec);

	workspaces.create(workspace);

	CountDownLatch latch = new CountDownLatch(1);
	Watch watch = workspaces.watch(new Watcher<Workspace>() {
	    @Override
	    public void eventReceived(Action action, Workspace resource) {
		if (resource.getSpec().getName().equals(workspace.getSpec().getName())
			&& resource.getSpec().getStorage() != null) {
		    workspace.getSpec().setStorage(resource.getSpec().getStorage());
		    latch.countDown();
		}
	    }

	    @Override
	    public void onClose(WatcherException cause) {
		// do nothing
	    }
	});
	try {
	    latch.await(1, TimeUnit.MINUTES);
	} catch (InterruptedException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Timeout while waiting for workspace storage " + data.name),
		    e);
	    workspace.getSpec().setError("Timeout while waiting for workspace storage " + data.name);
	    return workspace;
	} finally {
	    if (watch != null) {
		watch.close();
	    }
	}
	return workspace;
    }

    public static List<SessionSpec> listSessions(String user) {
	NonNamespaceOperation<Session, SessionSpecResourceList, Resource<Session>> sessions = CLIENT
		.resources(Session.class, SessionSpecResourceList.class).inNamespace(K8sUtil.NAMESPACE);
	return sessions.list().getItems().stream().filter(session -> Objects.equals(session.getSpec().getUser(), user))
		.map(session -> session.getSpec()).collect(Collectors.toList());
    }

    public static SessionLaunchResponse launchSession(String correlationId, UserWorkspace workspace) {
	String sessionName = WorkspaceUtil.getSessionName(workspace.name);

	NonNamespaceOperation<Session, SessionSpecResourceList, Resource<Session>> sessions = CLIENT
		.resources(Session.class, SessionSpecResourceList.class).inNamespace(K8sUtil.NAMESPACE);
	Resource<Session> existingSession = sessions.withName(sessionName);

	String createSpecName;
	SessionSpec existingSessionSpec = null;
	if (existingSession.get() == null) {
	    Session sessionSpecResource = new Session();

	    ObjectMeta metadata = new ObjectMeta();
	    sessionSpecResource.setMetadata(metadata);
	    metadata.setName(sessionName);

	    SessionSpec sessionSpec = new SessionSpec(sessionName, workspace.appDefinition, workspace.user,
		    workspace.name);
	    sessionSpec.setLastActivity(Instant.now().toEpochMilli());
	    sessionSpecResource.setSpec(sessionSpec);

	    Session created = sessions.create(sessionSpecResource);
	    createSpecName = created.getSpec().getName();
	} else {
	    createSpecName = existingSession.get().getSpec().getName();
	    existingSessionSpec = existingSession.get().getSpec();
	}

	AtomicReference<String> atomicReferenceURL = new AtomicReference<String>(null);
	AtomicReference<String> atomicReferenceError = new AtomicReference<String>(null);
	CountDownLatch latch = new CountDownLatch(1);

	Watch watch = null;
	if (existingSessionSpec != null
		&& ((existingSessionSpec.getUrl() != null && !existingSessionSpec.getUrl().isBlank())
			|| (existingSessionSpec.getError() != null && !existingSessionSpec.getError().isBlank()))) {
	    LOGGER.info(formatLogMessage(correlationId, "Session existing with result"));
	    if (existingSessionSpec.getUrl() != null && !existingSessionSpec.getUrl().isBlank()) {
		atomicReferenceURL.set(existingSessionSpec.getUrl());
		latch.countDown();
	    }
	    if (existingSessionSpec.getError() != null && !existingSessionSpec.getError().isBlank()) {
		atomicReferenceError.set(existingSessionSpec.getError());
		sessions.withName(sessionName).delete();
		latch.countDown();
	    }
	} else {
	    watch = sessions.watch(new Watcher<Session>() {

		@Override
		public void eventReceived(Action action, Session resource) {
		    LOGGER.trace(
			    formatLogMessage(correlationId, "Received session event " + action + " for " + resource));
		    if (createSpecName.equals(resource.getSpec().getName())) {
			if (resource.getSpec().getUrl() != null && !resource.getSpec().getUrl().isBlank()) {
			    LOGGER.info(formatLogMessage(correlationId, "Received URL for " + resource));
			    atomicReferenceURL.set(resource.getSpec().getUrl());
			    latch.countDown();
			} else if (resource.getSpec().getError() != null && !resource.getSpec().getError().isBlank()) {
			    LOGGER.info(formatLogMessage(correlationId,
				    "Received Error for " + resource + ". Deleting session again."));
			    atomicReferenceError.set(resource.getSpec().getError());
			    sessions.withName(sessionName).delete();
			    latch.countDown();
			}
		    }
		}

		@Override
		public void onClose(WatcherException cause) {
		}

	    });
	}

	try {
	    latch.await(1, TimeUnit.MINUTES);
	} catch (InterruptedException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Timeout while waiting for URL for " + sessionName), e);
	    return SessionLaunchResponse.error("Unable to start session");
	} finally {
	    if (watch != null) {
		watch.close();
	    }
	}

	return new SessionLaunchResponse(atomicReferenceURL.get() != null, atomicReferenceError.get(),
		atomicReferenceURL.get());

    }

    public static boolean reportSessionActivity(String correlationId, String sessionName) {
	NonNamespaceOperation<Session, SessionSpecResourceList, Resource<Session>> sessions = CLIENT
		.resources(Session.class, SessionSpecResourceList.class).inNamespace(K8sUtil.NAMESPACE);
	Resource<Session> existingSession = sessions.withName(sessionName);
	if (existingSession.get() == null) {
	    LOGGER.info(formatLogMessage(correlationId,
		    "Activity reported for session {" + sessionName + "} but session not found"));
	    return false;
	}
	return existingSession.edit(session -> {
	    session.getSpec().setLastActivity(Instant.now().toEpochMilli());
	    LOGGER.trace(formatLogMessage(correlationId, "updating activity for session {" + sessionName + "}"));
	    return session;
	}) != null;
    }

    public static boolean stopSession(String sessionName, String user) {
	NonNamespaceOperation<Session, SessionSpecResourceList, Resource<Session>> sessions = CLIENT
		.resources(Session.class, SessionSpecResourceList.class).inNamespace(K8sUtil.NAMESPACE);
	Boolean sessionDeleted = sessions.withName(sessionName).delete();
	return sessionDeleted != null && sessionDeleted;
    }

    public static boolean deleteWorkspace(String workspaceName, String user) {
	NonNamespaceOperation<Workspace, WorkspaceSpecResourceList, Resource<Workspace>> workspaces = CLIENT
		.resources(Workspace.class, WorkspaceSpecResourceList.class).inNamespace(K8sUtil.NAMESPACE);
	Boolean success = workspaces.withName(workspaceName).delete();
	return success != null && success;
    }

    public static List<UserWorkspace> listWorkspaces(String user) {
	NonNamespaceOperation<Workspace, WorkspaceSpecResourceList, Resource<Workspace>> workspaces = CLIENT
		.resources(Workspace.class, WorkspaceSpecResourceList.class).inNamespace(K8sUtil.NAMESPACE);

	List<UserWorkspace> userWorkspaces = workspaces.list().getItems().stream()
		.filter(workspace -> Objects.equals(workspace.getSpec().getUser(), user))
		.map(workspace -> new UserWorkspace(workspace.getSpec())).collect(Collectors.toList());

	// check if there is an active session for each workspace
	NonNamespaceOperation<Session, SessionSpecResourceList, Resource<Session>> sessions = CLIENT
		.resources(Session.class, SessionSpecResourceList.class).inNamespace(K8sUtil.NAMESPACE);
	for (UserWorkspace userWorkspace : userWorkspaces) {
	    String sessionName = WorkspaceUtil.getSessionName(userWorkspace.name);
	    userWorkspace.active = sessions.withName(sessionName).get() != null;
	}
	return userWorkspaces;
    }
}
