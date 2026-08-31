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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.util.LabelsUtil;
import org.eclipse.theia.cloud.common.util.NamingUtil;
import org.eclipse.theia.cloud.operator.TheiaCloudOperatorArguments;
import org.eclipse.theia.cloud.operator.util.JavaResourceUtil;

import com.google.inject.Inject;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.TLSConfigBuilder;
import io.fabric8.openshift.client.OpenShiftClient;

/**
 * OpenShift Route-based implementation of {@link SessionRoutingStrategy}.
 * <p>
 * Instead of managing IngressRules on a shared Ingress resource, this strategy
 * creates individual OpenShift Route
 * objects for each session from a classpath YAML template
 * ({@code templateRoute.yaml}). Routes are deleted when
 * sessions end.
 * <p>
 * TLS settings and custom annotations are read from the
 * {@code openshift-route-config} ConfigMap deployed by the Helm
 * chart.
 * <p>
 * Only subdomain-based routing ({@code usePaths: false}) is supported on
 * OpenShift. This strategy does not currently
 * create the dynamic wildcard hosts configured through
 * {@code ingressHostnamePrefixes}; see the OpenShift deployment
 * documentation for the current limitation and workaround.
 */
public class OpenShiftRouteRoutingStrategy implements SessionRoutingStrategy {

    private static final Logger LOGGER = LogManager.getLogger(OpenShiftRouteRoutingStrategy.class);

    private static final String TEMPLATE_ROUTE_YAML = "/templateRoute.yaml";
    private static final String ROUTE_CONFIG_CM_NAME = "openshift-route-config";

    @Inject
    private TheiaCloudClient client;

    @Inject
    private TheiaCloudOperatorArguments arguments;

    private OpenShiftClient osClient;
    private boolean useTls;
    private Map<String, String> routeAnnotations = Map.of();

    /** Package-private constructor for unit tests. */
    OpenShiftRouteRoutingStrategy(TheiaCloudOperatorArguments arguments) {
        this.arguments = arguments;
    }

    OpenShiftRouteRoutingStrategy() {
    }

    private synchronized void ensureOpenShiftClient() {
        if (osClient == null) {
            osClient = client.kubernetes().adapt(OpenShiftClient.class);
        }
    }

    private void refreshRouteConfiguration(String correlationId) {
        String namespace = client.namespace();
        ConfigMap configMap = client.kubernetes().configMaps().inNamespace(namespace).withName(ROUTE_CONFIG_CM_NAME)
                .get();
        if (configMap == null) {
            LOGGER.warn(formatLogMessage(correlationId, "ConfigMap '" + ROUTE_CONFIG_CM_NAME
                    + "' not found in namespace " + namespace + ". Using defaults (no TLS, no annotations)."));
        }
        applyRouteConfiguration(configMap, correlationId);
    }

    void applyRouteConfiguration(ConfigMap configMap) {
        applyRouteConfiguration(configMap, "TEST");
    }

    private void applyRouteConfiguration(ConfigMap configMap, String correlationId) {
        Map<String, String> data = configMap == null || configMap.getData() == null ? Map.of() : configMap.getData();
        this.useTls = Boolean.parseBoolean(data.getOrDefault("useTls", "false"));
        String annotationsYaml = data.get("annotations");
        if (annotationsYaml == null || annotationsYaml.isBlank()) {
            this.routeAnnotations = Map.of();
            return;
        }
        try {
            Map<?, ?> parsed = Serialization.unmarshal(annotationsYaml, Map.class);
            if (parsed == null) {
                this.routeAnnotations = Map.of();
                return;
            }
            Map<String, String> annotations = new HashMap<>();
            for (Map.Entry<?, ?> entry : parsed.entrySet()) {
                if (!(entry.getKey() instanceof String key) || entry.getValue() == null
                        || entry.getValue() instanceof Map<?, ?> || entry.getValue() instanceof List<?>) {
                    LOGGER.warn(formatLogMessage(correlationId, "Ignoring non-scalar annotation from ConfigMap '"
                            + ROUTE_CONFIG_CM_NAME + "'."));
                    continue;
                }
                annotations.put(key, String.valueOf(entry.getValue()));
            }
            this.routeAnnotations = annotations;
        } catch (Exception e) {
            LOGGER.warn(formatLogMessage(correlationId, "Failed to parse annotations from ConfigMap '"
                    + ROUTE_CONFIG_CM_NAME + "'. Using empty annotations."), e);
            this.routeAnnotations = Map.of();
        }
    }

