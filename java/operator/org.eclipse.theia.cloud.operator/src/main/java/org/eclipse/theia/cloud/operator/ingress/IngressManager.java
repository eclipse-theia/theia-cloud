package org.eclipse.theia.cloud.operator.ingress;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.operator.TheiaCloudOperatorArguments;
import org.eclipse.theia.cloud.operator.util.K8sUtil;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPBackendRef;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPBackendRefBuilder;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPHeader;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPHeaderBuilder;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPHeaderFilterBuilder;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPPathMatch;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPPathMatchBuilder;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPPathModifierBuilder;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRequestRedirectFilterBuilder;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRoute;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRouteFilter;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRouteFilterBuilder;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRouteMatch;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRouteMatchBuilder;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRouteRule;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRouteRuleBuilder;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRouteSpec;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRouteSpecBuilder;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPURLRewriteFilterBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;

/**
 * Centralized manager for HTTPRoute operations (Gateway API).
 *
 * Caller intent:
 * - find the route for an app definition
 * - expose a session path via redirect + backend route rule
 * - unexpose a session path during cleanup
 */
@Singleton
public class IngressManager {

    private static final Logger LOGGER = LogManager.getLogger(IngressManager.class);
    /**
     * Envoy Gateway runtime expression for the current request path.
     * This value is interpreted by Envoy; other Gateway API implementations may
     * treat it as a plain literal string.
     */
    private static final String ENVOY_REQUEST_PATH_EXPRESSION = "%REQ(:PATH)%";
    private static final int HTTP_CONFLICT = 409;
    private static final int ROUTE_EDIT_MAX_RETRIES = 5;
    private static final long ROUTE_EDIT_RETRY_BACKOFF_MS = 200L;
    private static final long ROUTE_EDIT_RETRY_JITTER_MS = 100L;

    @Inject
    private TheiaCloudClient client;

    @Inject
    private TheiaCloudOperatorArguments arguments;

    @Inject
    private IngressPathProvider pathProvider;

    /**
     * Gets the HTTPRoute for an app definition.
     */
    public Optional<HTTPRoute> getIngress(AppDefinition appDefinition, String correlationId) {
        Optional<HTTPRoute> route = K8sUtil.getExistingHttpRoute(
                client.kubernetes(),
                client.namespace(),
                appDefinition.getMetadata().getName(),
                appDefinition.getMetadata().getUid());
        if (route.isEmpty()) {
            LOGGER.debug(formatLogMessage(correlationId,
                    "No HTTPRoute found for app definition " + appDefinition.getMetadata().getName()));
        }
        return route;
    }

    /**
     * Exposes an eager session by adding/updating path rules in the shared HTTPRoute.
     *
     * @return the full URL for the session
     */
    public String addRuleForSession(
            HTTPRoute route,
            Service service,
            AppDefinition appDefinition,
            int instance,
            String correlationId) {

        String path = pathProvider.getPath(appDefinition, instance);
        return upsertRulesForPath(route, service, appDefinition, path, correlationId);
    }

    /**
     * Exposes a lazy session by adding/updating path rules in the shared HTTPRoute.
     *
     * @return the full URL for the session
     */
    public String addRuleForSession(
            HTTPRoute route,
            Service service,
            AppDefinition appDefinition,
            Session session,
            String correlationId) {

        String path = pathProvider.getPath(appDefinition, session);
        return upsertRulesForPath(route, service, appDefinition, path, correlationId);
    }

    /**
     * Removes rules for an eager session path from the shared HTTPRoute.
     */
    public void removeRulesForSession(
            HTTPRoute route,
            AppDefinition appDefinition,
            int instance,
            String correlationId) {

        String path = pathProvider.getPath(appDefinition, instance);
        removeRulesForPath(route, path, correlationId);
    }

    /**
     * Removes rules for a lazy session path from the shared HTTPRoute.
     */
    public void removeRulesForSession(
            HTTPRoute route,
            AppDefinition appDefinition,
            Session session,
            String correlationId) {

        String path = pathProvider.getPath(appDefinition, session);
        removeRulesForPath(route, path, correlationId);
    }

