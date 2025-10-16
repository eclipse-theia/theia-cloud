/********************************************************************************
 * Copyright (C) 2022 EclipseSource, Lockular, Ericsson, STMicroelectronics and 
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
package org.eclipse.theia.cloud.operator.util;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.util.JavaUtil;
import org.eclipse.theia.cloud.operator.handler.AddedHandlerUtil;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressRuleValue;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRule;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;

public final class TheiaCloudIngressUtil {

    private static final Logger LOGGER = LogManager.getLogger(TheiaCloudIngressUtil.class);

    private TheiaCloudIngressUtil() {
    }

    public static boolean checkForExistingIngressAndAddOwnerReferencesIfMissing(NamespacedKubernetesClient client,
            String namespace, AppDefinition appDefinition, String correlationId) {
        Optional<Ingress> existingIngressWithParentAppDefinition = K8sUtil.getExistingIngress(client, namespace,
                appDefinition.getMetadata().getName(), appDefinition.getMetadata().getUid());
        if (existingIngressWithParentAppDefinition.isPresent()) {
            return true;
        }
        Optional<Ingress> ingress = K8sUtil.getExistingIngress(client, namespace,
                appDefinition.getSpec().getIngressname());
        if (ingress.isPresent()) {
            OwnerReference ownerReference = new OwnerReference();
            ownerReference.setApiVersion(HasMetadata.getApiVersion(AppDefinition.class));
            ownerReference.setKind(AppDefinition.KIND);
            ownerReference.setName(appDefinition.getMetadata().getName());
            ownerReference.setUid(appDefinition.getMetadata().getUid());
            addOwnerReferenceToIngress(client, namespace, ingress.get(), ownerReference);
        }
        return ingress.isPresent();
    }

    public static String getIngressName(AppDefinition appDefinition) {
        return appDefinition.getSpec().getIngressname();
    }

    public static void addOwnerReferenceToIngress(NamespacedKubernetesClient client, String namespace, Ingress ingress,
            OwnerReference ownerReference) {
        client.network().v1().ingresses().inNamespace(namespace).withName(ingress.getMetadata().getName()).edit(JavaUtil
                .toUnary(ingressToEdit -> ingressToEdit.getMetadata().getOwnerReferences().add(ownerReference)));
    }

    public static void removeIngressRule(NamespacedKubernetesClient client, String namespace, Ingress ingress,
            String path, String correlationId) {
        client.network().v1().ingresses().inNamespace(namespace).withName(ingress.getMetadata().getName())
                .edit(JavaUtil.toUnary(ingressToEdit -> removeIngressRule(ingressToEdit, path, correlationId)));
    }

    private static void removeIngressRule(Ingress ingressToEdit, String path, String correlationId) {
        String ingressPath = path + AddedHandlerUtil.INGRESS_REWRITE_PATH;
        IngressRule ruleToDelete = null;
        for (IngressRule rule : ingressToEdit.getSpec().getRules()) {
            HTTPIngressRuleValue ingressRuleValue = rule.getHttp();
            if (ingressRuleValue == null) {
                continue;
            }
            for (HTTPIngressPath httpIngressPath : ingressRuleValue.getPaths()) {
                if (ingressPath.equals(httpIngressPath.getPath())) {
                    ruleToDelete = rule;
                    break;
                } else {
                    LOGGER.trace(formatLogMessage(correlationId, httpIngressPath.getPath() + " is NOT " + ingressPath));
                }
            }
        }
        if (ruleToDelete != null) {
            LOGGER.info(formatLogMessage(correlationId, "Removing ingress rule for path " + path));
            ingressToEdit.getSpec().getRules().remove(ruleToDelete);
        } else {
            LOGGER.warn(formatLogMessage(correlationId, "No ingress rule found to remove for path " + path));
        }
    }

    /**
     * Removes ingress rules for a specific path across all specified hosts. This enables cleaning up all ingress rules
     * created during session initialization, including rules for additional hostname prefixes.
     * 
     * @param client        the Kubernetes client
     * @param namespace     the namespace
     * @param ingress       the ingress resource to modify
     * @param path          the path to remove (without the rewrite suffix)
     * @param hosts         the list of hosts for which to remove rules
     * @param correlationId the correlation ID for logging
     * @return true if at least one rule was removed, false otherwise
     */
    public static boolean removeIngressRules(NamespacedKubernetesClient client, String namespace, Ingress ingress,
            String path, List<String> hosts, String correlationId) {
        AtomicInteger removedCount = new AtomicInteger(0);

        try {
            client.network().v1().ingresses().inNamespace(namespace).withName(ingress.getMetadata().getName())
                    .edit(JavaUtil.toUnary(ingressToEdit -> {
                        int count = removeIngressRules(ingressToEdit, path, hosts, correlationId);
                        removedCount.set(count);
                    }));
        } catch (Exception e) {
            LOGGER.error(formatLogMessage(correlationId,
                    "Error while removing ingress rules for path " + path + " across " + hosts.size() + " hosts"), e);
            return false;
        }

        return removedCount.get() > 0;
    }

    /**
     * Internal method to remove ingress rules for all given hosts during an edit operation.
     * 
     * @param ingressToEdit the ingress being edited
     * @param path          the path to remove (without the rewrite suffix)
     * @param hosts         the list of hosts for which to remove rules
     * @param correlationId the correlation ID for logging
     * @return the number of rules removed
     */
    private static int removeIngressRules(Ingress ingressToEdit, String path, List<String> hosts,
            String correlationId) {
        String ingressPath = path + AddedHandlerUtil.INGRESS_REWRITE_PATH;
        AtomicInteger removedCount = new AtomicInteger(0);

        // Remove rules matching the path across all specified hosts
        ingressToEdit.getSpec().getRules().removeIf(rule -> {
            HTTPIngressRuleValue ingressRuleValue = rule.getHttp();
            if (ingressRuleValue == null) {
                return false;
            }

            // Check if this rule is for one of our hosts
            boolean isOurHost = hosts.contains(rule.getHost());
            if (!isOurHost) {
                return false;
            }

            // Check if this rule has our path
            boolean hasOurPath = ingressRuleValue.getPaths().stream()
                    .anyMatch(httpIngressPath -> ingressPath.equals(httpIngressPath.getPath()));

            if (hasOurPath) {
                LOGGER.info(formatLogMessage(correlationId,
                        "Removing ingress rule for host " + rule.getHost() + " and path " + path));
                removedCount.incrementAndGet();
                return true;
            }
            return false;
        });

        if (removedCount.get() == 0) {
            LOGGER.warn(formatLogMessage(correlationId, "No ingress rules found to remove for path " + path + " across "
                    + hosts.size() + " hosts: " + hosts));
        } else {
            LOGGER.info(formatLogMessage(correlationId,
                    "Removed " + removedCount.get() + " ingress rule(s) for path " + path));
        }

        return removedCount.get();
    }

}
