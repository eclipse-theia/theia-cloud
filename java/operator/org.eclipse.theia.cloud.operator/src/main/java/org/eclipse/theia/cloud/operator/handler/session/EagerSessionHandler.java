/********************************************************************************
 * Copyright (C) 2022-2025 EclipseSource, Lockular, Ericsson, STMicroelectronics and 
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
package org.eclipse.theia.cloud.operator.handler.session;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionSpec;
import org.eclipse.theia.cloud.common.util.DataBridgeUtil;
import org.eclipse.theia.cloud.operator.databridge.AsyncDataInjector;
import org.eclipse.theia.cloud.operator.handler.AddedHandlerUtil;
import org.eclipse.theia.cloud.operator.ingress.IngressManager;
import org.eclipse.theia.cloud.operator.pool.PrewarmedResourcePool;
import org.eclipse.theia.cloud.common.tracing.Tracing;
import org.eclipse.theia.cloud.operator.util.SessionEnvCollector;
import org.eclipse.theia.cloud.operator.pool.PrewarmedResourcePool.PoolInstance;
import org.eclipse.theia.cloud.operator.pool.PrewarmedResourcePool.ReservationOutcome;
import org.eclipse.theia.cloud.operator.pool.PrewarmedResourcePool.ReservationResult;

import com.google.inject.Inject;

import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRoute;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.sentry.ISpan;
import io.sentry.Sentry;
import io.sentry.SpanStatus;

/**
 * A {@link SessionHandler} that uses prewarmed instances from the pool. This handler delegates to
 * {@link PrewarmedResourcePool} for instance management and {@link IngressManager} for ingress operations.
 */
public class EagerSessionHandler implements SessionHandler {

    private static final Logger LOGGER = LogManager.getLogger(EagerSessionHandler.class);

    public static final String SESSION_START_STRATEGY_ANNOTATION = "theia-cloud.io/session-start-strategy";
    public static final String SESSION_START_STRATEGY_EAGER = "eager";
    public static final String SESSION_INSTANCE_ID_ANNOTATION = "theia-cloud.io/instance-id";

    /**
     * Outcome of trying to handle a session with eager start.
     */
    public enum EagerSessionAddedOutcome {
        HANDLED, NO_CAPACITY, ERROR
    }

    @Inject
    private TheiaCloudClient client;

    @Inject
    private PrewarmedResourcePool pool;

    @Inject
    private IngressManager ingressManager;

    @Inject
    private SessionEnvCollector sessionEnvCollector;

    @Inject
    private AsyncDataInjector asyncDataInjector;

    @Override
    public boolean sessionAdded(Session session, String correlationId, ISpan span) {
        return trySessionAdded(session, correlationId, span) == EagerSessionAddedOutcome.HANDLED;
    }

