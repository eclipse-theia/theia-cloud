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

import org.eclipse.theia.cloud.common.k8s.client.DefaultTheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.Session;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.client.Adapters;
import io.fabric8.kubernetes.client.Client;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.ExtensionAdapter;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

public final class CustomResourceUtil {

    static {
	Adapters.register(new CustomResourceClientAdapter());
    }

    private CustomResourceUtil() {
    }

    public static NamespacedKubernetesClient createClient() {
	return createClient(new ConfigBuilder().build());
    }

    public static NamespacedKubernetesClient createClient(Config config) {
	DefaultKubernetesClient client = new DefaultKubernetesClient(config);
	registerSessionResource(client);
	registerWorkspaceResource(client);
	registerAppDefinitionResource(client);
	return client;
    }

    public static void registerSessionResource(NamespacedKubernetesClient client) {
	registerCustomResource(client, Session.class, Session.KIND, Session.CRD_NAME);
    }

    public static void registerWorkspaceResource(NamespacedKubernetesClient client) {
	registerCustomResource(client, Workspace.class, Workspace.KIND, Workspace.CRD_NAME);
    }

    public static void registerAppDefinitionResource(NamespacedKubernetesClient client) {
	registerCustomResource(client, AppDefinition.class, AppDefinition.KIND, AppDefinition.CRD_NAME);
    }

    public static void registerCustomResource(NamespacedKubernetesClient client,
	    Class<? extends KubernetesResource> resourceClass, String kind, String crdName) {
	String apiVersion = HasMetadata.getApiVersion(resourceClass);
	KubernetesDeserializer.registerCustomKind(apiVersion, kind, resourceClass);
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

    private static class CustomResourceClientAdapter implements ExtensionAdapter<TheiaCloudClient> {
	@Override
	public Class<TheiaCloudClient> getExtensionType() {
	    return TheiaCloudClient.class;
	}

	@Override
	public Boolean isAdaptable(Client client) {
	    return client instanceof NamespacedKubernetesClient;
	}

	@Override
	public TheiaCloudClient adapt(Client client) {
	    return new DefaultTheiaCloudClient((NamespacedKubernetesClient) client);
	}
    }

}
