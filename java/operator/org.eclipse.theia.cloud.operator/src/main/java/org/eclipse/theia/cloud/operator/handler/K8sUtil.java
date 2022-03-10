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
package org.eclipse.theia.cloud.operator.handler;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.PersistentVolume;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimList;
import io.fabric8.kubernetes.api.model.PersistentVolumeList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public final class K8sUtil {

    private static final Logger LOGGER = LogManager.getLogger(K8sUtil.class);

    private static final String DEPLOYMENT = "Deployment";
    private static final String CONFIG_MAP = "ConfigMap";
    private static final String SERVICE = "Service";
    private static final String INGRESS = "Ingress";

    private K8sUtil() {
    }

    /**
     * make sure string has max length of 62 and starts and ends with an
     * alphanumeric character
     */
    public static String validString(String originalString) {
	String string;
	if (originalString.length() <= 60) {
	    string = originalString;
	} else {
	    string = originalString.substring(originalString.length() - 60, originalString.length());
	}
	return "t" + string.replace(".", "-") + "c";
    }

    public static Optional<Ingress> getExistingIngress(DefaultKubernetesClient client, String namespace,
	    String ingressName) {
	return client.network().v1().ingresses().inNamespace(namespace).list().getItems().stream()//
		.filter(ingress -> ingressName.equals(ingress.getMetadata().getName()))//
		.findAny();
    }

    public static Optional<Ingress> getExistingIngress(DefaultKubernetesClient client, String namespace,
	    String ownerName, String ownerUid) {
	return getExistingTypesStream(client, namespace, ownerName, ownerUid,
		client.network().v1().ingresses().list().getItems())//
			.findAny();
    }

    public static List<Service> getExistingServices(DefaultKubernetesClient client, String namespace, String ownerName,
	    String ownerUid) {
	return getExistingTypes(client, namespace, ownerName, ownerUid,
		client.services().inNamespace(namespace).list().getItems());
    }

    public static List<Deployment> getExistingDeployments(DefaultKubernetesClient client, String namespace,
	    String ownerName, String ownerUid) {
	return getExistingTypes(client, namespace, ownerName, ownerUid,
		client.apps().deployments().inNamespace(namespace).list().getItems());
    }

    public static List<ConfigMap> getExistingConfigMaps(DefaultKubernetesClient client, String namespace,
	    String ownerName, String ownerUid) {
	return getExistingTypes(client, namespace, ownerName, ownerUid,
		client.configMaps().inNamespace(namespace).list().getItems());
    }

    public static Optional<PersistentVolumeClaim> getPersistentVolumeClaim(DefaultKubernetesClient client,
	    String namespace, String volumeName) {
	Resource<PersistentVolumeClaim> pvc = client.persistentVolumeClaims().inNamespace(namespace)
		.withName(volumeName);
	return pvc.get() == null ? Optional.empty() : Optional.of(pvc.get());
    }

    private static <T extends HasMetadata> List<T> getExistingTypes(DefaultKubernetesClient client, String namespace,
	    String ownerName, String ownerUid, List<T> items) {
	return getExistingTypesStream(client, namespace, ownerName, ownerUid, items)//
		.collect(Collectors.toList());
    }

    private static <T extends HasMetadata> Stream<T> getExistingTypesStream(DefaultKubernetesClient client,
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

    public static Optional<Ingress> loadAndCreateIngressWithOwnerReference(DefaultKubernetesClient client,
	    String namespace, String correlationId, String yaml, String ownerAPIVersion, String ownerKind,
	    String ownerName, String ownerUid, int ownerReferenceIndex) {
	return loadAndCreateTypeWithOwnerReference(client, namespace, correlationId, yaml, ownerAPIVersion, ownerKind,
		ownerName, ownerUid, ownerReferenceIndex, INGRESS,
		client.network().v1().ingresses().inNamespace(namespace), item -> {
		});
    }

    public static Optional<Service> loadAndCreateServiceWithOwnerReference(DefaultKubernetesClient client,
	    String namespace, String correlationId, String yaml, String ownerAPIVersion, String ownerKind,
	    String ownerName, String ownerUid, int ownerReferenceIndex) {
	return loadAndCreateTypeWithOwnerReference(client, namespace, correlationId, yaml, ownerAPIVersion, ownerKind,
		ownerName, ownerUid, ownerReferenceIndex, SERVICE, client.services().inNamespace(namespace), item -> {
		});
    }

    public static Optional<ConfigMap> loadAndCreateConfigMapWithOwnerReference(DefaultKubernetesClient client,
	    String namespace, String correlationId, String yaml, String ownerAPIVersion, String ownerKind,
	    String ownerName, String ownerUid, int ownerReferenceIndex) {
	return loadAndCreateTypeWithOwnerReference(client, namespace, correlationId, yaml, ownerAPIVersion, ownerKind,
		ownerName, ownerUid, ownerReferenceIndex, CONFIG_MAP, client.configMaps().inNamespace(namespace),
		item -> {
		});
    }

    public static Optional<Deployment> loadAndCreateDeploymentWithOwnerReference(DefaultKubernetesClient client,
	    String namespace, String correlationId, String yaml, String ownerAPIVersion, String ownerKind,
	    String ownerName, String ownerUid, int ownerReferenceIndex, Consumer<Deployment> additionalModification) {
	return loadAndCreateTypeWithOwnerReference(client, namespace, correlationId, yaml, ownerAPIVersion, ownerKind,
		ownerName, ownerUid, ownerReferenceIndex, DEPLOYMENT,
		client.apps().deployments().inNamespace(namespace), additionalModification);
    }

    public static Optional<PersistentVolumeClaim> loadAndCreatePersistentVolumeClaim(DefaultKubernetesClient client,
	    String namespace, String correlationId, String yaml) {
	NonNamespaceOperation<PersistentVolumeClaim, PersistentVolumeClaimList, Resource<PersistentVolumeClaim>> items = client
		.persistentVolumeClaims().inNamespace(namespace);
	try (ByteArrayInputStream inputStream = new ByteArrayInputStream(yaml.getBytes())) {
	    LOGGER.trace(formatLogMessage(correlationId, "Loading new PersistentVolumeClaim:\n" + yaml));
	    PersistentVolumeClaim newItem = items.load(inputStream).get();

	    LOGGER.trace(formatLogMessage(correlationId, "Creating new PersistentVolumeClaim"));
	    items.create(newItem);
	    LOGGER.info(formatLogMessage(correlationId, "Created a new PersistentVolumeClaim"));

	    return Optional.of(newItem);
	} catch (IOException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while reading yaml byte stream"), e);
	}
	return Optional.empty();
    }

    public static Optional<PersistentVolume> loadAndCreatePersistentVolume(DefaultKubernetesClient client,
	    String namespace, String correlationId, String yaml) {
	NonNamespaceOperation<PersistentVolume, PersistentVolumeList, Resource<PersistentVolume>> items = client
		.persistentVolumes();
	try (ByteArrayInputStream inputStream = new ByteArrayInputStream(yaml.getBytes())) {
	    LOGGER.trace(formatLogMessage(correlationId, "Loading new PersistentVolume:\n" + yaml));
	    PersistentVolume newItem = items.load(inputStream).get();

	    LOGGER.trace(formatLogMessage(correlationId, "Creating new PersistentVolume"));
	    items.create(newItem);
	    LOGGER.info(formatLogMessage(correlationId, "Created a new PersistentVolume"));

	    return Optional.of(newItem);
	} catch (IOException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while reading yaml byte stream"), e);
	}
	return Optional.empty();
    }

    public static Optional<ConfigMap> loadAndCreateConfigMapWithOwnerReference(DefaultKubernetesClient client,
	    String namespace, String correlationId, String yaml, String ownerAPIVersion, String ownerKind,
	    String ownerName, String ownerUid, int ownerReferenceIndex, Consumer<ConfigMap> additionalModification) {
	return loadAndCreateTypeWithOwnerReference(client, namespace, correlationId, yaml, ownerAPIVersion, ownerKind,
		ownerName, ownerUid, ownerReferenceIndex, CONFIG_MAP, client.configMaps().inNamespace(namespace),
		additionalModification);
    }

    private static <T extends HasMetadata, U, V extends Resource<T>> Optional<T> loadAndCreateTypeWithOwnerReference(
	    DefaultKubernetesClient client, String namespace, String correlationId, String yaml, String ownerAPIVersion,
	    String ownerKind, String ownerName, String ownerUid, int ownerReferenceIndex, String typeName,
	    NonNamespaceOperation<T, U, V> items, Consumer<T> additionalModification) {

	try (ByteArrayInputStream inputStream = new ByteArrayInputStream(yaml.getBytes())) {

	    LOGGER.trace(formatLogMessage(correlationId, "Loading new " + typeName + ":\n" + yaml));
	    T newItem = items.load(inputStream).get();

	    if (newItem.getMetadata().getOwnerReferences().size() > ownerReferenceIndex && ownerReferenceIndex >= 0) {
		LOGGER.trace(
			formatLogMessage(correlationId, "Updating owner reference at index " + ownerReferenceIndex));
		newItem.getMetadata().getOwnerReferences().get(ownerReferenceIndex).setApiVersion(ownerAPIVersion);
		newItem.getMetadata().getOwnerReferences().get(ownerReferenceIndex).setKind(ownerKind);
		newItem.getMetadata().getOwnerReferences().get(ownerReferenceIndex).setUid(ownerUid);
		newItem.getMetadata().getOwnerReferences().get(ownerReferenceIndex).setName(ownerName);
	    } else {
		LOGGER.trace(formatLogMessage(correlationId, "No owner reference at index " + ownerReferenceIndex));
	    }

	    additionalModification.accept(newItem);

	    LOGGER.trace(formatLogMessage(correlationId, "Creating new " + typeName));
	    items.create(newItem);
	    LOGGER.info(formatLogMessage(correlationId, "Created a new " + typeName));

	    return Optional.of(newItem);
	} catch (IOException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while reading yaml byte stream"), e);
	}
	return Optional.empty();
    }

}
