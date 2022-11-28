/********************************************************************************
 * Copyright (C) 2022 EclipseSource and others.
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
package org.eclipse.theia.cloud.common.k8s.client;

import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.PersistentVolume;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimList;
import io.fabric8.kubernetes.api.model.PersistentVolumeList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;

public interface TheiaCloudClient {
    TheiaCloudClient inNamespace(String namespace);

    String namespace();

    NamespacedKubernetesClient kubernetes();

    WorkspaceResourceClient workspaces();

    SessionResourceClient sessions();

    AppDefinitionResourceClient appDefinitions();

    default ResourceClient<PersistentVolumeClaim, PersistentVolumeClaimList> persistentVolumeClaims() {
	return client(PersistentVolumeClaim.class, PersistentVolumeClaimList.class);
    }

    default ResourceClient<PersistentVolume, PersistentVolumeList> persistentVolumes() {
	return client(PersistentVolume.class, PersistentVolumeList.class);
    }

    default ResourceClient<Ingress, IngressList> ingresses() {
	return client(kubernetes().network().v1().ingresses(), Ingress.class);
    }

    @SuppressWarnings("unchecked")
    default <T extends HasMetadata> ResourceClient<T, ? extends KubernetesResourceList<T>> client(Class<T> typeClass) {
	return client(typeClass,
		(Class<? extends KubernetesResourceList<T>>) KubernetesResourceUtil.inferListType(typeClass));
    }

    default <T extends HasMetadata, L extends KubernetesResourceList<T>> ResourceClient<T, L> client(Class<T> typeClass,
	    Class<L> listClass) {
	return new BaseResourceClient<T, L>(kubernetes(), typeClass, listClass);
    }

    default <T extends HasMetadata, L extends KubernetesResourceList<T>> ResourceClient<T, L> client(
	    NonNamespaceOperation<T, L, Resource<T>> operation, Class<T> typeClass) {
	return new BaseResourceClient<T, L>(kubernetes(), operation, typeClass);
    }

    default Optional<String> getClusterIPFromSessionName(String sessionName) {
	try (final KubernetesClient client = new DefaultKubernetesClient()) {
	    ServiceList svcList = client.services().inNamespace(namespace()).list();
	    for (Service svc : svcList.getItems()) {
		Map<String, String> labels = svc.getMetadata().getLabels();
		if (labels != null && sessionName.equals(labels.get("app"))) {
		    return Optional.of(svc.getSpec().getClusterIP());
		}
	    }
	}
	return Optional.empty();
    }
}