    String protocol() {
        return useTls ? "https://" : "http://";
    }

    String protocol(Route route) {
        return route.getSpec().getTls() == null ? "http://" : "https://";
    }

    Map<String, String> routeAnnotations() {
        return routeAnnotations;
    }

    @Override
    public boolean ensureRoutingResourceExists(AppDefinition appDefinition, String correlationId) {
        return true;
    }

    @Override
    public synchronized String addSessionRouting(Session session, AppDefinition appDefinition, Service service,
            String correlationId) {
        ensureOpenShiftClient();
        refreshRouteConfiguration(correlationId);
        String routeName = NamingUtil.createNameWithSuffix(session, "route");
        String hostname = computeSessionHostname(session);
        return createRoute(routeName, hostname, session, appDefinition, service, correlationId);
    }

    @Override
    public synchronized String addSessionRouting(Session session, AppDefinition appDefinition, Service service,
            int instance, String correlationId) {
        ensureOpenShiftClient();
        refreshRouteConfiguration(correlationId);
        String routeName = computeInstanceRouteName(appDefinition, instance);
        String hostname = computeInstanceHostname(appDefinition, instance);
        return createRoute(routeName, hostname, session, appDefinition, service, correlationId);
    }

    @Override
    public synchronized boolean removeSessionRouting(Session session, AppDefinition appDefinition,
            String correlationId) {
        ensureOpenShiftClient();
        String routeName = NamingUtil.createNameWithSuffix(session, "route");
        return deleteRoute(routeName, correlationId);
    }

    @Override
    public synchronized boolean removeSessionRouting(Session session, AppDefinition appDefinition, int instance,
            String correlationId) {
        ensureOpenShiftClient();
        String routeName = computeInstanceRouteName(appDefinition, instance);
        return deleteRoute(routeName, correlationId);
    }

    @Override
    public synchronized String getSessionURL(AppDefinition appDefinition, Session session) {
        refreshRouteConfiguration("GET_SESSION_URL");
        return protocol() + computeSessionHostname(session) + "/";
    }

    @Override
    public synchronized String getSessionURL(AppDefinition appDefinition, int instance) {
        refreshRouteConfiguration("GET_SESSION_URL");
        return protocol() + computeInstanceHostname(appDefinition, instance) + "/";
    }

    private String createRoute(String routeName, String hostname, Session session, AppDefinition appDefinition,
            Service service, String correlationId) {
        String namespace = client.namespace();
        String serviceName = service.getMetadata().getName();

        Route existingRoute = osClient.routes().inNamespace(namespace).withName(routeName).get();
        if (existingRoute != null) {
            osClient.routes().inNamespace(namespace).withName(routeName).edit(route -> {
                updateSessionOwnerReference(route, session);
                return route;
            });
            LOGGER.info(formatLogMessage(correlationId, "Route '" + routeName + "' already exists with host '"
                    + existingRoute.getSpec().getHost() + "'"));
            return protocol(existingRoute) + existingRoute.getSpec().getHost() + "/";
        }

        Map<String, String> replacements = new HashMap<>();
        replacements.put("placeholder-routename", routeName);
        replacements.put("placeholder-namespace", namespace);
        replacements.put("placeholder-hostname", hostname);
        replacements.put("placeholder-servicename", serviceName);

        String routeYaml;
        try {
            routeYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(TEMPLATE_ROUTE_YAML, replacements,
                    correlationId);
        } catch (IOException | URISyntaxException e) {
            LOGGER.error(formatLogMessage(correlationId,
                    "Error while loading Route template for route '" + routeName + "'"), e);
            return null;
        }

        Route route;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(routeYaml.getBytes(StandardCharsets.UTF_8))) {
            route = osClient.routes().load(inputStream).item();
        } catch (IOException e) {
            LOGGER.error(formatLogMessage(correlationId,
                    "Error while parsing Route YAML for route '" + routeName + "'"), e);
            return null;
        }
        if (route == null) {
            LOGGER.error(formatLogMessage(correlationId, "Parsed Route is null for route '" + routeName + "'"));
            return null;
        }

