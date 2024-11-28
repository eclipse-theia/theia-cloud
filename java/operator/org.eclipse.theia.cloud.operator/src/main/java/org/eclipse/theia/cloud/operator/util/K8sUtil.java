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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.resource.ResourceEdit;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.utils.Serialization;

public final class K8sUtil {

    private static final Logger LOGGER = LogManager.getLogger(K8sUtil.class);

    private static final String DEPLOYMENT = "Deployment";
    private static final String CONFIG_MAP = "ConfigMap";
    private static final String SERVICE = "Service";
    private static final String INGRESS = "Ingress";

    private K8sUtil() {
    }

    public static Optional<Ingress> getExistingIngress(NamespacedKubernetesClient client, String namespace,
            String ingressName) {
        return client.network().v1().ingresses().inNamespace(namespace).list().getItems().stream()//
                .filter(ingress -> ingressName.equals(ingress.getMetadata().getName()))//
                .findAny();
    }

    public static Optional<Ingress> getExistingIngress(NamespacedKubernetesClient client, String namespace,
            String ownerName, String ownerUid) {
        return getExistingTypesStream(client, namespace, ownerName, ownerUid,
                client.network().v1().ingresses().list().getItems())//
                        .findAny();
    }

    public static List<Service> getExistingServices(NamespacedKubernetesClient client, String namespace,
            String ownerName, String ownerUid) {
        return getExistingTypes(client, namespace, ownerName, ownerUid,
                client.services().inNamespace(namespace).list().getItems());
    }

    public static List<Deployment> getExistingDeployments(NamespacedKubernetesClient client, String namespace,
            String ownerName, String ownerUid) {
        return getExistingTypes(client, namespace, ownerName, ownerUid,
                client.apps().deployments().inNamespace(namespace).list().getItems());
    }

    public static List<ConfigMap> getExistingConfigMaps(NamespacedKubernetesClient client, String namespace,
            String ownerName, String ownerUid) {
        return getExistingTypes(client, namespace, ownerName, ownerUid,
                client.configMaps().inNamespace(namespace).list().getItems());
    }

    private static <T extends HasMetadata> List<T> getExistingTypes(NamespacedKubernetesClient client, String namespace,
            String ownerName, String ownerUid, List<T> items) {
        return getExistingTypesStream(client, namespace, ownerName, ownerUid, items)//
                .collect(Collectors.toList());
    }

    private static <T extends HasMetadata> Stream<T> getExistingTypesStream(NamespacedKubernetesClient client,
            String namespace, String ownerName, String ownerUid, List<T> items) {
        return items.stream()//
                .filter(item -> hasThisTemplateOwnerReference(item.getMetadata().getOwnerReferences(), ownerUid,
                        ownerName));
    }

    private static boolean hasThisTemplateOwnerReference(List<OwnerReference> ownerReferences, String ownerUid,
            String ownerName) {
        for (OwnerReference ownerReference : ownerReferences) {
            if (ownerUid.equals(ownerReference.getUid()) && ownerName.equals(ownerReference.getName())) {
                return true;
            }
        }
        return false;
    }

    public static Optional<Ingress> loadAndCreateIngressWithOwnerReference(NamespacedKubernetesClient client,
            String namespace, String correlationId, String yaml, String ownerAPIVersion, String ownerKind,
            String ownerName, String ownerUid, int ownerReferenceIndex, Map<String, String> labelsToAdd) {
        return loadAndCreateTypeWithOwnerReference(client, namespace, correlationId, yaml, ownerAPIVersion, ownerKind,
                ownerName, ownerUid, ownerReferenceIndex, INGRESS,
                client.network().v1().ingresses().inNamespace(namespace), labelsToAdd, item -> {
                });
    }

    public static Optional<Service> loadAndCreateServiceWithOwnerReference(NamespacedKubernetesClient client,
            String namespace, String correlationId, String yaml, String ownerAPIVersion, String ownerKind,
            String ownerName, String ownerUid, int ownerReferenceIndex, Map<String, String> labelsToAdd) {
        return loadAndCreateTypeWithOwnerReference(client, namespace, correlationId, yaml, ownerAPIVersion, ownerKind,
                ownerName, ownerUid, ownerReferenceIndex, SERVICE, client.services().inNamespace(namespace),
                labelsToAdd, item -> {
                });
    }

