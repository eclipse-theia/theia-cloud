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
import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatMetric;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.Workspace;
import org.eclipse.theia.cloud.common.util.LabelsUtil;
import org.eclipse.theia.cloud.common.util.TheiaCloudError;
import org.eclipse.theia.cloud.common.util.WorkspaceUtil;
import org.eclipse.theia.cloud.operator.TheiaCloudOperatorArguments;
import org.eclipse.theia.cloud.operator.handler.AddedHandlerUtil;
import org.eclipse.theia.cloud.operator.ingress.IngressManager;
import org.eclipse.theia.cloud.operator.util.K8sResourceFactory;
import org.eclipse.theia.cloud.operator.util.K8sUtil;
import org.eclipse.theia.cloud.operator.util.TheiaCloudK8sUtil;
import org.eclipse.theia.cloud.common.tracing.Tracing;
import org.eclipse.theia.cloud.operator.util.TheiaCloudPersistentVolumeUtil;

import com.google.inject.Inject;

import io.sentry.ISpan;
import io.sentry.Sentry;
import io.sentry.SpanStatus;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRoute;
import io.fabric8.kubernetes.client.KubernetesClientException;

/**
 * A {@link SessionHandler} that creates resources on-demand (lazy start).
 * 
 * Uses {@link K8sResourceFactory} for resource creation and
 * {@link IngressManager} for ingress operations.
 */
public class LazySessionHandler implements SessionHandler {

    private static final Logger LOGGER = LogManager.getLogger(LazySessionHandler.class);
    protected static final String USER_DATA = "user-data";

    @Inject
    protected TheiaCloudOperatorArguments arguments;

    @Inject
    protected TheiaCloudClient client;

    @Inject
    protected K8sResourceFactory resourceFactory;

    @Inject
    protected IngressManager ingressManager;

    @Override
    public boolean sessionAdded(Session session, String correlationId, ISpan parentSpan) {
        ISpan span = Tracing.childSpan(parentSpan, "lazy.setup", "Lazy session setup");
        try {
            return doSessionAdded(session, correlationId, span);
        } catch (Throwable ex) {
            LOGGER.error(formatLogMessage(correlationId,
                    "An unexpected exception occurred while adding Session: " + session), ex);
            Tracing.finishError(span, ex);
            SessionStatusUtil.markError(client, session, correlationId,
                    "Unexpected error. Please check the logs for correlationId: " + correlationId);
            return false;
        }
    }