    private String upsertRulesForPath(
            HTTPRoute route,
            Service service,
            AppDefinition appDefinition,
            String path,
            String correlationId) {

        String routeName = route.getMetadata().getName();
        String serviceName = service.getMetadata().getName();
        int port = appDefinition.getSpec().getPort();
        List<String> hosts = buildRouteHosts(appDefinition);

        try {
            editRouteWithRetry(routeName, routeToUpdate -> {
                HTTPRouteSpec spec = getOrCreateSpec(routeToUpdate);
                ensureHostnamesPresent(spec, hosts);

                List<HTTPRouteRule> rules = getOrCreateRules(spec);
                removeRulesMatchingPath(rules, path);

                rules.add(createRedirectRule(path));
                rules.add(createRouteRule(serviceName, port, path));
            }, correlationId);

            LOGGER.info(formatLogMessage(correlationId,
                    "Configured HTTPRoute " + routeName + " for path " + path
                            + " and backend service " + serviceName));
            return arguments.getInstancesHost() + ensureTrailingSlash(path);
        } catch (KubernetesClientException e) {
            LOGGER.error(formatLogMessage(correlationId,
                    "Failed to configure HTTPRoute " + routeName + " for path " + path), e);
            throw e;
        }
    }

    private void removeRulesForPath(HTTPRoute route, String path, String correlationId) {
        String routeName = route.getMetadata().getName();

        try {
            int[] removedRuleCount = new int[] { 0 };
            editRouteWithRetry(routeName, routeToUpdate -> {
                HTTPRouteSpec spec = getOrCreateSpec(routeToUpdate);
                List<HTTPRouteRule> rules = getOrCreateRules(spec);
                removedRuleCount[0] = removeRulesMatchingPath(rules, path);
                // hostnames are route-wide and shared across multiple session paths;
                // removing rules for one path must not delete shared hostnames.
            }, correlationId);

            if (removedRuleCount[0] > 0) {
                LOGGER.info(formatLogMessage(correlationId,
                        "Removed " + removedRuleCount[0] + " HTTPRoute rule(s) for path " + path + " from "
                                + routeName));
            } else {
                LOGGER.debug(formatLogMessage(correlationId,
                        "No HTTPRoute rules found for path " + path + " in " + routeName));
            }
        } catch (KubernetesClientException e) {
            LOGGER.error(formatLogMessage(correlationId,
                    "Failed to remove HTTPRoute rules for path " + path + " from " + routeName), e);
            throw e;
        }
    }

    private List<String> buildRouteHosts(AppDefinition appDefinition) {
        String instancesHost = arguments.getInstancesHost();
        List<String> hosts = new ArrayList<>();
        hosts.add(instancesHost);

        List<String> prefixes = appDefinition.getSpec().getIngressHostnamePrefixes();
        if (prefixes != null) {
            for (String prefix : prefixes) {
                hosts.add(prefix + instancesHost);
            }
        }

        return hosts;
    }

    private void ensureHostnamesPresent(HTTPRouteSpec spec, List<String> hosts) {
        List<String> hostnames = spec.getHostnames();
        if (hostnames == null) {
            hostnames = new ArrayList<>();
            spec.setHostnames(hostnames);
        }
        for (String host : hosts) {
            if (!hostnames.contains(host)) {
                hostnames.add(host);
            }
        }
    }

    private int removeRulesMatchingPath(List<HTTPRouteRule> rules, String path) {
        int initialSize = rules.size();
        rules.removeIf(rule -> hasPathMatch(rule, path));
        return initialSize - rules.size();
    }

    private HTTPRouteSpec getOrCreateSpec(HTTPRoute route) {
        HTTPRouteSpec spec = route.getSpec();
        if (spec == null) {
            spec = new HTTPRouteSpecBuilder().build();
            route.setSpec(spec);
        }
        return spec;
    }

    private List<HTTPRouteRule> getOrCreateRules(HTTPRouteSpec spec) {
        List<HTTPRouteRule> rules = spec.getRules();
        if (rules == null) {
            rules = new ArrayList<>();
            spec.setRules(rules);
        }
        return rules;
    }

