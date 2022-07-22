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
package org.eclipse.theia.cloud.common.k8s.resource.util;

import java.util.Objects;

import org.eclipse.theia.cloud.common.k8s.resource.Session;
import org.eclipse.theia.cloud.common.k8s.resource.SessionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.common.k8s.resource.WorkspaceSpec;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

public final class K8sResourceUtil {

    private K8sResourceUtil() {
    }

    public static <S, T> String customResourcetoString(CustomResource<S, T> resource) {
	String name = resource.getMetadata() != null ? resource.getMetadata().getName() : "unknown";
	String version = resource.getMetadata() != null ? resource.getMetadata().getResourceVersion() : "unknown";
	return "name=" + name + " version=" + version + " value=" + resource.getSpec();
    }

    public static void registerSessionResource(NamespacedKubernetesClient client) {
	registerCustomResource(client, Session.class, SessionSpec.KIND, SessionSpec.CRD_NAME);
    }

    public static void registerWorkspaceResource(NamespacedKubernetesClient client) {
	registerCustomResource(client, Workspace.class, WorkspaceSpec.KIND, WorkspaceSpec.CRD_NAME);
    }

    public static void registerCustomResource(NamespacedKubernetesClient client,
	    Class<? extends KubernetesResource> resourceClass, String kind, String crdName) {
	String apiVersion = HasMetadata.getApiVersion(resourceClass);
	KubernetesDeserializer.registerCustomKind(apiVersion, kind, resourceClass);

	client.apiextensions().v1().customResourceDefinitions().list().getItems().stream()//
		.filter(crd -> Objects.equals(crd.getMetadata().getName(), crdName)).findAny() //
		.orElseThrow(() -> new RuntimeException(
			"Deployment error: Custom resource definition " + crdName + " not found."));
    }
}
