package org.eclipse.theia.cloud.operator.ingress;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.operator.TheiaCloudOperatorArguments;
import org.eclipse.theia.cloud.operator.handler.AddedHandlerUtil;
import org.eclipse.theia.cloud.operator.util.K8sUtil;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressRuleValue;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBackend;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRule;
import io.fabric8.kubernetes.api.model.networking.v1.IngressServiceBackend;
import io.fabric8.kubernetes.api.model.networking.v1.ServiceBackendPort;
import io.fabric8.kubernetes.client.KubernetesClientException;

/**
 * Centralized manager for ingress operations.
 * Handles adding/removing ingress rules for sessions.
 */
@Singleton
public class IngressManager {

    private static final Logger LOGGER = LogManager.getLogger(IngressManager.class);

    @Inject
    private TheiaCloudClient client;

    @Inject
    private TheiaCloudOperatorArguments arguments;

    @Inject
    private IngressPathProvider pathProvider;

    /**
     * Specification for an ingress rule to be added.
     */
    public static class IngressRuleSpec {
        private final String serviceName;
        private final int port;
        private final String path;
        private final List<String> hosts;

        private IngressRuleSpec(Builder builder) {
            this.serviceName = builder.serviceName;
            this.port = builder.port;
            this.path = builder.path;
            this.hosts = builder.hosts;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String serviceName;
            private int port;
            private String path;
            private List<String> hosts = new ArrayList<>();

            public Builder serviceName(String serviceName) {
                this.serviceName = serviceName;
                return this;
            }

            public Builder port(int port) {
                this.port = port;
                return this;
            }

            public Builder path(String path) {
                this.path = path;
                return this;
            }

            public Builder host(String host) {
                this.hosts.add(host);
                return this;
            }

            public Builder hosts(List<String> hosts) {
                this.hosts.addAll(hosts);
                return this;
            }

            public IngressRuleSpec build() {
                return new IngressRuleSpec(this);
            }
        }
    }

    /**
     * Gets the ingress for an app definition.
     */
    public Optional<Ingress> getIngress(AppDefinition appDefinition, String correlationId) {
        return K8sUtil.getExistingIngress(
                client.kubernetes(),
                client.namespace(),
                appDefinition.getMetadata().getName(),
                appDefinition.getMetadata().getUid());
    }

    /**
     * Adds an ingress rule for a session using eager start (prewarmed instance).
     * 
     * @return the full URL for the session
     */
    public String addRuleForEagerSession(
            Ingress ingress,
            Service service,
            AppDefinition appDefinition,
            int instance,
            String correlationId) {

        String instancesHost = arguments.getInstancesHost();
        String path = pathProvider.getPath(appDefinition, instance);
        int port = appDefinition.getSpec().getPort();

        // Include hostname prefixes (e.g. *.webview.) for eager sessions too
        List<String> hosts = new ArrayList<>();
        hosts.add(instancesHost);
        List<String> prefixes = appDefinition.getSpec().getIngressHostnamePrefixes();
        if (prefixes != null) {
            for (String prefix : prefixes) {
                hosts.add(prefix + instancesHost);
            }
        }

        addRule(ingress, IngressRuleSpec.builder()
                .serviceName(service.getMetadata().getName())
                .port(port)
                .path(path)
                .hosts(hosts)
                .build(), correlationId);

        return instancesHost + path + "/";
    }

    /**
     * Adds an ingress rule for a session using lazy start.
     * Supports multiple hosts (for hostname prefixes).
     * 
     * @return the full URL for the session
     */
    public String addRuleForLazySession(
            Ingress ingress,
            Service service,
            Session session,
            AppDefinition appDefinition,
            String correlationId) {

        String instancesHost = arguments.getInstancesHost();
        String path = pathProvider.getPath(appDefinition, session);
        int port = appDefinition.getSpec().getPort();

        // Build list of all hosts
        List<String> hosts = new ArrayList<>();
        hosts.add(instancesHost);

        List<String> prefixes = appDefinition.getSpec().getIngressHostnamePrefixes();
        if (prefixes != null) {
            for (String prefix : prefixes) {
                hosts.add(prefix + instancesHost);
            }
        }

        addRule(ingress, IngressRuleSpec.builder()
                .serviceName(service.getMetadata().getName())
                .port(port)
                .path(path)
                .hosts(hosts)
                .build(), correlationId);

        return instancesHost + path + "/";
    }