    /**
     * Creates the backend routing rule for a session path.
     *
     * The X-Forwarded-Uri header uses an Envoy Gateway runtime expression and
     * therefore requires Envoy Gateway for correct behavior.
     */
    private HTTPRouteRule createRouteRule(String serviceName, int port, String path) {
        HTTPRouteMatch pathPrefixMatch = new HTTPRouteMatchBuilder()
                .withPath(new HTTPPathMatchBuilder()
                        .withType("PathPrefix")
                        .withValue(ensureTrailingSlash(path))
                        .build())
                .build();

        HTTPHeader forwardedUriHeader = new HTTPHeaderBuilder()
                .withName("X-Forwarded-Uri")
                .withValue(ENVOY_REQUEST_PATH_EXPRESSION)
                .build();

        HTTPRouteFilter requestHeaderFilter = new HTTPRouteFilterBuilder()
                .withType("RequestHeaderModifier")
                .withRequestHeaderModifier(new HTTPHeaderFilterBuilder()
                        .withSet(forwardedUriHeader)
                        .build())
                .build();

        HTTPRouteFilter urlRewriteFilter = new HTTPRouteFilterBuilder()
                .withType("URLRewrite")
                .withUrlRewrite(new HTTPURLRewriteFilterBuilder()
                        .withPath(new HTTPPathModifierBuilder()
                                .withType("ReplacePrefixMatch")
                                .withReplacePrefixMatch("/")
                                .build())
                        .build())
                .build();

        HTTPBackendRef backendRef = new HTTPBackendRefBuilder()
                .withName(serviceName)
                .withPort(port)
                .build();

        return new HTTPRouteRuleBuilder()
                .withMatches(pathPrefixMatch)
                .withFilters(requestHeaderFilter, urlRewriteFilter)
                .withBackendRefs(backendRef)
                .build();
    }

    private HTTPRouteRule createRedirectRule(String path) {
        HTTPRouteMatch exactMatch = new HTTPRouteMatchBuilder()
                .withPath(new HTTPPathMatchBuilder()
                        .withType("Exact")
                        .withValue(path)
                        .build())
                .build();

        HTTPRouteFilter redirectFilter = new HTTPRouteFilterBuilder()
                .withType("RequestRedirect")
                .withRequestRedirect(new HTTPRequestRedirectFilterBuilder()
                        .withStatusCode(302)
                        .withPath(new HTTPPathModifierBuilder()
                                .withType("ReplaceFullPath")
                                .withReplaceFullPath(ensureTrailingSlash(path))
                                .build())
                        .build())
                .build();

        return new HTTPRouteRuleBuilder()
                .withMatches(exactMatch)
                .withFilters(redirectFilter)
                .build();
    }

    private String ensureTrailingSlash(String value) {
        return value.endsWith("/") ? value : value + "/";
    }

    private boolean hasPathMatch(HTTPRouteRule rule, String path) {
        List<HTTPRouteMatch> matches = rule.getMatches();
        if (matches == null) {
            return false;
        }
        for (HTTPRouteMatch match : matches) {
            if (match == null) {
                continue;
            }
            HTTPPathMatch pathMatch = match.getPath();
            if (pathMatch == null || pathMatch.getValue() == null) {
                continue;
            }
            if (normalizePath(path).equals(normalizePath(pathMatch.getValue()))) {
                return true;
            }
        }
        return false;
    }

    private void editRouteWithRetry(String routeName, Consumer<HTTPRoute> editor, String correlationId) {
        for (int attempt = 1; attempt <= ROUTE_EDIT_MAX_RETRIES; attempt++) {
            try {
                client.httpRoutes().resource(routeName).edit(routeToUpdate -> {
                    if (routeToUpdate == null) {
                        throw new KubernetesClientException("HTTPRoute " + routeName + " not found");
                    }
                    editor.accept(routeToUpdate);
                    return routeToUpdate;
                });
                return;
            } catch (KubernetesClientException e) {
                if (e.getCode() != HTTP_CONFLICT || attempt == ROUTE_EDIT_MAX_RETRIES) {
                    throw e;
                }
                LOGGER.warn(formatLogMessage(correlationId,
                        "HTTPRoute edit conflict for " + routeName + " (attempt "
                                + attempt + "/" + ROUTE_EDIT_MAX_RETRIES + "). Retrying."));
                try {
                    long jitter = ThreadLocalRandom.current()
                            .nextLong(-ROUTE_EDIT_RETRY_JITTER_MS, ROUTE_EDIT_RETRY_JITTER_MS + 1);
                    long backoffMs = Math.max(0, ROUTE_EDIT_RETRY_BACKOFF_MS + jitter);
                    Thread.sleep(backoffMs);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    private String normalizePath(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() > 1 && value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
