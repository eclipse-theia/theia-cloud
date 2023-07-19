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
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.AppDefinitionSpec.Timeout;
import org.eclipse.theia.cloud.common.k8s.resource.Session;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.operator.handler.AppDefinitionHandler;
import org.eclipse.theia.cloud.operator.handler.SessionHandler;
import org.eclipse.theia.cloud.operator.handler.TimeoutStrategy;
import org.eclipse.theia.cloud.operator.handler.WorkspaceHandler;
import org.eclipse.theia.cloud.operator.monitor.MonitorActivityTracker;

import com.google.inject.Inject;

import io.fabric8.kubernetes.client.Watcher;

public class TheiaCloudImpl implements TheiaCloud {

    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    private static final Logger LOGGER = LogManager.getLogger(TheiaCloudImpl.class);

    private static final String COR_ID_APPDEFINITIONPREFIX = "appdefinition-watch-";
    private static final String COR_ID_WORKSPACEPREFIX = "workspace-watch-";
    private static final String COR_ID_SESSIONPREFIX = "session-watch-";
    private static final String COR_ID_TIMEOUTPREFIX = "timeout-";

    @Inject
    private TheiaCloudClient resourceClient;

    @Inject
    private MonitorActivityTracker monitorActivityTracker;

    @Inject
    private AppDefinitionHandler appDefinitionAddedHandler;

    @Inject
    private WorkspaceHandler workspaceHandler;

    @Inject
    private SessionHandler sessionHandler;

    @Inject
    private Set<TimeoutStrategy> timeoutStrategies;

    @Inject
    private TheiaCloudArguments arguments;

    private final Map<String, AppDefinition> appDefinitionCache = new ConcurrentHashMap<>();
    private final Map<String, Workspace> workspaceCache = new ConcurrentHashMap<>();
    private final Map<String, Session> sessionCache = new ConcurrentHashMap<>();

    @Override
    public void start() {
	if (arguments.isEnableMonitor() && arguments.isEnableActivityTracker()) {
	    monitorActivityTracker.start(arguments.getMonitorInterval());
	}
	initAppDefinitionsAndWatchForChanges();
	initWorkspacesAndWatchForChanges();
	initSessionsAndWatchForChanges();

	EXECUTOR.scheduleWithFixedDelay(this::stopTimedOutSessions, 1, 1, TimeUnit.MINUTES);
    }

    protected void initAppDefinitionsAndWatchForChanges() {
	try {
	    resourceClient.appDefinitions().list().forEach(this::initAppDefinition);
	    resourceClient.appDefinitions().operation().watch(new SpecWatch<>(appDefinitionCache,
		    this::handleAppDefnitionEvent, "App Definition", COR_ID_APPDEFINITIONPREFIX));
	} catch (Exception e) {
	    LOGGER.error(formatLogMessage(Main.COR_ID_INIT, "Error while initializing app definitions watch"), e);
	    System.exit(-1);
	}
    }

    protected void initWorkspacesAndWatchForChanges() {
	try {
	    resourceClient.workspaces().list().forEach(this::initWorkspace);
	    resourceClient.workspaces().operation().watch(
		    new SpecWatch<>(workspaceCache, this::handleWorkspaceEvent, "Workspace", COR_ID_WORKSPACEPREFIX));
	} catch (Exception e) {
	    LOGGER.error(formatLogMessage(Main.COR_ID_INIT, "Error while initializing workspace watch"), e);
	    System.exit(-1);
	}
    }

