/********************************************************************************
 * Copyright (C) 2022-2023 EclipseSource, Lockular, Ericsson, STMicroelectronics and 
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
package org.eclipse.theia.cloud.common.util;

import java.util.Objects;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;

public final class CustomResourceUtil {

    private CustomResourceUtil() {
    }

    public static NamespacedKubernetesClient createClient() {
	return createClient(new ConfigBuilder().build());
    }

    public static NamespacedKubernetesClient createClient(Config config) {
	KubernetesClient client = new KubernetesClientBuilder().withConfig(config).build();
	return client.adapt(NamespacedKubernetesClient.class);
    }

    public static void validateCustomResource(NamespacedKubernetesClient client, String crdName) {
	client.apiextensions().v1().customResourceDefinitions().list().getItems().stream()
		.filter(crd -> Objects.equals(crd.getMetadata().getName(), crdName)).findAny()
		.orElseThrow(() -> new RuntimeException(
			"Deployment error: Custom resource definition " + crdName + " not found."));
    }

    public static <S, T> String toString(CustomResource<S, T> resource) {
	String name = resource.getMetadata() != null ? resource.getMetadata().getName() : "unknown";
	String version = resource.getMetadata() != null ? resource.getMetadata().getResourceVersion() : "unknown";
	return "name=" + name + " version=" + version + " value=" + resource.getSpec();
    }

}