    /**
     * Tries to handle a session using eager start. Returns the outcome so callers can fall back to lazy start if
     * needed.
     */
    public EagerSessionAddedOutcome trySessionAdded(Session session, String correlationId, ISpan parentSpan) {
        SessionSpec spec = session.getSpec();
        String sessionName = spec.getName();
        String appDefinitionID = spec.getAppDefinition();

        // Create child span from parent span
        ISpan span = Tracing.childSpan(parentSpan, "eager.setup", "Eager session setup");

        span.setData("session_name", sessionName);
        span.setData("app_definition", appDefinitionID);
        span.setData("user", spec.getUser());

        LOGGER.info(formatLogMessage(correlationId, "Handling sessionAdded " + spec));

        try {
            SessionStatusUtil.PreHandleResult preHandle = SessionStatusUtil.evaluateStatus(session, client,
                    correlationId, LOGGER);
            if (preHandle == SessionStatusUtil.PreHandleResult.ALREADY_HANDLED) {
                span.setTag("outcome", "already_handled");
                Tracing.finish(span, SpanStatus.OK);
                return EagerSessionAddedOutcome.HANDLED;
            }
            if (preHandle == SessionStatusUtil.PreHandleResult.INTERRUPTED
                    || preHandle == SessionStatusUtil.PreHandleResult.PREVIOUS_ERROR) {
                span.setTag("outcome", "previous_error");
                Tracing.finish(span, SpanStatus.ABORTED);
                return EagerSessionAddedOutcome.ERROR;
            }

            boolean markedHandling = true;
            try {
                SessionStatusUtil.markHandling(client, session, correlationId);
            } catch (RuntimeException ex) {
                LOGGER.warn(formatLogMessage(correlationId,
                        "Unable to mark session as HANDLING. Treating as interrupted/race."), ex);
                markedHandling = false;
            }
            // Check if the session was already handled by another instance
            if (!markedHandling) {
                Optional<Session> latestSession = client.sessions().get(session.getMetadata().getName());
                if (latestSession.isPresent()) {
                    SessionStatusUtil.PreHandleResult latestPreHandle = SessionStatusUtil.evaluateStatus(
                            latestSession.get(), client, correlationId, LOGGER);
                    if (latestPreHandle == SessionStatusUtil.PreHandleResult.ALREADY_HANDLED) {
                        span.setTag("outcome", "already_handled");
                        Tracing.finish(span, SpanStatus.OK);
                        return EagerSessionAddedOutcome.HANDLED;
                    }
                    if (latestPreHandle == SessionStatusUtil.PreHandleResult.INTERRUPTED
                            || latestPreHandle == SessionStatusUtil.PreHandleResult.PREVIOUS_ERROR) {
                        span.setTag("outcome", "interrupted");
                        Tracing.finish(span, SpanStatus.ABORTED);
                        return EagerSessionAddedOutcome.ERROR;
                    }
                }
                span.setTag("outcome", "interrupted");
                Tracing.finish(span, SpanStatus.ABORTED);
                return EagerSessionAddedOutcome.ERROR;
            }

            // Find app definition
            ISpan appDefSpan = Tracing.childSpan(span, "eager.find_appdef", "Find app definition");
            Optional<AppDefinition> appDefOpt = client.appDefinitions().get(appDefinitionID);
            if (appDefOpt.isEmpty()) {
                LOGGER.error(
                        formatLogMessage(correlationId, "No App Definition with name " + appDefinitionID + " found."));
                SessionStatusUtil.markError(client, session, correlationId, "App Definition not found.");
                appDefSpan.setTag("outcome", "not_found");
                Tracing.finish(appDefSpan, SpanStatus.NOT_FOUND);
                span.setTag("outcome", "error");
                Tracing.finish(span, SpanStatus.NOT_FOUND);
                return EagerSessionAddedOutcome.ERROR;
            }
            AppDefinition appDef = appDefOpt.get();
            Tracing.finishSuccess(appDefSpan);

            // Find HTTPRoute
            ISpan ingressSpan = Tracing.childSpan(span, "eager.find_route", "Find HTTPRoute");
            Optional<HTTPRoute> routeOpt = ingressManager.getIngress(appDef, correlationId);
            if (routeOpt.isEmpty()) {
                LOGGER.error(formatLogMessage(correlationId,
                        "No HTTPRoute for app definition " + appDefinitionID + " found."));
                SessionStatusUtil.markError(client, session, correlationId, "HTTPRoute not available.");
                ingressSpan.setTag("outcome", "not_found");
                Tracing.finish(ingressSpan, SpanStatus.NOT_FOUND);
                span.setTag("outcome", "error");
                Tracing.finish(span, SpanStatus.NOT_FOUND);
                return EagerSessionAddedOutcome.ERROR;
            }
            HTTPRoute route = routeOpt.get();
            Tracing.finishSuccess(ingressSpan);

            // Reserve an instance from the pool
            ISpan reserveSpan = Tracing.childSpan(span, "eager.reserve_instance", "Reserve pool instance");
            ReservationResult reservation = pool.reserveInstance(session, appDef, correlationId);
            reserveSpan.setTag("pool.outcome", reservation.getOutcome().name().toLowerCase());

            if (reservation.getOutcome() == ReservationOutcome.NO_CAPACITY) {
                reserveSpan.setTag("outcome", "no_capacity");
                Tracing.finish(reserveSpan, SpanStatus.RESOURCE_EXHAUSTED);
                span.setTag("outcome", "no_capacity");
                Tracing.finish(span, SpanStatus.RESOURCE_EXHAUSTED);
                return EagerSessionAddedOutcome.NO_CAPACITY;
            }
            if (reservation.getOutcome() == ReservationOutcome.ERROR) {
                SessionStatusUtil.markError(client, session, correlationId, "Failed to reserve prewarmed instance.");
                reserveSpan.setTag("outcome", "error");
                Tracing.finish(reserveSpan, SpanStatus.INTERNAL_ERROR);
                span.setTag("outcome", "error");
                Tracing.finish(span, SpanStatus.INTERNAL_ERROR);
                return EagerSessionAddedOutcome.ERROR;
            }

            PoolInstance instance = reservation.getInstance().get();
            reserveSpan.setData("instance_id", instance.getInstanceId());
            Tracing.finishSuccess(reserveSpan);

            // Annotate session with start strategy and instance ID
            ISpan annotateSpan = Tracing.childSpan(span, "eager.annotate_session", "Annotate session");
            annotateSession(session, correlationId, instance.getInstanceId());
            Tracing.finishSuccess(annotateSpan);

            // Complete session setup (labels, deployment ownership, email config)
            ISpan setupSpan = Tracing.childSpan(span, "eager.complete_setup", "Complete session setup");
            setupSpan.setData("instance_id", instance.getInstanceId());
            if (!pool.completeSessionSetup(session, appDef, instance, correlationId)) {
                SessionStatusUtil.markError(client, session, correlationId, "Failed to complete session setup.");
                setupSpan.setTag("outcome", "failure");
                Tracing.finish(setupSpan, SpanStatus.INTERNAL_ERROR);
                span.setTag("outcome", "error");
                Tracing.finish(span, SpanStatus.INTERNAL_ERROR);
                return EagerSessionAddedOutcome.ERROR;
            }
            Tracing.finishSuccess(setupSpan);

            // Schedule async credential injection via data bridge (tracked in separate transaction)
            if (DataBridgeUtil.isDataBridgeEnabled(appDef.getSpec())) {
                Map<String, String> envVars = sessionEnvCollector.collect(session, correlationId);
                if (!envVars.isEmpty()) {
                    Sentry.addBreadcrumb("Scheduling data bridge injection with " + envVars.size() + " env vars",
                            "databridge");
                    asyncDataInjector.scheduleInjection(span, session, envVars, correlationId);
                }
            }

            // Add HTTPRoute rule
            ISpan ingressRuleSpan = Tracing.childSpan(span, "eager.add_route_rule", "Add HTTPRoute rule");
            String host;
            try {
                host = ingressManager.addRuleForSession(route, instance.getExternalService(), appDef,
                        instance.getInstanceId(), correlationId);
                ingressRuleSpan.setData("host", host);
                Tracing.finishSuccess(ingressRuleSpan);
            } catch (KubernetesClientException e) {
                LOGGER.error(formatLogMessage(correlationId, "Error while editing HTTPRoute"), e);
                SessionStatusUtil.markError(client, session, correlationId, "Failed to edit HTTPRoute.");
                Tracing.finishError(ingressRuleSpan, e);
                span.setTag("outcome", "error");
                Tracing.finish(span, SpanStatus.INTERNAL_ERROR);
                return EagerSessionAddedOutcome.ERROR;
            }

            Sentry.addBreadcrumb("Scheduling URL availability check for " + host, "session");
            AddedHandlerUtil.updateSessionURLAsync(client.sessions(), session, client.namespace(), host, correlationId, span);

            span.setData("instance_id", instance.getInstanceId());
            SessionStatusUtil.markHandled(client, session, correlationId, null);
            Tracing.finishSuccess(span);
            return EagerSessionAddedOutcome.HANDLED;

        } catch (Exception e) {
            SessionStatusUtil.markError(client, session, correlationId,
                    "Unexpected error. Please check the logs for correlationId: " + correlationId);
            Tracing.finishError(span, e);
            throw e;
        }
    }

