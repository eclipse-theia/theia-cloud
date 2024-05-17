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
package org.eclipse.theia.cloud.defaultoperator;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;
import static org.eclipse.theia.cloud.common.util.LogMessageUtil.generateCorrelationId;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.Workspace;
import org.eclipse.theia.cloud.operator.TheiaCloudOperator;
import org.eclipse.theia.cloud.operator.TheiaCloudOperatorArguments;
import org.eclipse.theia.cloud.operator.handler.appdef.AppDefinitionHandler;
import org.eclipse.theia.cloud.operator.handler.session.SessionHandler;
import org.eclipse.theia.cloud.operator.handler.ws.WorkspaceHandler;
import org.eclipse.theia.cloud.operator.plugins.OperatorPlugin;
import org.eclipse.theia.cloud.operator.util.SpecWatch;

import com.google.inject.Inject;

import io.fabric8.kubernetes.client.Watcher;

public class DefaultTheiaCloudOperator implements TheiaCloudOperator {

    private static final ScheduledExecutorService STOP_EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final ScheduledExecutorService WATCH_EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    private static final Logger LOGGER = LogManager.getLogger(DefaultTheiaCloudOperator.class);

    private static final String COR_ID_APPDEFINITIONPREFIX = "appdefinition-watch-";
    private static final String COR_ID_WORKSPACEPREFIX = "workspace-watch-";
    private static final String COR_ID_SESSIONPREFIX = "session-watch-";
    private static final String COR_ID_TIMEOUTPREFIX = "timeout-";

    @Inject
    private TheiaCloudClient resourceClient;

    @Inject
    protected Set<OperatorPlugin> operatorPlugins;

    @Inject
    private AppDefinitionHandler appDefinitionAddedHandler;

    @Inject
    private WorkspaceHandler workspaceHandler;

    @Inject
    private SessionHandler sessionHandler;

    @Inject
    private TheiaCloudOperatorArguments arguments;

    private final Map<String, AppDefinition> appDefinitionCache = new ConcurrentHashMap<>();
    private final Map<String, Workspace> workspaceCache = new ConcurrentHashMap<>();
    private final Map<String, Session> sessionCache = new ConcurrentHashMap<>();
    private final Set<SpecWatch<?>> watches = new LinkedHashSet<>();

    @Override
    public void start() {
	this.operatorPlugins.forEach(plugin -> plugin.start());
	watches.add(initAppDefinitionsAndWatchForChanges());
	watches.add(initWorkspacesAndWatchForChanges());
	watches.add(initSessionsAndWatchForChanges());

	STOP_EXECUTOR.scheduleWithFixedDelay(this::stopTimedOutSessions, 1, 1, TimeUnit.MINUTES);
	WATCH_EXECUTOR.scheduleWithFixedDelay(this::lookForIdleWatches, 1, 1, TimeUnit.MINUTES);
    }

    protected SpecWatch<AppDefinition> initAppDefinitionsAndWatchForChanges() {
	try {
	    resourceClient.appDefinitions().list().forEach(this::initAppDefinition);
	    SpecWatch<AppDefinition> watcher = new SpecWatch<>(appDefinitionCache, this::handleAppDefnitionEvent,
		    "App Definition", COR_ID_APPDEFINITIONPREFIX);
	    resourceClient.appDefinitions().operation().watch(watcher);
	    return watcher;
	} catch (Exception e) {
	    LOGGER.error(formatLogMessage(DefaultTheiaCloudOperatorLauncher.COR_ID_INIT,
		    "Error while initializing app definitions watch"), e);
	    System.exit(-1);
	    throw new IllegalStateException();
	}
    }

    protected SpecWatch<Workspace> initWorkspacesAndWatchForChanges() {
	try {
	    resourceClient.workspaces().list().forEach(this::initWorkspace);
	    SpecWatch<Workspace> watcher = new SpecWatch<>(workspaceCache, this::handleWorkspaceEvent, "Workspace",
		    COR_ID_WORKSPACEPREFIX);
	    resourceClient.workspaces().operation().watch(watcher);
	    return watcher;
	} catch (Exception e) {
	    LOGGER.error(
		    formatLogMessage(DefaultTheiaCloudOperatorLauncher.COR_ID_INIT, "Error while initializing workspace watch"),
		    e);
	    System.exit(-1);
	    throw new IllegalStateException();
	}
    }

    protected SpecWatch<Session> initSessionsAndWatchForChanges() {
	try {
	    resourceClient.sessions().list().forEach(this::initSession);
	    SpecWatch<Session> watcher = new SpecWatch<>(sessionCache, this::handleSessionEvent, "Session",
		    COR_ID_SESSIONPREFIX);
	    resourceClient.sessions().operation().watch(watcher);
	    return watcher;
	} catch (Exception e) {
	    LOGGER.error(
		    formatLogMessage(DefaultTheiaCloudOperatorLauncher.COR_ID_INIT, "Error while initializing session watch"), e);
	    System.exit(-1);
	    throw new IllegalStateException();
	}
    }

