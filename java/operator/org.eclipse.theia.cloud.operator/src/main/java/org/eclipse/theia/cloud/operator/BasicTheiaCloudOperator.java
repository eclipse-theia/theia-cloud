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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.Workspace;
import org.eclipse.theia.cloud.common.tracing.TraceContext;
import org.eclipse.theia.cloud.common.tracing.Tracing;
import org.eclipse.theia.cloud.operator.handler.appdef.AppDefinitionHandler;
import org.eclipse.theia.cloud.operator.handler.session.SessionHandler;
import org.eclipse.theia.cloud.operator.handler.ws.WorkspaceHandler;
import org.eclipse.theia.cloud.operator.plugins.OperatorPlugin;
import org.eclipse.theia.cloud.operator.util.SpecWatch;

import io.sentry.ISpan;
import io.sentry.Sentry;

import com.google.inject.Inject;

import io.fabric8.kubernetes.client.Watcher;

public class BasicTheiaCloudOperator implements TheiaCloudOperator {

    private static final ScheduledExecutorService STOP_EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final ScheduledExecutorService WATCH_EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final int SESSION_EVENT_QUEUE_SIZE = 1000;
    private static final long SESSION_EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS = 30L;

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

    // Session event scheduling: per-session serialization, cross-session
    // parallelism.
    private ExecutorService sessionExecutor;
    private final Map<String, SessionState> sessionStates = new ConcurrentHashMap<>();

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