    @Override
    public boolean sessionDeleted(Session session, String correlationId, ISpan parentSpan) {
        SessionSpec spec = session.getSpec();
        String sessionName = spec.getName();
        String appDefinitionID = spec.getAppDefinition();

        ISpan span = Tracing.childSpan(parentSpan, "eager.cleanup", "Eager session cleanup");

        span.setData("session_name", sessionName);
        span.setData("app_definition", appDefinitionID);

        LOGGER.info(formatLogMessage(correlationId, "Handling sessionDeleted " + spec));

        try {
            // Find app definition
            ISpan appDefSpan = Tracing.childSpan(span, "eager.find_appdef", "Find app definition");
            Optional<AppDefinition> appDefOpt = client.appDefinitions().get(appDefinitionID);
            if (appDefOpt.isEmpty()) {
                LOGGER.info(formatLogMessage(correlationId,
                        "No App Definition found. Resources will be cleaned up by K8s garbage collection."));
                appDefSpan.setTag("outcome", "not_found");
                Tracing.finish(appDefSpan, SpanStatus.NOT_FOUND);
                Tracing.finishSuccess(span); // This is OK - K8s GC handles it
                return true;
            }
            AppDefinition appDef = appDefOpt.get();
            Tracing.finishSuccess(appDefSpan);

            // Find HTTPRoute
            ISpan ingressSpan = Tracing.childSpan(span, "eager.find_route", "Find HTTPRoute");
            Optional<HTTPRoute> routeOpt = ingressManager.getIngress(appDef, correlationId);
            if (routeOpt.isEmpty()) {
                LOGGER.error(formatLogMessage(correlationId,
                        "No HTTPRoute for app definition " + appDefinitionID + " found."));
                ingressSpan.setTag("outcome", "not_found");
                Tracing.finish(ingressSpan, SpanStatus.NOT_FOUND);
                span.setTag("outcome", "failure");
                Tracing.finish(span, SpanStatus.INTERNAL_ERROR);
                return false;
            }
            Tracing.finishSuccess(ingressSpan);

            // Get instance ID from session annotation
            Integer instanceId = getInstanceIdFromAnnotation(session);
            if (instanceId == null) {
                LOGGER.error(formatLogMessage(correlationId,
                        "Session missing instance-id annotation. Cannot determine which instance to release."));
                span.setTag("error.reason", "missing_instance_id_annotation");
                span.setTag("outcome", "failure");
                Tracing.finish(span, SpanStatus.INTERNAL_ERROR);
                return false;
            }
            span.setData("instance_id", instanceId);

            // Remove HTTPRoute rule
            ISpan removeIngressSpan = Tracing.childSpan(span, "eager.remove_route_rule", "Remove HTTPRoute rule");
            removeIngressSpan.setData("instance_id", instanceId);
            try {
                ingressManager.removeRulesForSession(routeOpt.get(), appDef, instanceId, correlationId);
                Tracing.finishSuccess(removeIngressSpan);
            } catch (KubernetesClientException e) {
                LOGGER.error(formatLogMessage(correlationId, "Error while removing HTTPRoute rule"), e);
                Tracing.finishError(removeIngressSpan, e);
                span.setTag("outcome", "failure");
                Tracing.finish(span, SpanStatus.INTERNAL_ERROR);
                return false;
            }

            // Release instance back to pool
            ISpan releaseSpan = Tracing.childSpan(span, "eager.release_instance", "Release pool instance");
            releaseSpan.setData("instance_id", instanceId);
            boolean success = pool.releaseInstance(session, appDef, correlationId);
            releaseSpan.setTag("outcome", success ? "success" : "failure");
            Tracing.finish(releaseSpan, success ? SpanStatus.OK : SpanStatus.INTERNAL_ERROR);

            // Reconcile the specific instance
            ISpan reconcileSpan = Tracing.childSpan(span, "eager.reconcile_instance", "Reconcile released instance");
            reconcileSpan.setData("instance_id", instanceId);
            pool.reconcileInstance(appDef, instanceId, correlationId);
            Tracing.finishSuccess(reconcileSpan);

            span.setTag("outcome", success ? "success" : "failure");
            Tracing.finish(span, success ? SpanStatus.OK : SpanStatus.INTERNAL_ERROR);
            return success;

        } catch (Exception e) {
            Tracing.finishError(span, e);
            throw e;
        }
    }

    private void annotateSession(Session session, String correlationId, int instanceId) {
        String name = session.getMetadata().getName();
        client.sessions().edit(correlationId, name, s -> {
            Map<String, String> annotations = s.getMetadata().getAnnotations();
            if (annotations == null) {
                annotations = new HashMap<>();
                s.getMetadata().setAnnotations(annotations);
            }
            annotations.put(SESSION_START_STRATEGY_ANNOTATION, SESSION_START_STRATEGY_EAGER);
            annotations.put(SESSION_INSTANCE_ID_ANNOTATION, String.valueOf(instanceId));
        });
    }

    private Integer getInstanceIdFromAnnotation(Session session) {
        Map<String, String> annotations = session.getMetadata().getAnnotations();
        if (annotations == null) {
            return null;
        }
        String idStr = annotations.get(SESSION_INSTANCE_ID_ANNOTATION);
        if (idStr == null) {
            return null;
        }
        try {
            return Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