        Map<String, String> labels = new HashMap<>(LabelsUtil.createSessionLabels(session, appDefinition));
        labels.put("app", serviceName);
        route.getMetadata().setLabels(labels);

        updateSessionOwnerReference(route, session);

        if (useTls) {
            route.getSpec().setTls(new TLSConfigBuilder().withTermination("edge").build());
        }

        if (!routeAnnotations.isEmpty()) {
            Map<String, String> annotations = route.getMetadata().getAnnotations();
            if (annotations == null) {
                annotations = new HashMap<>();
                route.getMetadata().setAnnotations(annotations);
            }
            annotations.putAll(routeAnnotations);
        }

        try {
            osClient.routes().inNamespace(namespace).resource(route).create();
            LOGGER.info(formatLogMessage(correlationId,
                    "Created Route '" + routeName + "' with host '" + hostname + "'"));
        } catch (Exception e) {
            LOGGER.error(formatLogMessage(correlationId, "Failed to create Route '" + routeName + "'"), e);
            return null;
        }

        return protocol() + hostname + "/";
    }

    void updateSessionOwnerReference(Route route, Session session) {
        List<OwnerReference> ownerReferences = route.getMetadata().getOwnerReferences();
        if (ownerReferences == null) {
            ownerReferences = new ArrayList<>();
            route.getMetadata().setOwnerReferences(ownerReferences);
        }

        OwnerReference sessionOwner = null;
        for (int index = ownerReferences.size() - 1; index >= 0; index--) {
            OwnerReference ownerReference = ownerReferences.get(index);
            if (ownerReference != null && Session.KIND.equals(ownerReference.getKind())) {
                if (sessionOwner == null) {
                    sessionOwner = ownerReference;
                } else {
                    ownerReferences.remove(index);
                }
            }
        }
        if (sessionOwner == null) {
            sessionOwner = new OwnerReference();
            ownerReferences.add(sessionOwner);
        }
        sessionOwner.setApiVersion(Session.API);
        sessionOwner.setKind(Session.KIND);
        sessionOwner.setName(session.getMetadata().getName());
        sessionOwner.setUid(session.getMetadata().getUid());
    }

    private boolean deleteRoute(String routeName, String correlationId) {
        String namespace = client.namespace();

        Route existingRoute = osClient.routes().inNamespace(namespace).withName(routeName).get();
        if (existingRoute == null) {
            LOGGER.info(formatLogMessage(correlationId,
                    "Route '" + routeName + "' not found -- may have been cleaned up by owner reference GC."));
            return true;
        }

        try {
            osClient.routes().inNamespace(namespace).withName(routeName).delete();
            LOGGER.info(formatLogMessage(correlationId, "Deleted Route '" + routeName + "'"));
        } catch (Exception e) {
            LOGGER.error(formatLogMessage(correlationId, "Failed to delete Route '" + routeName + "'"), e);
            return false;
        }

        return true;
    }

    /**
     * Compute the hostname for a session Route. Uses the full session UID to create
     * a unique subdomain under the
     * instances host.
     * <p>
     * For example: {@code <full-uid>.ws.apps-crc.testing}
     */
    String computeSessionHostname(Session session) {
        String instancesHost = arguments.getInstancesHost();
        String uid = session.getMetadata().getUid();
        return uid + "." + instancesHost;
    }

    String computeInstanceHostname(AppDefinition appDefinition, int instance) {
        String instancesHost = arguments.getInstancesHost();
        String subdomainLabel = NamingUtil.asValidName(
                appDefinition.getSpec().getName() + "-" + instance, 63);
        return subdomainLabel + "." + instancesHost;
    }

    String computeInstanceRouteName(AppDefinition appDefinition, int instance) {
        return NamingUtil.createNameWithSuffix(appDefinition, instance, "route");
    }
}