    public static Optional<ConfigMap> loadAndCreateConfigMapWithOwnerReference(NamespacedKubernetesClient client,
            String namespace, String correlationId, String yaml, String ownerAPIVersion, String ownerKind,
            String ownerName, String ownerUid, int ownerReferenceIndex, Map<String, String> labelsToAdd) {
        return loadAndCreateTypeWithOwnerReference(client, namespace, correlationId, yaml, ownerAPIVersion, ownerKind,
                ownerName, ownerUid, ownerReferenceIndex, CONFIG_MAP, client.configMaps().inNamespace(namespace),
                labelsToAdd, item -> {
                });
    }

    public static Optional<Deployment> loadAndCreateDeploymentWithOwnerReference(NamespacedKubernetesClient client,
            String namespace, String correlationId, String yaml, String ownerAPIVersion, String ownerKind,
            String ownerName, String ownerUid, int ownerReferenceIndex, Map<String, String> labelsToAdd,
            Consumer<Deployment> additionalModification) {
        return loadAndCreateTypeWithOwnerReference(client, namespace, correlationId, yaml, ownerAPIVersion, ownerKind,
                ownerName, ownerUid, ownerReferenceIndex, DEPLOYMENT,
                client.apps().deployments().inNamespace(namespace), labelsToAdd, additionalModification);
    }

    public static Optional<ConfigMap> loadAndCreateConfigMapWithOwnerReference(NamespacedKubernetesClient client,
            String namespace, String correlationId, String yaml, String ownerAPIVersion, String ownerKind,
            String ownerName, String ownerUid, int ownerReferenceIndex, Map<String, String> labelsToAdd,
            Consumer<ConfigMap> additionalModification) {
        return loadAndCreateTypeWithOwnerReference(client, namespace, correlationId, yaml, ownerAPIVersion, ownerKind,
                ownerName, ownerUid, ownerReferenceIndex, CONFIG_MAP, client.configMaps().inNamespace(namespace),
                labelsToAdd, additionalModification);
    }

    private static <T extends HasMetadata, U, V extends Resource<T>> Optional<T> loadAndCreateTypeWithOwnerReference(
            NamespacedKubernetesClient client, String namespace, String correlationId, String yaml,
            String ownerAPIVersion, String ownerKind, String ownerName, String ownerUid, int ownerReferenceIndex,
            String typeName, NonNamespaceOperation<T, U, V> items, Map<String, String> labelsToAdd,
            Consumer<T> additionalModification) {

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(yaml.getBytes())) {

            LOGGER.trace(formatLogMessage(correlationId, "Loading new " + typeName + ":\n" + yaml));
            T newItem = items.load(inputStream).item();
            if (newItem == null) {
                LOGGER.error(formatLogMessage(correlationId, "Loading new " + typeName + " resulted in null object"));
                return Optional.empty();
            }

            // Apply labels to the resource metadata
            if (newItem.getMetadata().getLabels() == null) {
                newItem.getMetadata().setLabels(new HashMap<>());
            }
            newItem.getMetadata().getLabels().putAll(labelsToAdd);

            // If the resource is a Deployment, also apply labels to the pod template metadata
            if (newItem instanceof Deployment deployment) {
                if (deployment.getSpec().getTemplate().getMetadata().getLabels() == null) {
                    deployment.getSpec().getTemplate().getMetadata().setLabels(new HashMap<>());
                }
                deployment.getSpec().getTemplate().getMetadata().getLabels().putAll(labelsToAdd);
            }

            ResourceEdit.<T> updateOwnerReference(ownerReferenceIndex, ownerAPIVersion, ownerKind, ownerName, ownerUid,
                    correlationId).andThen(additionalModification).accept(newItem);

            String resultingYaml;
            try {
                resultingYaml = Serialization.asYaml(newItem);
            } catch (Exception e) {
                resultingYaml = "Serializing " + typeName + " to Yaml failed.";
            }

            LOGGER.trace(formatLogMessage(correlationId, "Creating new " + typeName + ":\n" + resultingYaml));
            items.resource(newItem).create();
            LOGGER.info(formatLogMessage(correlationId, "Created a new " + typeName));

            return Optional.of(newItem);
        } catch (IOException e) {
            LOGGER.error(formatLogMessage(correlationId, "Error while reading yaml byte stream"), e);
        }
        return Optional.empty();
    }

}