    /**
     * Adds ingress rules according to the specification.
     */
    public synchronized void addRule(Ingress ingress, IngressRuleSpec spec, String correlationId) {
        try {
            client.ingresses().edit(correlationId, ingress.getMetadata().getName(), ingressToUpdate -> {
                for (String host : spec.hosts) {
                    IngressRule rule = createIngressRule(host, spec.serviceName, spec.port, spec.path);
                    ingressToUpdate.getSpec().getRules().add(rule);
                }
            });
            LOGGER.info(formatLogMessage(correlationId,
                    "Added ingress rule for path " + spec.path + " to " + ingress.getMetadata().getName()));
        } catch (KubernetesClientException e) {
            LOGGER.error(formatLogMessage(correlationId,
                    "Failed to add ingress rule to " + ingress.getMetadata().getName()), e);
            throw e;
        }
    }

    /**
     * Removes an ingress rule for an eager session.
     */
    public void removeRuleForEagerSession(
            Ingress ingress,
            AppDefinition appDefinition,
            int instance,
            String correlationId) {

        String path = pathProvider.getPath(appDefinition, instance);
        removeRuleByPath(ingress, path, correlationId);
    }

    /**
     * Removes ingress rules for a lazy session (handles multiple hosts).
     */
    public boolean removeRulesForLazySession(
            Ingress ingress,
            Session session,
            AppDefinition appDefinition,
            String correlationId) {

        String instancesHost = arguments.getInstancesHost();
        String path = pathProvider.getPath(appDefinition, session);

        List<String> hosts = new ArrayList<>();
        hosts.add(instancesHost);

        List<String> prefixes = appDefinition.getSpec().getIngressHostnamePrefixes();
        if (prefixes != null) {
            for (String prefix : prefixes) {
                hosts.add(prefix + instancesHost);
            }
        }

        return removeRulesByPathAndHosts(ingress, path, hosts, correlationId);
    }

    /**
     * Removes ingress rule by path (matches any host).
     */
    public synchronized void removeRuleByPath(Ingress ingress, String path, String correlationId) {
        String fullPath = path + AddedHandlerUtil.INGRESS_REWRITE_PATH;

        try {
            client.ingresses().resource(ingress.getMetadata().getName()).edit(ingressToUpdate -> {
                ingressToUpdate.getSpec().getRules().removeIf(rule -> {
                    if (rule.getHttp() == null) {
                        return false;
                    }
                    return rule.getHttp().getPaths().stream()
                            .anyMatch(httpPath -> fullPath.equals(httpPath.getPath()));
                });
                return ingressToUpdate;
            });
            LOGGER.info(formatLogMessage(correlationId,
                    "Removed ingress rule for path " + path + " from " + ingress.getMetadata().getName()));
        } catch (KubernetesClientException e) {
            LOGGER.error(formatLogMessage(correlationId,
                    "Failed to remove ingress rule from " + ingress.getMetadata().getName()), e);
            throw e;
        }
    }

    /**
     * Removes ingress rules matching path and specific hosts.
     */
    public synchronized boolean removeRulesByPathAndHosts(
            Ingress ingress,
            String path,
            List<String> hosts,
            String correlationId) {

        String fullPath = path + AddedHandlerUtil.INGRESS_REWRITE_PATH;

        try {
            client.ingresses().resource(ingress.getMetadata().getName()).edit(ingressToUpdate -> {
                ingressToUpdate.getSpec().getRules().removeIf(rule -> {
                    if (rule.getHttp() == null || rule.getHost() == null) {
                        return false;
                    }
                    if (!hosts.contains(rule.getHost())) {
                        return false;
                    }
                    return rule.getHttp().getPaths().stream()
                            .anyMatch(httpPath -> fullPath.equals(httpPath.getPath()));
                });
                return ingressToUpdate;
            });
            LOGGER.info(formatLogMessage(correlationId,
                    "Removed ingress rules for path " + path + " from " + ingress.getMetadata().getName()));
            return true;
        } catch (KubernetesClientException e) {
            LOGGER.error(formatLogMessage(correlationId,
                    "Failed to remove ingress rules from " + ingress.getMetadata().getName()), e);
            throw e;
        }
    }

    /**
     * Creates an IngressRule object.
     */
    private IngressRule createIngressRule(String host, String serviceName, int port, String path) {
        IngressRule rule = new IngressRule();
        rule.setHost(host);

        HTTPIngressRuleValue http = new HTTPIngressRuleValue();
        rule.setHttp(http);

        HTTPIngressPath httpPath = new HTTPIngressPath();
        http.setPaths(Collections.singletonList(httpPath));
        httpPath.setPath(path + AddedHandlerUtil.INGRESS_REWRITE_PATH);
        httpPath.setPathType(AddedHandlerUtil.INGRESS_PATH_TYPE);

        IngressBackend backend = new IngressBackend();
        httpPath.setBackend(backend);

        IngressServiceBackend serviceBackend = new IngressServiceBackend();
        backend.setService(serviceBackend);
        serviceBackend.setName(serviceName);

        ServiceBackendPort servicePort = new ServiceBackendPort();
        serviceBackend.setPort(servicePort);
        servicePort.setNumber(port);

        return rule;
    }
}