    protected void initAppDefinition(AppDefinition resource) {
	appDefinitionCache.put(resource.getMetadata().getUid(), resource);
	String uid = resource.getMetadata().getUid();
	handleAppDefnitionEvent(Watcher.Action.ADDED, uid, DefaultTheiaCloudOperatorLauncher.COR_ID_INIT);
    }

    protected void initWorkspace(Workspace resource) {
	workspaceCache.put(resource.getMetadata().getUid(), resource);
	String uid = resource.getMetadata().getUid();
	handleWorkspaceEvent(Watcher.Action.ADDED, uid, DefaultTheiaCloudOperatorLauncher.COR_ID_INIT);
    }

    protected void initSession(Session resource) {
	sessionCache.put(resource.getMetadata().getUid(), resource);
	String uid = resource.getMetadata().getUid();
	handleSessionEvent(Watcher.Action.ADDED, uid, DefaultTheiaCloudOperatorLauncher.COR_ID_INIT);
    }

    protected void handleAppDefnitionEvent(Watcher.Action action, String uid, String correlationId) {
	try {
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
	} catch (Exception e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while handling app definitions"), e);
	}
    }

    protected void handleSessionEvent(Watcher.Action action, String uid, String correlationId) {
	try {
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
	} catch (Exception e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while handling sessions"), e);
	    if (!arguments.isContinueOnException()) {
		System.exit(-1);
	    }
	}
    }

    protected void handleWorkspaceEvent(Watcher.Action action, String uid, String correlationId) {
	try {
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
	} catch (Exception e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while handling workspaces"), e);
	    if (!arguments.isContinueOnException()) {
		System.exit(-1);
	    }
	}
    }

    protected void stopTimedOutSessions() {
	String correlationId = generateCorrelationId();

	try {
	    Set<String> timedOutSessions = new LinkedHashSet<>();
	    Instant now = Instant.now();
	    for (Session session : resourceClient.sessions().list()) {
		if (isSessionTimedOut(correlationId, now, session)) {
		    timedOutSessions.add(session.getSpec().getName());
		}
	    }

	    for (String sessionName : timedOutSessions) {
		resourceClient.sessions().delete(COR_ID_TIMEOUTPREFIX + correlationId, sessionName);
	    }
	} catch (Exception e) {
	    LOGGER.error(formatLogMessage(COR_ID_TIMEOUTPREFIX, correlationId, "Exception in kill after runnable"), e);
	    if (!arguments.isContinueOnException()) {
		System.exit(-1);
	    }
	}
    }

    /**
     * If watches have not been called at all (neither reconnecting calls or actual
     * actions), this might mean that the watch can't communicate with the kube API
     * anymore. In this case we want to hand over to a different operator which will
     * start up fresh watches.
     */
    protected void lookForIdleWatches() {
	String correlationId = generateCorrelationId();
	long now = System.currentTimeMillis();
	for (SpecWatch<?> watch : watches) {
	    long idleForMs = now - watch.getLastActive();
	    LOGGER.trace(formatLogMessage(COR_ID_TIMEOUTPREFIX, correlationId,
		    watch.getResourceName() + " watch was idle for " + idleForMs + " ms"));
	    if (idleForMs > arguments.getMaxWatchIdleTime()) {
		LOGGER.error(formatLogMessage(COR_ID_TIMEOUTPREFIX, correlationId, watch.getResourceName()
			+ " was idle for too long and is assumed to be disconnected. Exit operator.."));
		System.exit(-1);
	    }
	}
    }

    protected boolean isSessionTimedOut(String correlationId, Instant now, Session session) {
	Optional<Integer> timeout = resourceClient.appDefinitions().get(session.getSpec().getAppDefinition())
		.map(appDef -> appDef.getSpec().getTimeout());
	if (timeout.isEmpty() || timeout.get() <= 0) {
	    LOGGER.trace(formatLogMessage(COR_ID_TIMEOUTPREFIX, correlationId,
		    "Session " + session.getSpec().getName() + " will not be stopped automatically [NoTimeout]."));
	    return false;
	}
	int limit = timeout.get();
	String creationTimestamp = session.getMetadata().getCreationTimestamp();
	Instant parse = Instant.parse(creationTimestamp);
	long minutesSinceCreation = ChronoUnit.MINUTES.between(parse, now);
	LOGGER.trace(formatLogMessage(correlationId, "Checking " + session.getSpec().getName()
		+ ". minutesSinceLastActivity: " + minutesSinceCreation + ". limit: " + limit));
	if (minutesSinceCreation > limit) {
	    LOGGER.trace(formatLogMessage(COR_ID_TIMEOUTPREFIX, correlationId,
		    "Session " + session.getSpec().getName() + " was stopped after " + limit + " minutes."));
	    return true;
	} else {
	    LOGGER.trace(formatLogMessage(COR_ID_TIMEOUTPREFIX, correlationId, "Session " + session.getSpec().getName()
		    + " will keep running until the limit of " + limit + " is hit."));
	}
	return false;
    }
}
