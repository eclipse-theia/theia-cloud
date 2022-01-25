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
package org.eclipse.theia.cloud.operator;

import static org.eclipse.theia.cloud.operator.util.LogMessageUtil.formatLogMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.operator.di.TheiaCloudOperatorModule;
import org.eclipse.theia.cloud.operator.resource.TemplateSpec;
import org.eclipse.theia.cloud.operator.resource.TemplateSpecResource;
import org.eclipse.theia.cloud.operator.resource.TemplateSpecResourceList;
import org.eclipse.theia.cloud.operator.resource.WorkspaceSpec;
import org.eclipse.theia.cloud.operator.resource.WorkspaceSpecResource;
import org.eclipse.theia.cloud.operator.resource.WorkspaceSpecResourceList;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

public class Main {

    private static final Logger LOGGER = LogManager.getLogger(Main.class);

    static final String COR_ID_INIT = "init";

    public static void main(String[] args) throws InterruptedException {
	new Main().runMain(args);
    }

    public void runMain(String[] args) throws InterruptedException {
	Config config = new ConfigBuilder().build();

	/*
	 * Don't close the client resource here, because this would also stop any
	 * threads
	 */
	@SuppressWarnings("resource")
	DefaultKubernetesClient client = new DefaultKubernetesClient(config);

	String namespace = client.getNamespace();
	LOGGER.info(formatLogMessage(COR_ID_INIT, "Namespace: " + namespace));

	String templateAPIVersion = HasMetadata.getApiVersion(TemplateSpecResource.class);
	LOGGER.info(formatLogMessage(COR_ID_INIT, "Registering TemplateSpecResource in version " + templateAPIVersion));
	KubernetesDeserializer.registerCustomKind(templateAPIVersion, TemplateSpec.KIND, TemplateSpecResource.class);

	String workspaceAPIVersion = HasMetadata.getApiVersion(WorkspaceSpecResource.class);
	LOGGER.info(
		formatLogMessage(COR_ID_INIT, "Registering WorkspaceSpecResource in version " + workspaceAPIVersion));
	KubernetesDeserializer.registerCustomKind(workspaceAPIVersion, WorkspaceSpec.KIND, WorkspaceSpecResource.class);

	/* Check if custom resource definition for template is registered */
	client.apiextensions().v1().customResourceDefinitions().list().getItems().stream()//
		.filter(Main::isTemplateCRD)//
		.findAny()//
		.orElseThrow(() -> new RuntimeException(
			"Deployment error: Custom resource definition Template for Theia.Cloud not found."));

	/* Check if custom resource definition for workspace is registered */
	client.apiextensions().v1().customResourceDefinitions().list().getItems().stream()//
		.filter(Main::isWorkspaceCRD)//
		.findAny()//
		.orElseThrow(() -> new RuntimeException(
			"Deployment error: Custom resource definition Workspace for Theia.Cloud not found."));

	TheiaCloudOperatorModule module = createModule(args);
	LOGGER.info(formatLogMessage(COR_ID_INIT, "Using " + module.getClass().getName() + " as DI module"));

	TheiaCloud theiaCloud = new TheiaCloudImpl(namespace, module, client,
		client.customResources(TemplateSpecResource.class, TemplateSpecResourceList.class)
			.inNamespace(namespace),
		client.customResources(WorkspaceSpecResource.class, WorkspaceSpecResourceList.class)
			.inNamespace(namespace));

	LOGGER.info(formatLogMessage(COR_ID_INIT, "Launching Theia Cloud Now"));
	theiaCloud.start();
    }

    protected TheiaCloudOperatorModule createModule(String[] args) {
	return new TheiaCloudOperatorModule();
    }

    private static boolean isTemplateCRD(CustomResourceDefinition crd) {
	String metadataName = crd.getMetadata().getName();
	LOGGER.trace(
		formatLogMessage(COR_ID_INIT, "Checking whether " + metadataName + " is " + TemplateSpec.CRD_NAME));
	return TemplateSpec.CRD_NAME.equals(metadataName);
    }

    private static boolean isWorkspaceCRD(CustomResourceDefinition crd) {
	String metadataName = crd.getMetadata().getName();
	LOGGER.trace(
		formatLogMessage(COR_ID_INIT, "Checking whether " + metadataName + " is " + WorkspaceSpec.CRD_NAME));
	return WorkspaceSpec.CRD_NAME.equals(metadataName);
    }

}
