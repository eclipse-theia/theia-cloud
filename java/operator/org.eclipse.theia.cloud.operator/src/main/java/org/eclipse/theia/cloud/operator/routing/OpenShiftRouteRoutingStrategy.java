/********************************************************************************
 * Copyright (C) 2026 EclipseSource and others.
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
package org.eclipse.theia.cloud.operator.routing;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.util.LabelsUtil;
import org.eclipse.theia.cloud.common.util.NamingUtil;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.operator.TheiaCloudOperatorArguments;

import com.google.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.client.OpenShiftClient;


/**
 * OpenShift Route-based implementation of {@link SessionRoutingStrategy}.
 * <p>
 * Instead of managing IngressRules on a shared Ingress resource, this strategy
 * creates individual OpenShift Route objects for each session by cloning a template Route.
 * Routes are deleted when sessions end.
 * <p>
 * Only subdomain-based routing ({@code usePaths: false}) is supported on OpenShift.
 */
public class OpenShiftRouteRoutingStrategy implements SessionRoutingStrategy {

    private static final Logger LOGGER = LogManager.getLogger(OpenShiftRouteRoutingStrategy.class);

    @Inject
    private TheiaCloudClient client;

    private OpenShiftClient osClient;

    private OpenShiftClient openShiftClient() {
        if (osClient == null) {
            osClient = client.kubernetes().adapt(OpenShiftClient.class);
        }
        return osClient;
    }

    @Inject
    private TheiaCloudOperatorArguments arguments;

    @Override
    public boolean ensureRoutingResourceExists(AppDefinition appDefinition, String correlationId) {
        String namespace = client.namespace();
        String templateName = appDefinition.getSpec().getIngressname();

        // Check if template Route already has an owner reference for this AppDefinition
        Route existingRoute = openShiftClient().routes().inNamespace(namespace).withName(templateName).get();
        if (existingRoute == null) {
            LOGGER.error(formatLogMessage(correlationId,
                    "Template Route '" + templateName + "' not found in namespace " + namespace));
            return false;
        }

        // Check if owner reference already exists — if so, nothing to do
        List<OwnerReference> existingRefs = existingRoute.getMetadata().getOwnerReferences();
        if (existingRefs != null
                && existingRefs.stream().anyMatch(ref -> appDefinition.getMetadata().getUid().equals(ref.getUid()))) {
            return true;
        }

        // Owner reference is missing — add it
        OwnerReference ownerReference = new OwnerReference();
        ownerReference.setApiVersion(HasMetadata.getApiVersion(AppDefinition.class));
        ownerReference.setKind(AppDefinition.KIND);
        ownerReference.setName(appDefinition.getMetadata().getName());
        ownerReference.setUid(appDefinition.getMetadata().getUid());

        openShiftClient().routes().inNamespace(namespace).withName(templateName).edit(route -> {
            List<OwnerReference> refs = route.getMetadata().getOwnerReferences();
            if (refs == null) {
                refs = new ArrayList<>();
                route.getMetadata().setOwnerReferences(refs);
            }
            refs.add(ownerReference);
            return route;
        });
        LOGGER.info(formatLogMessage(correlationId,
                "Added owner reference for AppDefinition to template Route '" + templateName + "'"));

        return true;
    }

    @Override
    public synchronized String addSessionRouting(Session session, AppDefinition appDefinition, Service service,
            String correlationId) {
        return createSessionRoute(session, appDefinition, service, correlationId);
    }

    // On OpenShift, Routes are always per-session (keyed by session UID), not per-instance.
    // The instance parameter is used by IngressRoutingStrategy for path-based routing but is
    // not applicable to Route-based routing where each session gets its own Route + hostname.
    @Override
    public synchronized String addSessionRouting(Session session, AppDefinition appDefinition, Service service,
            int instance, String correlationId) {
        return createSessionRoute(session, appDefinition, service, correlationId);
    }

    @Override
    public synchronized boolean removeSessionRouting(Session session, AppDefinition appDefinition,
            String correlationId) {
        return deleteSessionRoute(session, correlationId);
    }

    // On OpenShift, Routes are always per-session (keyed by session UID), not per-instance.
    // The instance parameter is used by IngressRoutingStrategy for path-based routing but is
    // not applicable to Route-based routing where each session gets its own Route + hostname.
    @Override
    public synchronized boolean removeSessionRouting(Session session, AppDefinition appDefinition, int instance,
            String correlationId) {
        return deleteSessionRoute(session, correlationId);
    }

