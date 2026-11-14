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
import static org.eclipse.theia.cloud.operator.util.TheiaCloudDeploymentUtil.HOST_PROTOCOL;

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
import org.eclipse.theia.cloud.operator.ingress.IngressPathProvider;
import org.eclipse.theia.cloud.operator.util.K8sUtil;
import org.eclipse.theia.cloud.operator.util.TheiaCloudIngressUtil;

import com.google.inject.Inject;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressRuleValue;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBackend;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRule;
import io.fabric8.kubernetes.api.model.networking.v1.IngressServiceBackend;
import io.fabric8.kubernetes.api.model.networking.v1.ServiceBackendPort;

/**
 * Ingress-based implementation of {@link SessionRoutingStrategy} for vanilla Kubernetes clusters.
 * <p>
 * This strategy manages IngressRules on a shared Ingress resource. Rules are added when sessions start and removed when
 * sessions are deleted.
 */
public class IngressRoutingStrategy implements SessionRoutingStrategy {

    private static final Logger LOGGER = LogManager.getLogger(IngressRoutingStrategy.class);

    @Inject
    private TheiaCloudClient client;

    @Inject
    private IngressPathProvider ingressPathProvider;

    @Inject
    private TheiaCloudOperatorArguments arguments;

    @Override
    public boolean ensureRoutingResourceExists(AppDefinition appDefinition, String correlationId) {
        return TheiaCloudIngressUtil.checkForExistingIngressAndAddOwnerReferencesIfMissing(client.kubernetes(),
                client.namespace(), appDefinition, correlationId);
    }

    @Override
    public synchronized String addSessionRouting(Session session, AppDefinition appDefinition, Service service,
            String correlationId) {
        Optional<Ingress> ingress = getIngress(appDefinition, correlationId);
        if (ingress.isEmpty()) {
            LOGGER.error(formatLogMessage(correlationId,
                    "No Ingress for app definition " + appDefinition.getSpec().getName() + " found."));
            return null;
        }
        String path = ingressPathProvider.getPath(appDefinition, session);
        return addIngressRulesForSession(ingress.get(), service, appDefinition, path, correlationId);
    }

    @Override
    public synchronized String addSessionRouting(Session session, AppDefinition appDefinition, Service service,
            int instance, String correlationId) {
        Optional<Ingress> ingress = getIngress(appDefinition, correlationId);
        if (ingress.isEmpty()) {
            LOGGER.error(formatLogMessage(correlationId,
                    "No Ingress for app definition " + appDefinition.getSpec().getName() + " found."));
            return null;
        }
        String path = ingressPathProvider.getPath(appDefinition, instance);
        addSingleIngressRule(ingress.get(), service, appDefinition.getSpec().getPort(), path, correlationId);
        return HOST_PROTOCOL + arguments.getInstancesHost() + path + "/";
    }

    @Override
    public synchronized boolean removeSessionRouting(Session session, AppDefinition appDefinition,
            String correlationId) {
        Optional<Ingress> ingress = getIngress(appDefinition, correlationId);
        if (ingress.isEmpty()) {
            LOGGER.error(formatLogMessage(correlationId,
                    "No Ingress for app definition " + appDefinition.getSpec().getName() + " found."));
            return false;
        }

        String path = ingressPathProvider.getPath(appDefinition, session);

        List<String> hostsToClean = new ArrayList<>();
        final String instancesHost = arguments.getInstancesHost();
        hostsToClean.add(instancesHost);
        List<String> ingressHostnamePrefixes = appDefinition.getSpec().getIngressHostnamePrefixes();
        if (ingressHostnamePrefixes != null) {
            for (String prefix : ingressHostnamePrefixes) {
                hostsToClean.add(prefix + instancesHost);
            }
        }

        boolean cleanupSuccess = TheiaCloudIngressUtil.removeIngressRules(client.kubernetes(), client.namespace(),
                ingress.get(), path, arguments.getIngressPathSuffix(), hostsToClean, correlationId);

        if (!cleanupSuccess) {
            LOGGER.error(formatLogMessage(correlationId,
                    "Failed to remove ingress rules for session " + session.getSpec().getName()));
        }

        return cleanupSuccess;
    }