    protected void initSessionsAndWatchForChanges() {
	try {
	    resourceClient.sessions().list().forEach(this::initSession);

	    resourceClient.sessions().operation()
		    .watch(new SpecWatch<>(sessionCache, this::handleSessionEvent, "Session", COR_ID_SESSIONPREFIX));
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

    protected void handleAppDefnitionEvent(Watcher.Action action, String uid, String correlationId) {
	AppDefinition appDefinition = appDefinitionCache.get(uid);
	switch (action) {
	case ADDED:
	    appDefinitionAddedHandler.appDefinitionAdded(appDefinition, correlationId);
	    break;
	case DELETED:
	    appDefinitionAddedHandler.appDefinitionDeleted(appDefinition, correlationId);
	    break;
	case MODIFIED:
	    appDefinitionAddedHandler.appDefinitionModified(appDefinition, correlationId);
	    break;
	case ERROR:
	    appDefinitionAddedHandler.appDefinitionErrored(appDefinition, correlationId);
	    break;
	case BOOKMARK:
	    appDefinitionAddedHandler.appDefinitionBookmarked(appDefinition, correlationId);
	    break;
	}
    }

    protected void handleSessionEvent(Watcher.Action action, String uid, String correlationId) {
	Session session = sessionCache.get(uid);
	switch (action) {
	case ADDED:
	    sessionHandler.sessionAdded(session, correlationId);
	    break;
	case DELETED:
	    sessionHandler.sessionDeleted(session, correlationId);
	    break;
	case MODIFIED:
	    sessionHandler.sessionModified(session, correlationId);
	    break;
	case ERROR:
	    sessionHandler.sessionErrored(session, correlationId);
	    break;
	case BOOKMARK:
	    sessionHandler.sessionBookmarked(session, correlationId);
	    break;
	}
    }

    protected void handleWorkspaceEvent(Watcher.Action action, String uid, String correlationId) {
	Workspace workspace = workspaceCache.get(uid);
	switch (action) {
	case ADDED:
	    workspaceHandler.workspaceAdded(workspace, correlationId);
	    break;
	case DELETED:
	    workspaceHandler.workspaceDeleted(workspace, correlationId);
	    break;
	case MODIFIED:
	    workspaceHandler.workspaceModified(workspace, correlationId);
	    break;
	case ERROR:
	    workspaceHandler.workspaceErrored(workspace, correlationId);
	    break;
	case BOOKMARK:
	    workspaceHandler.workspaceBookmarked(workspace, correlationId);
	    break;
	}
    }

    protected void stopTimedOutSessions() {
	String correlationId = generateCorrelationId();

	try {
	    Set<String> timedOutSessions = new LinkedHashSet<>();
	    Instant now = Instant.now();
	    for (Session session : resourceClient.sessions().list()) {
		if (isSessionTimedOut(correlationId, now, session)) {
		    timedOutSessions.add(session.getSpec().getId());
		}
	    }

	    for (String sessionName : timedOutSessions) {
		resourceClient.sessions().delete(COR_ID_TIMEOUTPREFIX + correlationId, sessionName);
	    }
	} catch (Exception e) {
	    LOGGER.error(formatLogMessage(COR_ID_TIMEOUTPREFIX, correlationId, "Exception in kill after runnable"), e);
	}
    }

    protected boolean isSessionTimedOut(String correlationId, Instant now, Session session) {
	Optional<Timeout> timeout = resourceClient.appDefinitions().get(session.getSpec().getAppDefinition())
		.map(appDef -> appDef.getSpec().getTimeout());
	if (timeout.isEmpty() || timeout.get().getLimit() <= 0) {
	    LOGGER.trace(formatLogMessage(COR_ID_TIMEOUTPREFIX, correlationId,
		    "Session " + session.getSpec().getId() + " will not be stopped automatically [NoTimout]."));
	    return false;
	}
	String strategyName = timeout.get().getStrategy();
	int limit = timeout.get().getLimit();
	Optional<TimeoutStrategy> strategy = timeoutStrategies.stream()
		.filter(registeredStrategy -> registeredStrategy.getName().equals(strategyName)).findAny();
	if (!strategy.isPresent()) {
	    LOGGER.warn(formatLogMessage(COR_ID_TIMEOUTPREFIX, correlationId, "No strategy configured."));
	}
	if (strategy.isPresent() && strategy.get().evaluate(COR_ID_TIMEOUTPREFIX, session, now, limit)) {
	    LOGGER.trace(formatLogMessage(COR_ID_TIMEOUTPREFIX, correlationId, "Session " + session.getSpec().getId()
		    + " was stopped after " + limit + " minutes [" + strategyName + "]."));
	    return true;
	} else {
	    LOGGER.trace(formatLogMessage(COR_ID_TIMEOUTPREFIX, correlationId, "Session " + session.getSpec().getId()
		    + " will keep running until the limit of " + limit + " is hit [" + strategyName + "]."));
	}
	return false;
    }
}
