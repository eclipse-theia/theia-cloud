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
package org.eclipse.theia.cloud.operator.di;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.resource.SessionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.util.K8sResourceUtil;
import org.eclipse.theia.cloud.operator.resource.AppDefinitionSpec;
import org.eclipse.theia.cloud.operator.resource.AppDefinition;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

public final class TheiaCloudModuleDefaults {
    private static final Logger LOGGER = LogManager.getLogger(TheiaCloudModuleDefaults.class);
    private static final String COR_ID_INIT = "init";

    private TheiaCloudModuleDefaults() {
    }

    public static NamespacedKubernetesClient createKubernetesClient() {
	Config config = new ConfigBuilder().build();

	/*
	 * Don't close the client resource here, because this would also stop any
	 * threads
	 */
	DefaultKubernetesClient client = new DefaultKubernetesClient(config);

	String namespace = client.getNamespace();
	LOGGER.info(formatLogMessage(COR_ID_INIT, "Namespace: " + namespace));

	String appDefinitionAPIVersion = HasMetadata.getApiVersion(AppDefinition.class);
	LOGGER.info(formatLogMessage(COR_ID_INIT,
		"Registering AppDefinitionSpecResource in version " + appDefinitionAPIVersion));
	KubernetesDeserializer.registerCustomKind(appDefinitionAPIVersion, AppDefinitionSpec.KIND,
		AppDefinition.class);

	K8sResourceUtil.registerSessionResource(client);
	K8sResourceUtil.registerWorkspaceResource(client);
	K8sResourceUtil.registerCustomResource(client, AppDefinition.class, AppDefinitionSpec.KIND,
		AppDefinitionSpec.CRD_NAME);

	/* Check if custom resource definition for app definition is registered */
	client.apiextensions().v1().customResourceDefinitions().list().getItems().stream()//
		.filter(TheiaCloudModuleDefaults::isAppDefinitionCRD)//
		.findAny()//
		.orElseThrow(() -> new RuntimeException(
			"Deployment error: Custom resource definition App Definition for Theia.Cloud not found."));

	/* Check if custom resource definition for session is registered */
	client.apiextensions().v1().customResourceDefinitions().list().getItems().stream()//
		.filter(TheiaCloudModuleDefaults::isSessionCRD)//
		.findAny()//
		.orElseThrow(() -> new RuntimeException(
			"Deployment error: Custom resource definition Session for Theia.Cloud not found."));

	return client;
    }

    private static boolean isAppDefinitionCRD(CustomResourceDefinition crd) {
	String metadataName = crd.getMetadata().getName();
	LOGGER.trace(formatLogMessage(COR_ID_INIT,
		"Checking whether " + metadataName + " is " + AppDefinitionSpec.CRD_NAME));
	return AppDefinitionSpec.CRD_NAME.equals(metadataName);
    }

    private static boolean isSessionCRD(CustomResourceDefinition crd) {
	String metadataName = crd.getMetadata().getName();
	LOGGER.trace(formatLogMessage(COR_ID_INIT, "Checking whether " + metadataName + " is " + SessionSpec.CRD_NAME));
	return SessionSpec.CRD_NAME.equals(metadataName);
    }
}