    private String createSessionRoute(Session session, AppDefinition appDefinition, Service service,
            String correlationId) {
        String namespace = client.namespace();
        String templateName = appDefinition.getSpec().getIngressname();

        // Load the template Route
        Route template = openShiftClient().routes().inNamespace(namespace).withName(templateName).get();
        if (template == null) {
            LOGGER.error(formatLogMessage(correlationId,
                    "Template Route '" + templateName + "' not found. Cannot create session Route."));
            return null;
        }

        String sessionRouteName = NamingUtil.createNameWithSuffix(session, "route");
        String sessionHostname = computeSessionHostname(session);
        String serviceName = service.getMetadata().getName();

        // Build labels: session labels + app label matching the service
        Map<String, String> labels = new HashMap<>(LabelsUtil.createSessionLabels(session, appDefinition));
        labels.put("app", serviceName);

        // Build owner reference pointing to the Session CR
        OwnerReference ownerReference = new OwnerReference();
        ownerReference.setApiVersion(Session.API);
        ownerReference.setKind(Session.KIND);
        ownerReference.setName(session.getMetadata().getName());
        ownerReference.setUid(session.getMetadata().getUid());

        // Inherit annotations from the template (user customizations)
        Map<String, String> annotations = template.getMetadata().getAnnotations() != null
                ? new HashMap<>(template.getMetadata().getAnnotations())
                : new HashMap<>();
        // Remove Helm-managed annotations that should not be copied to session Routes
        annotations.remove("meta.helm.sh/release-name");
        annotations.remove("meta.helm.sh/release-namespace");

        // Build the session Route from the template
        RouteBuilder routeBuilder = new RouteBuilder().withNewMetadata().withName(sessionRouteName)
                .withNamespace(namespace).withAnnotations(annotations).withLabels(labels)
                .addToOwnerReferences(ownerReference).endMetadata().withNewSpec().withHost(sessionHostname).withNewTo()
                .withKind("Service").withName(serviceName).withWeight(100).endTo().withNewPort()
                .withNewTargetPort("http").endPort().withWildcardPolicy("None").endSpec();

        // Inherit TLS settings from the template
        if (template.getSpec().getTls() != null) {
            routeBuilder.editSpec().withTls(template.getSpec().getTls()).endSpec();
        }

        // Check if the Route already exists (idempotency for operator restarts / reconciliation)
        Route existingRoute = openShiftClient().routes().inNamespace(namespace).withName(sessionRouteName).get();
        if (existingRoute != null) {
            LOGGER.info(formatLogMessage(correlationId, "Session Route '" + sessionRouteName
                    + "' already exists with host '" + existingRoute.getSpec().getHost() + "'"));
            return existingRoute.getSpec().getHost() + "/";
        }

        Route sessionRoute = routeBuilder.build();

        try {
            openShiftClient().routes().inNamespace(namespace).resource(sessionRoute).create();
            LOGGER.info(formatLogMessage(correlationId,
                    "Created session Route '" + sessionRouteName + "' with host '" + sessionHostname + "'"));
        } catch (Exception e) {
            LOGGER.error(formatLogMessage(correlationId, "Failed to create session Route '" + sessionRouteName + "'"),
                    e);
            return null;
        }

        return sessionHostname + "/";
    }

    private boolean deleteSessionRoute(Session session, String correlationId) {
        String namespace = client.namespace();
        String sessionRouteName = NamingUtil.createNameWithSuffix(session, "route");

        Route existingRoute = openShiftClient().routes().inNamespace(namespace).withName(sessionRouteName).get();
        if (existingRoute == null) {
            LOGGER.info(formatLogMessage(correlationId, "Session Route '" + sessionRouteName
                    + "' not found — may have been cleaned up by owner reference GC."));
            return true;
        }

        try {
            openShiftClient().routes().inNamespace(namespace).withName(sessionRouteName).delete();
            LOGGER.info(formatLogMessage(correlationId, "Deleted session Route '" + sessionRouteName + "'"));
        } catch (Exception e) {
            LOGGER.error(formatLogMessage(correlationId, "Failed to delete session Route '" + sessionRouteName + "'"),
                    e);
            return false;
        }

        return true;
    }

    /**
     * Compute the hostname for a session Route. Uses the session's UID to create a
     * unique subdomain under the instances
     * host. For example: {@code ws-<uid-prefix>.<baseHost>} →
     * {@code ws-abc123def456.apps-crc.testing}
     */
    private String computeSessionHostname(Session session) {
        String instancesHost = arguments.getInstancesHost();
        String uid = session.getMetadata().getUid();
        // Use last 12 chars of UID to keep hostname short but unique
        String shortUid = uid.substring(uid.length() - 12);
        return shortUid + "." + instancesHost;
    }
}