    @Override
    public void stop() {
        if (sessionExecutor == null) {
            return;
        }
        sessionExecutor.shutdown();
        try {
            if (!sessionExecutor.awaitTermination(SESSION_EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                sessionExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            sessionExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
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
        SessionState state = sessionStates.computeIfAbsent(uid, k -> new SessionState());
        boolean shouldSchedule = false;
        synchronized (state.lock) {
            state.queue.add(new SessionEvent(action, correlationId, session));
            // Ensure only one runner per session UID.
            if (!state.running) {
                state.running = true;
                shouldSchedule = true;
            }
        }
        if (shouldSchedule) {
            sessionExecutor.execute(() -> drainSessionEvents(uid, state));
        }
    }

    protected void handleAppDefnitionEvent(Watcher.Action action, String uid, String correlationId) {
        try {
            AppDefinition appDefinition = appDefinitionCache.get(uid);
            switch (action) {
                case ADDED -> appDefinitionAddedHandler.appDefinitionAdded(appDefinition, correlationId);
                case DELETED -> appDefinitionAddedHandler.appDefinitionDeleted(appDefinition, correlationId);
                case MODIFIED -> appDefinitionAddedHandler.appDefinitionModified(appDefinition, correlationId);
                case ERROR -> appDefinitionAddedHandler.appDefinitionErrored(appDefinition, correlationId);
                case BOOKMARK -> appDefinitionAddedHandler.appDefinitionBookmarked(appDefinition, correlationId);
            }
        } catch (Exception e) {
            LOGGER.error(formatLogMessage(correlationId, "Error while handling app definitions"), e);
        }
    }

    protected void handleSessionEvent(Watcher.Action action, String uid, String correlationId) {
        Session session = sessionCache.get(uid);
        if (session == null) {
            return;
        }

        // Extract trace context from Session annotations (if propagated from service)
        Optional<TraceContext> traceContext = TraceContext.fromMetadata(session.getMetadata());

        ISpan span = null;
        try {
            String name = "session." + action.name().toLowerCase();
            String operation = action.name() + " session " + session.getSpec().getName();
            span = Tracing.continueTraceAsync(traceContext, name, operation);
            span.setTag("session.name", session.getSpec().getName());
            span.setTag("app_definition", session.getSpec().getAppDefinition());
            span.setTag("user", session.getSpec().getUser());

            span.setTag("action", action.name());
            span.setData("correlation_id", correlationId);

            switch (action) {
                case ADDED -> sessionHandler.sessionAdded(session, correlationId, span);
                case DELETED -> sessionHandler.sessionDeleted(session, correlationId, span);
                case MODIFIED -> sessionHandler.sessionModified(session, correlationId, span);
                case ERROR -> sessionHandler.sessionErrored(session, correlationId, span);
                case BOOKMARK -> sessionHandler.sessionBookmarked(session, correlationId, span);
            }

            Tracing.finishSuccess(span);
        } catch (Exception e) {
            LOGGER.error(formatLogMessage(correlationId, "Error while handling sessions"), e);
            if (span != null) {
                Tracing.finishError(span, e);
            }
            if (!arguments.isContinueOnException()) {
                System.exit(-1);
            }
        }
    }

    private void handleSessionEvent(SessionEvent event) {
        if (event.session == null) {
            throw new IllegalArgumentException("Session must not be null");
        }
        // Extract trace context from Session annotations (if propagated from service)
        Optional<TraceContext> traceContext = TraceContext.fromMetadata(event.session.getMetadata());

        ISpan span = null;
        try {
            String name = "session." + event.action.name().toLowerCase();
            String operation = event.action.name() + " session " + event.session.getSpec().getName();
            span = Tracing.continueTraceAsync(traceContext, name, operation);
            span.setTag("session.name", event.session.getSpec().getName());
            span.setTag("app_definition", event.session.getSpec().getAppDefinition());
            span.setTag("user", event.session.getSpec().getUser());

            span.setTag("action", event.action.name());
            span.setData("correlation_id", event.correlationId);

            switch (event.action) {
                case ADDED -> sessionHandler.sessionAdded(event.session, event.correlationId, span);
                case DELETED -> sessionHandler.sessionDeleted(event.session, event.correlationId, span);
                case MODIFIED -> sessionHandler.sessionModified(event.session, event.correlationId, span);
                case ERROR -> sessionHandler.sessionErrored(event.session, event.correlationId, span);
                case BOOKMARK -> sessionHandler.sessionBookmarked(event.session, event.correlationId, span);
            }
            Tracing.finishSuccess(span);
        } catch (Exception e) {
            LOGGER.error(formatLogMessage(event.correlationId, "Error while handling sessions"), e);
            if (span != null) {
                Tracing.finishError(span, e);
            }
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

    private void drainSessionEvents(String uid, SessionState state) {
        while (true) {
            SessionEvent event;
            synchronized (state.lock) {
                event = state.queue.poll();
                if (event == null) {
                    state.running = false;
                    break;
                }
            }
            try {
                handleSessionEvent(event);
            } catch (IllegalArgumentException e) {
                LOGGER.error(formatLogMessage(event.correlationId, "Error while handling sessions"), e);
                Sentry.captureException(e);
            }
        }
        tryCleanupSessionState(uid, state);
    }

    private void tryCleanupSessionState(String uid, SessionState state) {
        SessionState currentState = sessionStates.get(uid);
        if (currentState == null) {
            return;
        }
        synchronized (currentState.lock) {
            if (currentState != state) {
                return;
            }
            if (!currentState.queue.isEmpty() || currentState.running) {
                return;
            }
            sessionStates.remove(uid, currentState);
        }
    }

    private static final class SessionState {
        private final Deque<SessionEvent> queue = new ArrayDeque<>();
        private final Object lock = new Object();
        private boolean running;
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
                case ADDED -> workspaceHandler.workspaceAdded(workspace, correlationId);
                case DELETED -> workspaceHandler.workspaceDeleted(workspace, correlationId);
                case MODIFIED -> workspaceHandler.workspaceModified(workspace, correlationId);
                case ERROR -> workspaceHandler.workspaceErrored(workspace, correlationId);
                case BOOKMARK -> workspaceHandler.workspaceBookmarked(workspace, correlationId);
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
     * actions), this might mean that the
     * watch can't communicate with the kube API anymore. In this case we want to
     * hand over to a different operator
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
