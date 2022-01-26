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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;

public final class K8sUtil {

    private K8sUtil() {
    }

    public static Optional<Ingress> getExistingIngress(DefaultKubernetesClient client, String namespace,
	    String templateResourceName, String templateResourceUID) {
	return client.network().v1().ingresses().list().getItems().stream()//
		.filter(ingress -> hasThisTemplateOwnerReference(ingress.getMetadata().getOwnerReferences(),
			templateResourceUID, templateResourceName))//
		.findAny();
    }

    public static List<Service> getExistingServices(DefaultKubernetesClient client, String namespace,
	    String templateResourceName, String templateResourceUID) {
	return client.services().inNamespace(namespace).list().getItems().stream()//
		.filter(service -> hasThisTemplateOwnerReference(service.getMetadata().getOwnerReferences(),
			templateResourceUID, templateResourceName))//
		.collect(Collectors.toList());
    }

    public static List<Deployment> getExistingDeployments(DefaultKubernetesClient client, String namespace,
	    String templateResourceName, String templateResourceUID) {
	return client.apps().deployments().inNamespace(namespace).list().getItems().stream()//
		.filter(deployment -> hasThisTemplateOwnerReference(deployment.getMetadata().getOwnerReferences(),
			templateResourceUID, templateResourceName))//
		.collect(Collectors.toList());
    }

    public static List<ConfigMap> getExistingConfigMaps(DefaultKubernetesClient client, String namespace,
	    String templateResourceName, String templateResourceUID) {
	return client.configMaps().inNamespace(namespace).list().getItems().stream()//
		.filter(configMap -> hasThisTemplateOwnerReference(configMap.getMetadata().getOwnerReferences(),
			templateResourceUID, templateResourceName))//
		.collect(Collectors.toList());
    }

    private static boolean hasThisTemplateOwnerReference(List<OwnerReference> ownerReferences,
	    String templateResourceUID, String templateResourceName) {
	for (OwnerReference ownerReference : ownerReferences) {
	    if (templateResourceUID.equals(ownerReference.getUid())
		    && templateResourceName.equals(ownerReference.getName())) {
		return true;
	    }
	}
	return false;
    }

}