    @Override
    public synchronized boolean removeSessionRouting(Session session, AppDefinition appDefinition, int instance,
            String correlationId) {
        Optional<Ingress> ingress = getIngress(appDefinition, correlationId);
        if (ingress.isEmpty()) {
            LOGGER.error(formatLogMessage(correlationId,
                    "No Ingress for app definition " + appDefinition.getSpec().getName() + " found."));
            return false;
        }

        String ruleHttpPath = ingressPathProvider.getPath(appDefinition, instance) + arguments.getIngressPathSuffix();
        client.ingresses().resource(ingress.get().getMetadata().getName()).edit(ingressToUpdate -> {
            ingressToUpdate.getSpec().getRules().removeIf(rule -> {
                if (rule.getHttp() == null) {
                    LOGGER.warn(formatLogMessage(correlationId,
                            "Error while removing ingress rule: The rule's HTTP block is null"));
                    return false;
                }
                return rule.getHttp().getPaths().stream().anyMatch(httpPath -> ruleHttpPath.equals(httpPath.getPath()));
            });
            return ingressToUpdate;
        });

        return true;
    }

    private Optional<Ingress> getIngress(AppDefinition appDefinition, String correlationId) {
        String appDefinitionResourceName = appDefinition.getMetadata().getName();
        String appDefinitionResourceUID = appDefinition.getMetadata().getUid();
        return K8sUtil.getExistingIngress(client.kubernetes(), client.namespace(), appDefinitionResourceName,
                appDefinitionResourceUID);
    }

    private String addIngressRulesForSession(Ingress ingress, Service service, AppDefinition appDefinition, String path,
            String correlationId) {
        List<String> hostsToAdd = new ArrayList<>();
        final String instancesHost = arguments.getInstancesHost();
        hostsToAdd.add(instancesHost);
        List<String> ingressHostnamePrefixes = appDefinition.getSpec().getIngressHostnamePrefixes() != null
                ? appDefinition.getSpec().getIngressHostnamePrefixes()
                : Collections.emptyList();
        for (String prefix : ingressHostnamePrefixes) {
            hostsToAdd.add(prefix + instancesHost);
        }

        int port = appDefinition.getSpec().getPort();
        String serviceName = service.getMetadata().getName();
        client.ingresses().edit(correlationId, ingress.getMetadata().getName(), ingressToUpdate -> {
            for (String host : hostsToAdd) {
                addIngressRule(ingressToUpdate, host, serviceName, port, path);
            }
        });

        return HOST_PROTOCOL + instancesHost + path + "/";
    }

    private void addSingleIngressRule(Ingress ingress, Service service, int port, String path, String correlationId) {
        final String host = arguments.getInstancesHost();
        String serviceName = service.getMetadata().getName();
        client.ingresses().edit(correlationId, ingress.getMetadata().getName(),
                ingressToUpdate -> addIngressRule(ingressToUpdate, host, serviceName, port, path));
    }

    private void addIngressRule(Ingress ingress, String host, String serviceName, int port, String path) {
        IngressRule ingressRule = new IngressRule();
        ingress.getSpec().getRules().add(ingressRule);
        ingressRule.setHost(host);

        HTTPIngressRuleValue http = new HTTPIngressRuleValue();
        ingressRule.setHttp(http);

        HTTPIngressPath httpIngressPath = new HTTPIngressPath();
        http.getPaths().add(httpIngressPath);
        httpIngressPath.setPath(path + arguments.getIngressPathSuffix());
        httpIngressPath.setPathType(AddedHandlerUtil.INGRESS_PATH_TYPE);

        IngressBackend ingressBackend = new IngressBackend();
        httpIngressPath.setBackend(ingressBackend);

        IngressServiceBackend ingressServiceBackend = new IngressServiceBackend();
        ingressBackend.setService(ingressServiceBackend);
        ingressServiceBackend.setName(serviceName);

        ServiceBackendPort serviceBackendPort = new ServiceBackendPort();
        ingressServiceBackend.setPort(serviceBackendPort);
        serviceBackendPort.setNumber(port);
    }

    @Override
    public String getSessionURL(AppDefinition appDefinition, Session session) {
        String path = ingressPathProvider.getPath(appDefinition, session);
        return HOST_PROTOCOL + arguments.getInstancesHost() + path + "/";
    }

    @Override
    public String getSessionURL(AppDefinition appDefinition, int instance) {
        String path = ingressPathProvider.getPath(appDefinition, instance);
        return HOST_PROTOCOL + arguments.getInstancesHost() + path + "/";
    }
}