    protected boolean doSessionAdded(Session session, String correlationId, ISpan span) {
        // Session information
        String sessionResourceName = session.getMetadata().getName();
        String sessionResourceUID = session.getMetadata().getUid();

        // Use provided span
        span.setTag("session.name", sessionResourceName);
        span.setTag("session.strategy", "lazy");
        span.setData("correlation_id", correlationId);

        // Check current session status and ignore if handling failed or finished before
        SessionStatusUtil.PreHandleResult preHandle = SessionStatusUtil.evaluateStatus(session, client, correlationId,
                LOGGER);
        if (preHandle == SessionStatusUtil.PreHandleResult.ALREADY_HANDLED) {
            span.setTag("outcome", "already_handled");
            Tracing.finish(span, SpanStatus.OK);
            return true;
        }
        if (preHandle == SessionStatusUtil.PreHandleResult.INTERRUPTED) {
            span.setTag("outcome", "interrupted");
            Tracing.finish(span, SpanStatus.ABORTED);
            return false;
        }
        if (preHandle == SessionStatusUtil.PreHandleResult.PREVIOUS_ERROR) {
            span.setTag("outcome", "previous_error");
            Tracing.finish(span, SpanStatus.ABORTED);
            return false;
        }

        // Set status to handling
        SessionStatusUtil.markHandling(client, session, correlationId);

        SessionSpec sessionSpec = session.getSpec();

        // Find app definition
        ISpan appDefSpan = Tracing.childSpan(span, "lazy.find_appdef", "Find app definition");
        String appDefinitionID = sessionSpec.getAppDefinition();
        span.setTag("app_definition", appDefinitionID);
        Optional<AppDefinition> appDefOpt = client.appDefinitions().get(appDefinitionID);
        if (appDefOpt.isEmpty()) {
            LOGGER.error(formatLogMessage(correlationId,
                    "No App Definition with name " + appDefinitionID + " found."));
            SessionStatusUtil.markError(client, session, correlationId, "App Definition not found.");
            appDefSpan.setTag("outcome", "not_found");
            Tracing.finish(appDefSpan, SpanStatus.NOT_FOUND);
            span.setTag("outcome", "appdef_not_found");
            Tracing.finish(span, SpanStatus.NOT_FOUND);
            return false;
        }
        AppDefinition appDef = appDefOpt.get();
        AppDefinitionSpec appDefSpec = appDef.getSpec();
        Tracing.finishSuccess(appDefSpan);

        // Create labels
        Map<String, String> labels = LabelsUtil.createSessionLabels(session, appDef);

        // Check limits
        ISpan limitsSpan = Tracing.childSpan(span, "lazy.check_limits", "Check instance and session limits");
        if (hasMaxInstancesReached(appDef, session, correlationId)) {
            SessionStatusUtil.markError(client, session, correlationId, "Max instances reached.");
            limitsSpan.setTag("outcome", "max_instances");
            Tracing.finish(limitsSpan, SpanStatus.RESOURCE_EXHAUSTED);
            span.setTag("outcome", "max_instances_reached");
            Tracing.finish(span, SpanStatus.RESOURCE_EXHAUSTED);
            return false;
        }
        if (hasMaxSessionsReached(session, correlationId)) {
            SessionStatusUtil.markError(client, session, correlationId, "Max sessions reached.");
            limitsSpan.setTag("outcome", "max_sessions");
            Tracing.finish(limitsSpan, SpanStatus.RESOURCE_EXHAUSTED);
            span.setTag("outcome", "max_sessions_reached");
            Tracing.finish(span, SpanStatus.RESOURCE_EXHAUSTED);
            return false;
        }
        Tracing.finishSuccess(limitsSpan);

        // Get HTTPRoute
        ISpan ingressSpan = Tracing.childSpan(span, "lazy.get_route", "Get HTTPRoute for app definition");
        Optional<HTTPRoute> routeOpt = ingressManager.getIngress(appDef, correlationId);
        if (routeOpt.isEmpty()) {
            SessionStatusUtil.markError(client, session, correlationId, "HTTPRoute not available.");
            ingressSpan.setTag("outcome", "not_found");
            Tracing.finish(ingressSpan, SpanStatus.NOT_FOUND);
            span.setTag("outcome", "route_not_found");
            Tracing.finish(span, SpanStatus.NOT_FOUND);
            return false;
        }
        Tracing.finishSuccess(ingressSpan);

        syncSessionDataToWorkspace(session, correlationId);

        // Check for existing service (idempotency)
        List<Service> existingServices = K8sUtil.getExistingServices(
                client.kubernetes(), client.namespace(), sessionResourceName, sessionResourceUID);
        if (!existingServices.isEmpty()) {
            LOGGER.warn(formatLogMessage(correlationId, "Service already exists for session."));
            SessionStatusUtil.markHandled(client, session, correlationId, "Service already exists.");
            span.setTag("outcome", "idempotent_service_exists");
            Tracing.finish(span, SpanStatus.OK);
            return true;
        }

        // Create service
        ISpan serviceSpan = Tracing.childSpan(span, "lazy.create_service", "Create service");
        Optional<Service> serviceOpt = resourceFactory.createServiceForLazySession(
                session, appDef, labels, correlationId);
        if (serviceOpt.isEmpty()) {
            LOGGER.error(formatLogMessage(correlationId, "Unable to create service for session."));
            SessionStatusUtil.markError(client, session, correlationId, "Failed to create service.");
            serviceSpan.setTag("outcome", "failed");
            Tracing.finish(serviceSpan, SpanStatus.INTERNAL_ERROR);
            span.setTag("outcome", "service_creation_failed");
            Tracing.finish(span, SpanStatus.INTERNAL_ERROR);
            return false;
        }
        Tracing.finishSuccess(serviceSpan);

        // Create internal service
        ISpan internalServiceSpan = Tracing.childSpan(span, "lazy.create_internal_service", "Create internal service");
        Optional<Service> internalServiceOpt = resourceFactory.createInternalServiceForLazySession(
                session, appDef, labels, correlationId);
        if (internalServiceOpt.isEmpty()) {
            LOGGER.error(formatLogMessage(correlationId, "Unable to create internal service."));
            SessionStatusUtil.markError(client, session, correlationId, "Failed to create internal service.");
            internalServiceSpan.setTag("outcome", "failed");
            Tracing.finish(internalServiceSpan, SpanStatus.INTERNAL_ERROR);
            span.setTag("outcome", "internal_service_failed");
            Tracing.finish(span, SpanStatus.INTERNAL_ERROR);
            return false;
        }
        Tracing.finishSuccess(internalServiceSpan);

        // Create configmaps (if using Keycloak)
        if (arguments.isUseKeycloak()) {
            ISpan configMapSpan = Tracing.childSpan(span, "lazy.create_configmaps", "Create OAuth2 configmaps");
            List<ConfigMap> existingConfigMaps = K8sUtil.getExistingConfigMaps(
                    client.kubernetes(), client.namespace(), sessionResourceName, sessionResourceUID);
            if (!existingConfigMaps.isEmpty()) {
                LOGGER.warn(formatLogMessage(correlationId, "ConfigMaps already exist for session."));
                SessionStatusUtil.markHandled(client, session, correlationId, "ConfigMaps already exist.");
                configMapSpan.setTag("outcome", "already_exists");
                Tracing.finish(configMapSpan, SpanStatus.OK);
                span.setTag("outcome", "idempotent_configmaps_exist");
                Tracing.finish(span, SpanStatus.OK);
                return true;
            }
            resourceFactory.createEmailConfigMapForLazySession(session, labels, correlationId);
            resourceFactory.createProxyConfigMapForLazySession(session, appDef, labels, correlationId);
            Tracing.finishSuccess(configMapSpan);
        }

        // Check for existing deployment (idempotency)
        List<Deployment> existingDeployments = K8sUtil.getExistingDeployments(
                client.kubernetes(), client.namespace(), sessionResourceName, sessionResourceUID);
        if (!existingDeployments.isEmpty()) {
            LOGGER.warn(formatLogMessage(correlationId, "Deployment already exists for session."));
            SessionStatusUtil.markHandled(client, session, correlationId, "Deployment already exists.");
            span.setTag("outcome", "idempotent_deployment_exists");
            Tracing.finish(span, SpanStatus.OK);
            return true;
        }

        // Create deployment
        ISpan deploymentSpan = Tracing.childSpan(span, "lazy.create_deployment", "Create deployment");
        Optional<String> storageName = getStorageName(session, correlationId);
        deploymentSpan.setData("has_storage", storageName.isPresent());
        resourceFactory.createDeploymentForLazySession(
                session, appDef, storageName, labels,
                deployment -> storageName.ifPresent(name -> addVolumeClaim(deployment, name, appDefSpec)),
                correlationId);
        Tracing.finishSuccess(deploymentSpan);

        // Add HTTPRoute rule
        ISpan ingressRuleSpan = Tracing.childSpan(span, "lazy.add_route_rule", "Add HTTPRoute rule");
        String host;
        try {
            host = ingressManager.addRuleForSession(
                    routeOpt.get(), serviceOpt.get(), appDef, session, correlationId);
            ingressRuleSpan.setData("host", host);
            Tracing.finishSuccess(ingressRuleSpan);
        } catch (KubernetesClientException e) {
            LOGGER.error(formatLogMessage(correlationId, "Error while editing HTTPRoute"), e);
            SessionStatusUtil.markError(client, session, correlationId, "Failed to edit HTTPRoute.");
            Tracing.finishError(ingressRuleSpan, e);
            span.setTag("outcome", "route_rule_failed");
            Tracing.finish(span, SpanStatus.INTERNAL_ERROR);
            return false;
        }

        Sentry.addBreadcrumb("Scheduling URL availability check for " + host, "session");
        AddedHandlerUtil.updateSessionURLAsync(client.sessions(), session, client.namespace(), host, correlationId,
                span);

        SessionStatusUtil.markHandled(client, session, correlationId, null);
        span.setTag("outcome", "success");
        Tracing.finishSuccess(span);
        return true;
    }

