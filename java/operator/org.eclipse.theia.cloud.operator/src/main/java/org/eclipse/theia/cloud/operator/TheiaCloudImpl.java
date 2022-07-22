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
package org.eclipse.theia.cloud.operator;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;
import static org.eclipse.theia.cloud.common.util.LogMessageUtil.generateCorrelationId;
import static org.eclipse.theia.cloud.operator.handler.TheiaCloudHandlerUtil.getAppDefinitionForSession;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.resource.Session;
import org.eclipse.theia.cloud.common.k8s.resource.SessionSpecResourceList;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.common.k8s.resource.WorkspaceSpecResourceList;
import org.eclipse.theia.cloud.operator.di.TheiaCloudOperatorModule;
import org.eclipse.theia.cloud.operator.handler.AppDefinitionAddedHandler;
import org.eclipse.theia.cloud.operator.handler.SessionAddedHandler;
import org.eclipse.theia.cloud.operator.handler.SessionRemovedHandler;
import org.eclipse.theia.cloud.operator.handler.WorkspaceHandler;
import org.eclipse.theia.cloud.operator.resource.AppDefinition;
import org.eclipse.theia.cloud.operator.resource.AppDefinitionSpec.Timeout;
import org.eclipse.theia.cloud.operator.resource.AppDefinitionSpecResourceList;
import org.eclipse.theia.cloud.operator.timeout.TimeoutStrategy;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public class TheiaCloudImpl implements TheiaCloud {

    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    private static final Logger LOGGER = LogManager.getLogger(TheiaCloudImpl.class);

    private static final String COR_ID_APPDEFINITIONPREFIX = "appdefinition-watch-";
    private static final String COR_ID_WORKSPACEPREFIX = "workspace-watch-";
    private static final String COR_ID_SESSIONPREFIX = "session-watch-";
    private static final String COR_ID_TIMEOUTPREFIX = "timeout-";

    @Inject
    private NamespacedKubernetesClient client;

    @Inject
    @Named(TheiaCloudOperatorModule.NAMESPACE)
    private String namespace;

    @Inject
    private NonNamespaceOperation<AppDefinition, AppDefinitionSpecResourceList, Resource<AppDefinition>> appDefinitionResourceClient;

    @Inject
    private NonNamespaceOperation<Session, SessionSpecResourceList, Resource<Session>> sessionResourceClient;

    @Inject
    private NonNamespaceOperation<Workspace, WorkspaceSpecResourceList, Resource<Workspace>> workspaceResourceClient;

    @Inject
    private AppDefinitionAddedHandler appDefinitionAddedHandler;

    @Inject
    private WorkspaceHandler workspaceHandler;

    @Inject
    private SessionAddedHandler sessionAddedHandler;

    @Inject
    private SessionRemovedHandler sessionRemovedHandler;

    @Inject
    private Set<TimeoutStrategy> timeoutStrategies;

    @Inject
    private TheiaCloudArguments arguments;

    private final Map<String, AppDefinition> appDefinitionCache = new ConcurrentHashMap<>();
    private final Map<String, Workspace> workspaceCache = new ConcurrentHashMap<>();
    private final Map<String, Session> sessionCache = new ConcurrentHashMap<>();

    @Override
    public void start() {
	initAppDefinitionsAndWatchForChanges();
	initWorkspacesAndWatchForChanges();
	initSessionsAndWatchForChanges();

	EXECUTOR.scheduleWithFixedDelay(this::stopTimedOutSessions, 1, 1, TimeUnit.MINUTES);
    }

    private void handleAppDefnitionEvent(Watcher.Action action, String uid, String correlationId) {
	AppDefinition appDefinition = appDefinitionCache.get(uid);
	switch (action) {
	case ADDED:
	    appDefinitionAdded(appDefinition, correlationId);
	    break;
	case DELETED:
	    appDefinitionDeleted(appDefinition, correlationId);
	    break;
	case MODIFIED:
	    appDefinitionModified(appDefinition, correlationId);
	    break;
	case ERROR:
	    appDefinitionErrored(appDefinition, correlationId);
	    break;
	case BOOKMARK:
	    appDefinitionBookmarked(appDefinition, correlationId);
	    break;
	}
    }

    private void appDefinitionAdded(AppDefinition appDefinition, String correlationId) {
	LOGGER.trace(formatLogMessage(COR_ID_APPDEFINITIONPREFIX, correlationId,
		"Delegating appDefinitionAdded to " + appDefinitionAddedHandler.getClass().getName()));
	appDefinitionAddedHandler.handle(appDefinition, correlationId);
    }

    private void appDefinitionDeleted(AppDefinition appDefinition, String correlationId) {
	// TODO
	LOGGER.warn(
		formatLogMessage(COR_ID_APPDEFINITIONPREFIX, correlationId, "appDefinitionDeleted not implemented"));
    }

    private void appDefinitionModified(AppDefinition appDefinition, String correlationId) {
	// TODO
	LOGGER.warn(
		formatLogMessage(COR_ID_APPDEFINITIONPREFIX, correlationId, "appDefinitionModified not implemented"));
    }

    private void appDefinitionErrored(AppDefinition appDefinition, String correlationId) {
	// TODO
	LOGGER.warn(
		formatLogMessage(COR_ID_APPDEFINITIONPREFIX, correlationId, "appDefinitionErrored not implemented"));
    }

    private void appDefinitionBookmarked(AppDefinition appDefinition, String correlationId) {
	// TODO
	LOGGER.warn(
		formatLogMessage(COR_ID_APPDEFINITIONPREFIX, correlationId, "appDefinitionBookmarked not implemented"));
    }

    private void handleSessionEvent(Watcher.Action action, String uid, String correlationId) {
	Session session = sessionCache.get(uid);
	switch (action) {
	case ADDED:
	    sessionAdded(session, correlationId);
	    break;
	case DELETED:
	    sessionDeleted(session, correlationId);
	    break;
	case MODIFIED:
	    sessionModified(session, correlationId);
	    break;
	case ERROR:
	    sessionErrored(session, correlationId);
	    break;
	case BOOKMARK:
	    sessionBookmarked(session, correlationId);
	    break;
	}
    }

    private void sessionAdded(Session session, String correlationId) {
	LOGGER.trace(formatLogMessage(COR_ID_SESSIONPREFIX, correlationId,
		"Delegating sessionAdded to " + sessionAddedHandler.getClass().getName()));
	sessionAddedHandler.handle(session, correlationId);
    }

    private void sessionDeleted(Session session, String correlationId) {
	LOGGER.trace(formatLogMessage(COR_ID_SESSIONPREFIX, correlationId,
		"Delegating sessionDeleted to " + sessionRemovedHandler.getClass().getName()));
	sessionRemovedHandler.handle(session, correlationId);
    }

    private void sessionModified(Session session, String correlationId) {
	// TODO
	LOGGER.warn(formatLogMessage(COR_ID_SESSIONPREFIX, correlationId, "sessionModified not implemented"));
    }

    private void sessionErrored(Session session, String correlationId) {
	// TODO
	LOGGER.warn(formatLogMessage(COR_ID_SESSIONPREFIX, correlationId, "sessionErrored not implemented"));
    }

    private void sessionBookmarked(Session session, String correlationId) {
	// TODO
	LOGGER.warn(formatLogMessage(COR_ID_SESSIONPREFIX, correlationId, "sessionBookmarked not implemented"));
    }

    protected void initAppDefinitionsAndWatchForChanges() {
	try {
	    /* init existing app definitions */
	    appDefinitionResourceClient.list().getItems().forEach(this::initAppDefinition);

	    /* watch for changes */
	    appDefinitionResourceClient.watch(new SpecWatch<AppDefinition>(appDefinitionCache,
		    this::handleAppDefnitionEvent, "App Definition", COR_ID_APPDEFINITIONPREFIX));
	} catch (Exception e) {
	    LOGGER.error(formatLogMessage(Main.COR_ID_INIT, "Error while initializing app definitions watch"), e);
	    System.exit(-1);
	}
    }

    protected void initWorkspacesAndWatchForChanges() {
	try {
	    /* init existing sessions */
	    workspaceResourceClient.list().getItems().forEach(this::initWorkspace);

	    /* watch for changes */
	    workspaceResourceClient.watch(new SpecWatch<Workspace>(workspaceCache, this::handleWorkspaceEvent,
		    "Workspace", COR_ID_SESSIONPREFIX));
	} catch (Exception e) {
	    LOGGER.error(formatLogMessage(Main.COR_ID_INIT, "Error while initializing workspace watch"), e);
	    System.exit(-1);
	}
    }

    protected void initSessionsAndWatchForChanges() {
	try {
	    /* init existing sessions */
	    sessionResourceClient.list().getItems().forEach(this::initSession);

	    /* watch for changes */
	    sessionResourceClient.watch(
		    new SpecWatch<Session>(sessionCache, this::handleSessionEvent, "Session", COR_ID_SESSIONPREFIX));
	} catch (Exception e) {
	    LOGGER.error(formatLogMessage(Main.COR_ID_INIT, "Error while initializing session watch"), e);
	    System.exit(-1);
	}
    }

    protected void initAppDefinition(AppDefinition resource) {
	appDefinitionCache.put(resource.getMetadata().getUid(), resource);
	String uid = resource.getMetadata().getUid();
	handleAppDefnitionEvent(Watcher.Action.ADDED, uid, Main.COR_ID_INIT);
    }

    protected void initWorkspace(Workspace resource) {
	workspaceCache.put(resource.getMetadata().getUid(), resource);
	String uid = resource.getMetadata().getUid();
	handleWorkspaceEvent(Watcher.Action.ADDED, uid, Main.COR_ID_INIT);
    }

    protected void initSession(Session resource) {
	sessionCache.put(resource.getMetadata().getUid(), resource);
	String uid = resource.getMetadata().getUid();
	handleSessionEvent(Watcher.Action.ADDED, uid, Main.COR_ID_INIT);
    }

    protected void stopTimedOutSessions() {
	String correlationId = generateCorrelationId();

	try {
	    Set<String> timedOutSessions = new LinkedHashSet<>();
	    Instant now = Instant.now();
	    for (Session session : sessionResourceClient.list().getItems()) {
		if (isSessionTimedOut(correlationId, now, session)) {
		    timedOutSessions.add(session.getSpec().getName());
		}
	    }

	    for (String sessionName : timedOutSessions) {
		sessionResourceClient.withName(sessionName).delete();
	    }
	} catch (Exception e) {
	    LOGGER.error(formatLogMessage(COR_ID_TIMEOUTPREFIX, correlationId, "Exception in kill after runnable"), e);
	}
    }

    protected boolean isSessionTimedOut(String correlationId, Instant now, Session session) {
	Optional<Timeout> timeout = getTimeoutForAppDefinition(session.getSpec().getAppDefinition());
	if (timeout.isEmpty() && timeout.get().getLimit() <= 0) {
	    LOGGER.trace(formatLogMessage(COR_ID_TIMEOUTPREFIX, correlationId,
		    "Session " + session.getSpec().getName() + " will not be stopped automatically [NoTimout]."));
	    return false;
	}
	String strategyName = timeout.get().getStrategy();
	int limit = timeout.get().getLimit();
	Optional<TimeoutStrategy> strategy = timeoutStrategies.stream()
		.filter(registeredStrategy -> registeredStrategy.getName().equals(strategyName)).findAny();
	if (strategy.isPresent() && strategy.get().evaluate(COR_ID_TIMEOUTPREFIX, session, now, limit)) {
	    LOGGER.trace(formatLogMessage(COR_ID_TIMEOUTPREFIX, correlationId, "Session " + session.getSpec().getName()
		    + " was stopped after " + limit + " minutes [" + strategyName + "]."));
	    return true;
	} else {
	    LOGGER.trace(formatLogMessage(COR_ID_TIMEOUTPREFIX, correlationId, "Session " + session.getSpec().getName()
		    + " will keep running until the limit of " + limit + " is hit [" + strategyName + "]."));
	}
	return false;
    }

    protected Optional<Timeout> getTimeoutForAppDefinition(String appDefinition) {
	// given arguments will override app definition
	Optional<Timeout> appDefTimeout = getAppDefinitionForSession(client, namespace, appDefinition)
		.map(appDef -> appDef.getSpec().getTimeout());
	Optional<String> strategyName = Optional.ofNullable(arguments.getTimeoutStrategy())
		.or(() -> appDefTimeout.map(Timeout::getStrategy));
	Optional<Integer> limit = Optional.ofNullable(arguments.getTimeoutLimit())
		.or(() -> appDefTimeout.map(Timeout::getLimit));
	return strategyName.isPresent() && limit.isPresent() ? Optional.of(new Timeout(strategyName.get(), limit.get()))
		: Optional.empty();
    }

    protected void handleWorkspaceEvent(Watcher.Action action, String uid, String correlationId) {
	Workspace workspace = workspaceCache.get(uid);
	switch (action) {
	case ADDED:
	    workspaceAdded(workspace, correlationId);
	    break;
	case DELETED:
	    workspaceDeleted(workspace, correlationId);
	    break;
	case MODIFIED:
	    workspaceModified(workspace, correlationId);
	    break;
	case ERROR:
	    workspaceErrored(workspace, correlationId);
	    break;
	case BOOKMARK:
	    workspaceBookmarked(workspace, correlationId);
	    break;
	}
    }

    private void workspaceAdded(Workspace workspace, String correlationId) {
	workspaceHandler.workspaceAdded(workspace, correlationId);
    }

    private void workspaceDeleted(Workspace workspace, String correlationId) {
	workspaceHandler.workspaceDeleted(workspace, correlationId);
    }

    private void workspaceModified(Workspace workspace, String correlationId) {
	// TODO
	LOGGER.warn(formatLogMessage(COR_ID_WORKSPACEPREFIX, correlationId, "workspaceModified not implemented"));
    }

    private void workspaceErrored(Workspace workspace, String correlationId) {
	// TODO
	LOGGER.warn(formatLogMessage(COR_ID_WORKSPACEPREFIX, correlationId, "workspaceErrored not implemented"));
    }

    private void workspaceBookmarked(Workspace workspace, String correlationId) {
	// TODO
	LOGGER.warn(formatLogMessage(COR_ID_WORKSPACEPREFIX, correlationId, "workspaceBookmarked not implemented"));
    }

}
