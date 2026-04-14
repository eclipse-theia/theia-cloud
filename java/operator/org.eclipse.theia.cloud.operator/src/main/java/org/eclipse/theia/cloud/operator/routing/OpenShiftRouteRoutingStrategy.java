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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.ResourceEdit;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.util.LabelsUtil;
import org.eclipse.theia.cloud.common.util.NamingUtil;
import org.eclipse.theia.cloud.operator.TheiaCloudOperatorArguments;
import org.eclipse.theia.cloud.operator.util.JavaResourceUtil;

import com.google.inject.Inject;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.TLSConfigBuilder;
import io.fabric8.openshift.client.OpenShiftClient;

/**
 * OpenShift Route-based implementation of {@link SessionRoutingStrategy}.
 * <p>
 * Instead of managing IngressRules on a shared Ingress resource, this strategy creates individual OpenShift Route
 * objects for each session from a classpath YAML template ({@code templateRoute.yaml}). Routes are deleted when
 * sessions end.
 * <p>
 * TLS settings and custom annotations are read from the {@code openshift-route-config} ConfigMap deployed by the Helm
 * chart.
 * <p>
 * Only subdomain-based routing ({@code usePaths: false}) is supported on OpenShift.
 */
public class OpenShiftRouteRoutingStrategy implements SessionRoutingStrategy {

    private static final Logger LOGGER = LogManager.getLogger(OpenShiftRouteRoutingStrategy.class);

    private static final String TEMPLATE_ROUTE_YAML = "/templateRoute.yaml";
    private static final String ROUTE_CONFIG_CM_NAME = "openshift-route-config";

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

    private volatile boolean routeConfigLoaded;
    private boolean useTls;
    private Map<String, String> routeAnnotations;

    private void loadRouteConfig() {
        if (routeConfigLoaded) {
            return;
        }
        String namespace = client.namespace();
        ConfigMap cm = client.kubernetes().configMaps().inNamespace(namespace).withName(ROUTE_CONFIG_CM_NAME).get();
        if (cm == null) {
            LOGGER.warn(formatLogMessage("INIT", "ConfigMap '" + ROUTE_CONFIG_CM_NAME + "' not found in namespace "
                    + namespace + ". Using defaults (no TLS, no annotations)."));
            this.useTls = false;
            this.routeAnnotations = Map.of();
            this.routeConfigLoaded = true;
            return;
        }
        this.useTls = Boolean.parseBoolean(cm.getData().getOrDefault("useTls", "false"));
        String annotationsYaml = cm.getData().get("annotations");
        if (annotationsYaml != null && !annotationsYaml.isBlank()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> parsed = Serialization.unmarshal(annotationsYaml, Map.class);
                this.routeAnnotations = parsed != null ? parsed : Map.of();
            } catch (Exception e) {
                LOGGER.warn(formatLogMessage("INIT", "Failed to parse annotations from ConfigMap '"
                        + ROUTE_CONFIG_CM_NAME + "'. Using empty annotations."), e);
                this.routeAnnotations = Map.of();
            }
        } else {
            this.routeAnnotations = Map.of();
        }
        this.routeConfigLoaded = true;
    }

    @Override
    public boolean ensureRoutingResourceExists(AppDefinition appDefinition, String correlationId) {
        // No-op: the operator creates session Routes from a classpath template.
        // There is no template Route on the cluster to attach owner references to.
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
        loadRouteConfig();

        String namespace = client.namespace();
        String sessionRouteName = NamingUtil.createNameWithSuffix(session, "route");
        String sessionHostname = computeSessionHostname(session);
        String serviceName = service.getMetadata().getName();

        // Check if the Route already exists (idempotency for operator restarts / reconciliation)
        Route existingRoute = openShiftClient().routes().inNamespace(namespace).withName(sessionRouteName).get();
        if (existingRoute != null) {
            LOGGER.info(formatLogMessage(correlationId, "Session Route '" + sessionRouteName
                    + "' already exists with host '" + existingRoute.getSpec().getHost() + "'"));
            return existingRoute.getSpec().getHost() + "/";
        }

        // Build replacement map for the template
        Map<String, String> replacements = new HashMap<>();
        replacements.put("placeholder-routename", sessionRouteName);
        replacements.put("placeholder-namespace", namespace);
        replacements.put("placeholder-hostname", sessionHostname);
        replacements.put("placeholder-servicename", serviceName);

        String routeYaml;
        try {
            routeYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(TEMPLATE_ROUTE_YAML, replacements,
                    correlationId);
        } catch (IOException | URISyntaxException e) {
            LOGGER.error(formatLogMessage(correlationId,
                    "Error while loading Route template for session " + session.getMetadata().getName()), e);
            return null;
        }

        // Parse the YAML into a Route object
        Route sessionRoute;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(routeYaml.getBytes(StandardCharsets.UTF_8))) {
            sessionRoute = openShiftClient().routes().load(inputStream).item();
        } catch (IOException e) {
            LOGGER.error(formatLogMessage(correlationId,
                    "Error while parsing Route YAML for session " + session.getMetadata().getName()), e);
            return null;
        }
        if (sessionRoute == null) {
            LOGGER.error(formatLogMessage(correlationId,
                    "Parsed Route is null for session " + session.getMetadata().getName()));
            return null;
        }

        // Set labels: session labels + app label matching the service
        Map<String, String> labels = new HashMap<>(LabelsUtil.createSessionLabels(session, appDefinition));
        labels.put("app", serviceName);
        sessionRoute.getMetadata().setLabels(labels);

        // Update the placeholder owner reference to point to the Session CR
        ResourceEdit.<Route> updateOwnerReference(0, Session.API, Session.KIND, session.getMetadata().getName(),
                session.getMetadata().getUid(), correlationId).accept(sessionRoute);

        // Add TLS settings if configured
        if (useTls) {
            sessionRoute.getSpec().setTls(new TLSConfigBuilder().withTermination("edge").build());
        }

        // Merge in annotations from the ConfigMap
        if (routeAnnotations != null && !routeAnnotations.isEmpty()) {
            Map<String, String> annotations = sessionRoute.getMetadata().getAnnotations();
            if (annotations == null) {
                annotations = new HashMap<>();
                sessionRoute.getMetadata().setAnnotations(annotations);
            }
            annotations.putAll(routeAnnotations);
        }

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
                    + "' not found -- may have been cleaned up by owner reference GC."));
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

    @Override
    public String getSessionURL(AppDefinition appDefinition, Session session) {
        loadRouteConfig();
        String protocol = useTls ? "https://" : "http://";
        return protocol + computeSessionHostname(session) + "/";
    }

    @Override
    public String getSessionURL(AppDefinition appDefinition, int instance) {
        // On OpenShift, session URLs are only known once a session (with a UID) exists.
        // For eager-start pre-provisioned instances that have no session yet, return an empty placeholder.
        return "";
    }

    /**
     * Compute the hostname for a session Route. Uses the full session UID to create a unique subdomain under the
     * instances host - the same identifier that {@code IngressPathProvider} uses for Ingress paths.
     * <p>
     * For example: {@code <full-uid>.ws.apps-crc.testing}
     */
    private String computeSessionHostname(Session session) {
        String instancesHost = arguments.getInstancesHost();
        String uid = session.getMetadata().getUid();
        return uid + "." + instancesHost;
    }
}