    @Override
    public synchronized boolean sessionDeleted(Session session, String correlationId, ISpan parentSpan) {
        return doSessionDeleted(session, correlationId, parentSpan);
    }

    protected boolean doSessionDeleted(Session session, String correlationId, ISpan span) {
        SessionSpec sessionSpec = session.getSpec();
        String sessionName = session.getMetadata().getName();
        String appDefinitionID = sessionSpec.getAppDefinition();

        // Use provided span
        span.setTag("session.name", sessionName);
        span.setTag("session.strategy", "lazy");
        span.setTag("app_definition", appDefinitionID);
        span.setData("correlation_id", correlationId);

        ISpan appDefSpan = Tracing.childSpan(span, "lazy.find_appdef", "Find app definition");
        Optional<AppDefinition> appDefOpt = client.appDefinitions().get(appDefinitionID);
        if (appDefOpt.isEmpty()) {
            LOGGER.error(formatLogMessage(correlationId,
                    "No App Definition found. Cannot clean up ingress for session " + sessionSpec.getName()));
            appDefSpan.setTag("outcome", "not_found");
            Tracing.finish(appDefSpan, SpanStatus.NOT_FOUND);
            span.setTag("outcome", "appdef_not_found");
            span.setStatus(SpanStatus.NOT_FOUND);
            return false;
        }
        AppDefinition appDef = appDefOpt.get();
        Tracing.finishSuccess(appDefSpan);

        ISpan ingressSpan = Tracing.childSpan(span, "lazy.get_route", "Get HTTPRoute for cleanup");
        Optional<HTTPRoute> routeOpt = ingressManager.getIngress(appDef, correlationId);
        if (routeOpt.isEmpty()) {
            LOGGER.error(formatLogMessage(correlationId, "No HTTPRoute found for app definition."));
            ingressSpan.setTag("outcome", "not_found");
            Tracing.finish(ingressSpan, SpanStatus.NOT_FOUND);
            span.setTag("outcome", "route_not_found");
            span.setStatus(SpanStatus.NOT_FOUND);
            return false;
        }
        Tracing.finishSuccess(ingressSpan);

        // Remove HTTPRoute rules
        ISpan removeRulesSpan = Tracing.childSpan(span, "lazy.remove_route_rules", "Remove HTTPRoute rules");
        try {
            ingressManager.removeRulesForSession(routeOpt.get(), appDef, session, correlationId);
            Tracing.finishSuccess(removeRulesSpan);
            LOGGER.info(formatLogMessage(correlationId, "Successfully cleaned up HTTPRoute rules for session"));
            span.setTag("outcome", "success");
            return true;
        } catch (KubernetesClientException e) {
            LOGGER.error(formatLogMessage(correlationId, "Failed to remove HTTPRoute rules for session"), e);
            removeRulesSpan.setTag("outcome", "failed");
            Tracing.finishError(removeRulesSpan, e);
            span.setTag("outcome", "remove_rules_failed");
            return false;
        }
    }

