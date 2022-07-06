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

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.resource.Session;
import org.eclipse.theia.cloud.common.k8s.resource.SessionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.SessionSpecResourceList;
import org.eclipse.theia.cloud.operator.di.AbstractTheiaCloudOperatorModule;
import org.eclipse.theia.cloud.operator.di.DefaultTheiaCloudOperatorModule;
import org.eclipse.theia.cloud.operator.resource.AppDefinitionSpec;
import org.eclipse.theia.cloud.operator.resource.AppDefinitionSpecResource;
import org.eclipse.theia.cloud.operator.resource.AppDefinitionSpecResourceList;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import picocli.CommandLine;

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
	DefaultKubernetesClient client = new DefaultKubernetesClient(config);

	String namespace = client.getNamespace();
	LOGGER.info(formatLogMessage(COR_ID_INIT, "Namespace: " + namespace));

	String appDefinitionAPIVersion = HasMetadata.getApiVersion(AppDefinitionSpecResource.class);
	LOGGER.info(formatLogMessage(COR_ID_INIT,
		"Registering AppDefinitionSpecResource in version " + appDefinitionAPIVersion));
	KubernetesDeserializer.registerCustomKind(appDefinitionAPIVersion, AppDefinitionSpec.KIND,
		AppDefinitionSpecResource.class);

	String sessionAPIVersion = HasMetadata.getApiVersion(Session.class);
	LOGGER.info(formatLogMessage(COR_ID_INIT, "Registering Sessions in version " + sessionAPIVersion));
	KubernetesDeserializer.registerCustomKind(sessionAPIVersion, SessionSpec.KIND, Session.class);

	/* Check if custom resource definition for app definition is registered */
	client.apiextensions().v1().customResourceDefinitions().list().getItems().stream()//
		.filter(Main::isAppDefinitionCRD)//
		.findAny()//
		.orElseThrow(() -> new RuntimeException(
			"Deployment error: Custom resource definition App Definition for Theia.Cloud not found."));

	/* Check if custom resource definition for session is registered */
	client.apiextensions().v1().customResourceDefinitions().list().getItems().stream()//
		.filter(Main::isSessionCRD)//
		.findAny()//
		.orElseThrow(() -> new RuntimeException(
			"Deployment error: Custom resource definition Session for Theia.Cloud not found."));

	TheiaCloudArguments arguments = createArguments(args);
	AbstractTheiaCloudOperatorModule module = createModule(arguments);
	LOGGER.info(formatLogMessage(COR_ID_INIT, "Using " + module.getClass().getName() + " as DI module"));

	TheiaCloud theiaCloud = new TheiaCloudImpl(namespace, module, arguments, client,
		client.customResources(AppDefinitionSpecResource.class, AppDefinitionSpecResourceList.class)
			.inNamespace(namespace),
		client.customResources(Session.class, SessionSpecResourceList.class).inNamespace(namespace));

	LOGGER.info(formatLogMessage(COR_ID_INIT, "Launching Theia Cloud Now"));
	theiaCloud.start();
    }

    protected TheiaCloudArguments createArguments(String[] args) {
	TheiaCloudArguments arguments = new TheiaCloudArguments();
	CommandLine commandLine = new CommandLine(arguments).setTrimQuotes(true);
	commandLine.parseArgs(args);

	LOGGER.info(formatLogMessage(COR_ID_INIT, "Parsing args: keycloak " + arguments.isUseKeycloak()));
	LOGGER.info(formatLogMessage(COR_ID_INIT, "Parsing args: eagerStart " + arguments.isEagerStart()));
	LOGGER.info(formatLogMessage(COR_ID_INIT, "Parsing args: ephemeralStorage " + arguments.isEphemeralStorage()));
	LOGGER.info(formatLogMessage(COR_ID_INIT, "Parsing args: cloudProvider " + arguments.getCloudProvider()));
	LOGGER.info(formatLogMessage(COR_ID_INIT, "Parsing args: bandwidthLimiter " + arguments.getBandwidthLimiter()));
	LOGGER.info(formatLogMessage(COR_ID_INIT, "Parsing args: killAfter " + arguments.getKillAfter()));
	return arguments;
    }

    protected AbstractTheiaCloudOperatorModule createModule(TheiaCloudArguments arguments) {
	return new DefaultTheiaCloudOperatorModule(arguments);
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
