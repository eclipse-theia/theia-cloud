/********************************************************************************
 * Copyright (C) 2023 EclipseSource and others.
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
import java.time.temporal.ChronoUnit;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.Workspace;
import org.eclipse.theia.cloud.operator.handler.appdef.AppDefinitionHandler;
import org.eclipse.theia.cloud.operator.handler.session.SessionHandler;
import org.eclipse.theia.cloud.operator.handler.ws.WorkspaceHandler;
import org.eclipse.theia.cloud.operator.plugins.OperatorPlugin;
import org.eclipse.theia.cloud.operator.util.SpecWatch;

import com.google.inject.Inject;

import io.fabric8.kubernetes.client.Watcher;

public class BasicTheiaCloudOperator implements TheiaCloudOperator {

    private static final ScheduledExecutorService STOP_EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final ScheduledExecutorService WATCH_EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final int SESSION_EVENT_QUEUE_SIZE = 1000;

    private static final Logger LOGGER = LogManager.getLogger(BasicTheiaCloudOperator.class);

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

    // Session event scheduling: per-session serialization, cross-session parallelism.
    private ExecutorService sessionExecutor;
    private final Map<String, Deque<SessionEvent>> sessionEventQueues = new ConcurrentHashMap<>();
    private final Map<String, AtomicBoolean> sessionEventRunning = new ConcurrentHashMap<>();

    @Override
    public void start() {
        this.operatorPlugins.forEach(plugin -> plugin.start());
        initSessionExecutor();
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
            LOGGER.error(formatLogMessage(TheiaCloudOperatorLauncher.COR_ID_INIT,
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
            LOGGER.error(formatLogMessage(TheiaCloudOperatorLauncher.COR_ID_INIT,
                    "Error while initializing workspace watch"), e);
            System.exit(-1);
            throw new IllegalStateException();
        }
    }

    protected SpecWatch<Session> initSessionsAndWatchForChanges() {
        try {
            resourceClient.sessions().list().forEach(this::initSession);
            SpecWatch<Session> watcher = new SpecWatch<>(sessionCache, this::scheduleSessionEvent, "Session",
                    COR_ID_SESSIONPREFIX);
            resourceClient.sessions().operation().watch(watcher);
            return watcher;
        } catch (Exception e) {
            LOGGER.error(
                    formatLogMessage(TheiaCloudOperatorLauncher.COR_ID_INIT, "Error while initializing session watch"),
                    e);
            System.exit(-1);
            throw new IllegalStateException();
        }
    }

    protected void initAppDefinition(AppDefinition resource) {
        appDefinitionCache.put(resource.getMetadata().getUid(), resource);
        String uid = resource.getMetadata().getUid();
        handleAppDefnitionEvent(Watcher.Action.ADDED, uid, TheiaCloudOperatorLauncher.COR_ID_INIT);
    }

    protected void initWorkspace(Workspace resource) {
        workspaceCache.put(resource.getMetadata().getUid(), resource);
        String uid = resource.getMetadata().getUid();
        handleWorkspaceEvent(Watcher.Action.ADDED, uid, TheiaCloudOperatorLauncher.COR_ID_INIT);
    }

    protected void initSession(Session resource) {
        sessionCache.put(resource.getMetadata().getUid(), resource);
        String uid = resource.getMetadata().getUid();
        scheduleSessionEvent(Watcher.Action.ADDED, uid, TheiaCloudOperatorLauncher.COR_ID_INIT);
    }

    protected void scheduleSessionEvent(Watcher.Action action, String uid, String correlationId) {
        Session session = sessionCache.get(uid);
        sessionEventQueues.computeIfAbsent(uid, k -> new ConcurrentLinkedDeque<>())
                .add(new SessionEvent(action, correlationId, session));

        // Ensure only one runner per session UID.
        AtomicBoolean running = sessionEventRunning.computeIfAbsent(uid, k -> new AtomicBoolean(false));
        if (running.compareAndSet(false, true)) {
            sessionExecutor.execute(() -> drainSessionEvents(uid));
        }
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

    private void handleSessionEvent(SessionEvent event) {
        if (event.session == null) {
            return;
        }
        try {
            switch (event.action) {
            case ADDED:
                sessionHandler.sessionAdded(event.session, event.correlationId);
                break;
            case DELETED:
                sessionHandler.sessionDeleted(event.session, event.correlationId);
                break;
            case MODIFIED:
                sessionHandler.sessionModified(event.session, event.correlationId);
                break;
            case ERROR:
                sessionHandler.sessionErrored(event.session, event.correlationId);
                break;
            case BOOKMARK:
                sessionHandler.sessionBookmarked(event.session, event.correlationId);
                break;
            }
        } catch (Exception e) {
            LOGGER.error(formatLogMessage(event.correlationId, "Error while handling sessions"), e);
            if (!arguments.isContinueOnException()) {
                System.exit(-1);
            }
        }
    }

    private void initSessionExecutor() {
        int threads = arguments.getSessionHandlerThreads();
        sessionExecutor = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(SESSION_EVENT_QUEUE_SIZE), new ThreadPoolExecutor.CallerRunsPolicy());
    }

    private void drainSessionEvents(String uid) {
        try {
            while (true) {
                SessionEvent event = pollSessionEvent(uid);
                if (event == null) {
                    break;
                }
                handleSessionEvent(event);
            }
        } finally {
            AtomicBoolean running = sessionEventRunning.get(uid);
            if (running != null) {
                running.set(false);
            }
            // Handle race: an event can be enqueued after we observe "empty" but before running=false.
            // In that case, the enqueuer sees running=true and won't schedule a drainer, so we must
            // recheck after flipping the flag and reschedule if anything is pending.
            if (hasPendingSessionEvents(uid)) {
                AtomicBoolean retryRunning = sessionEventRunning.computeIfAbsent(uid, k -> new AtomicBoolean(false));
                if (retryRunning.compareAndSet(false, true)) {
                    sessionExecutor.execute(() -> drainSessionEvents(uid));
                }
            } else {
                // Cleanup maps for idle sessions.
                sessionEventQueues.remove(uid);
                sessionEventRunning.remove(uid);
            }
        }
    }

    private SessionEvent pollSessionEvent(String uid) {
        Deque<SessionEvent> queue = sessionEventQueues.get(uid);
        if (queue != null) {
            return queue.poll();
        }
        return null;
    }

    private boolean hasPendingSessionEvents(String uid) {
        Deque<SessionEvent> queue = sessionEventQueues.get(uid);
        return queue != null && !queue.isEmpty();
    }

    private static final class SessionEvent {
        private final Watcher.Action action;
        private final String correlationId;
        private final Session session;

        private SessionEvent(Watcher.Action action, String correlationId, Session session) {
            this.action = action;
            this.correlationId = correlationId;
            this.session = session;
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
     * If watches have not been called at all (neither reconnecting calls or actual actions), this might mean that the
     * watch can't communicate with the kube API anymore. In this case we want to hand over to a different operator
     * which will start up fresh watches.
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