    // ========== Helper Methods ==========

    protected void syncSessionDataToWorkspace(Session session, String correlationId) {
        if (!session.getSpec().isEphemeral() && session.getSpec().hasAppDefinition()) {
            client.workspaces().edit(correlationId, session.getSpec().getWorkspace(), workspace -> {
                workspace.getSpec().setAppDefinition(session.getSpec().getAppDefinition());
            });
        }
    }

    protected boolean hasMaxInstancesReached(AppDefinition appDef, Session session, String correlationId) {
        if (TheiaCloudK8sUtil.checkIfMaxInstancesReached(
                client.kubernetes(), client.namespace(), session.getSpec(), appDef.getSpec(), correlationId)) {
            LOGGER.info(formatMetric(correlationId, "Max instances reached for " + appDef.getSpec().getName()));
            client.sessions().updateStatus(correlationId, session, status -> {
                status.setError(TheiaCloudError.SESSION_SERVER_LIMIT_REACHED);
            });
            return true;
        }
        return false;
    }

    protected boolean hasMaxSessionsReached(Session session, String correlationId) {
        if (arguments.getSessionsPerUser() == null || arguments.getSessionsPerUser() < 0) {
            return false;
        }
        if (arguments.getSessionsPerUser() == 0) {
            LOGGER.info(formatLogMessage(correlationId, "No sessions allowed for this user."));
            client.sessions().updateStatus(correlationId, session, status -> {
                status.setError(TheiaCloudError.SESSION_USER_NO_SESSIONS);
            });
            return true;
        }
        long userSessions = client.sessions().list(session.getSpec().getUser()).size();
        if (userSessions > arguments.getSessionsPerUser()) {
            LOGGER.info(formatLogMessage(correlationId,
                    "Session limit reached for user: " + arguments.getSessionsPerUser()));
            client.sessions().updateStatus(correlationId, session, status -> {
                status.setError(TheiaCloudError.SESSION_USER_LIMIT_REACHED);
            });
            return true;
        }
        return false;
    }

    protected Optional<String> getStorageName(Session session, String correlationId) {
        if (session.getSpec().isEphemeral()) {
            return Optional.empty();
        }
        Optional<Workspace> workspace = client.workspaces().get(session.getSpec().getWorkspace());
        if (workspace.isEmpty()) {
            LOGGER.info(formatLogMessage(correlationId,
                    "No workspace with name " + session.getSpec().getWorkspace() + " found for session "
                            + session.getSpec().getName()));
            return Optional.empty();
        }
        if (!session.getSpec().getUser().equals(workspace.get().getSpec().getUser())) {
            // the workspace is owned by a different user. do not mount and go ephemeral
            // should get prevented by service, but we need to be sure to not expose data
            LOGGER.error(formatLogMessage(correlationId, "Workspace is owned by " + workspace.get().getSpec().getUser()
                    + ", but requesting user is " + session.getSpec().getUser()));
            return Optional.empty();
        }
        String storageName = WorkspaceUtil.getStorageName(workspace.get());
        if (!client.persistentVolumeClaimsClient().has(storageName)) {
            LOGGER.info(formatLogMessage(correlationId, "No storage found. Using ephemeral storage."));
            return Optional.empty();
        }
        return Optional.of(storageName);
    }

    protected void addVolumeClaim(Deployment deployment, String pvcName, AppDefinitionSpec appDef) {
        PodSpec podSpec = deployment.getSpec().getTemplate().getSpec();

        Volume volume = new Volume();
        podSpec.getVolumes().add(volume);
        volume.setName(USER_DATA);
        PersistentVolumeClaimVolumeSource pvc = new PersistentVolumeClaimVolumeSource();
        volume.setPersistentVolumeClaim(pvc);
        pvc.setClaimName(pvcName);

        Container theiaContainer = TheiaCloudPersistentVolumeUtil.getTheiaContainer(podSpec, appDef);

        VolumeMount volumeMount = new VolumeMount();
        theiaContainer.getVolumeMounts().add(volumeMount);
        volumeMount.setName(USER_DATA);
        volumeMount.setMountPath(TheiaCloudPersistentVolumeUtil.getMountPath(appDef));
    }
}
