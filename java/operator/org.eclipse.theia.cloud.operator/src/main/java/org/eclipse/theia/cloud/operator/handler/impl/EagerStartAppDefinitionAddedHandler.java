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
package org.eclipse.theia.cloud.operator.handler.impl;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.operator.TheiaCloudArguments;
import org.eclipse.theia.cloud.operator.handler.AppDefinitionAddedHandler;
import org.eclipse.theia.cloud.operator.handler.BandwidthLimiter;
import org.eclipse.theia.cloud.operator.handler.IngressPathProvider;
import org.eclipse.theia.cloud.operator.handler.K8sUtil;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudConfigMapUtil;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudDeploymentUtil;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudIngressUtil;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudServiceUtil;
import org.eclipse.theia.cloud.operator.resource.AppDefinitionSpec;
import org.eclipse.theia.cloud.operator.resource.AppDefinitionSpecResource;
import org.eclipse.theia.cloud.operator.util.JavaResourceUtil;

import com.google.inject.Inject;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;

/**
 * A {@link AppDefinitionAddedHandler} that will eagerly start up deployments
 * ahead of usage time which will later be used as sessions.
 */
public class EagerStartAppDefinitionAddedHandler implements AppDefinitionAddedHandler {

    private static final Logger LOGGER = LogManager.getLogger(EagerStartAppDefinitionAddedHandler.class);

    public static final String LABEL_KEY = "theiacloud";
    public static final String LABEL_VALUE_PROXY = "proxy";
    public static final String LABEL_VALUE_EMAILS = "emails";

    protected TheiaCloudArguments arguments;
    protected IngressPathProvider ingressPathProvider;
    protected BandwidthLimiter bandwidthLimiter;

    @Inject
    public EagerStartAppDefinitionAddedHandler(TheiaCloudArguments arguments, IngressPathProvider ingressPathProvider,
	    BandwidthLimiter bandwidthLimiter) {
	this.arguments = arguments;
	this.ingressPathProvider = ingressPathProvider;
	this.bandwidthLimiter = bandwidthLimiter;
    }

    @Override
    public void handle(DefaultKubernetesClient client, AppDefinitionSpecResource appDefinition, String namespace,
	    String correlationId) {
	AppDefinitionSpec spec = appDefinition.getSpec();
	LOGGER.info(formatLogMessage(correlationId, "Handling " + spec));

	String appDefinitionResourceName = appDefinition.getMetadata().getName();
	String appDefinitionResourceUID = appDefinition.getMetadata().getUid();
	int instances = spec.getMinInstances();

	/* Create ingress if not existing */
	if (!TheiaCloudIngressUtil.checkForExistingIngressAndAddOwnerReferencesIfMissing(client, namespace,
		appDefinition, correlationId)) {
	    LOGGER.trace(formatLogMessage(correlationId, "No existing Ingress"));
	    AddedHandler.createAndApplyIngress(client, namespace, correlationId, appDefinitionResourceName,
		    appDefinitionResourceUID, appDefinition);
	} else {
	    LOGGER.trace(formatLogMessage(correlationId, "Ingress available already"));
	}

	/* Get existing services for this app definition */
	List<Service> existingServices = K8sUtil.getExistingServices(client, namespace, appDefinitionResourceName,
		appDefinitionResourceUID);

	/* Compute missing services */
	Set<Integer> missingServiceIds = TheiaCloudServiceUtil.computeIdsOfMissingServices(appDefinition, correlationId,
		instances, existingServices);

	/* Create missing services for this app definition */
	for (int instance : missingServiceIds) {
	    createAndApplyService(client, namespace, correlationId, appDefinitionResourceName, appDefinitionResourceUID,
		    instance, appDefinition, arguments.isUseKeycloak());
	}

	if (arguments.isUseKeycloak()) {
	    /* Get existing configmaps for this app definition */
	    List<ConfigMap> existingConfigMaps = K8sUtil.getExistingConfigMaps(client, namespace,
		    appDefinitionResourceName, appDefinitionResourceUID);
	    List<ConfigMap> existingProxyConfigMaps = existingConfigMaps.stream()//
		    .filter(configmap -> LABEL_VALUE_PROXY.equals(configmap.getMetadata().getLabels().get(LABEL_KEY)))//
		    .collect(Collectors.toList());
	    List<ConfigMap> existingEmailsConfigMaps = existingConfigMaps.stream()//
		    .filter(configmap -> LABEL_VALUE_EMAILS.equals(configmap.getMetadata().getLabels().get(LABEL_KEY)))//
		    .collect(Collectors.toList());

	    /* Compute missing configmaps */
	    Set<Integer> missingProxyIds = TheiaCloudConfigMapUtil.computeIdsOfMissingProxyConfigMaps(appDefinition,
		    correlationId, instances, existingProxyConfigMaps);
	    Set<Integer> missingEmailIds = TheiaCloudConfigMapUtil.computeIdsOfMissingEmailConfigMaps(appDefinition,
		    correlationId, instances, existingEmailsConfigMaps);

	    /* Create missing configmaps for this app definition */
	    for (int instance : missingProxyIds) {
		createAndApplyProxyConfigMap(client, namespace, correlationId, appDefinitionResourceName,
			appDefinitionResourceUID, instance, appDefinition);
	    }
	    for (int instance : missingEmailIds) {
		createAndApplyEmailConfigMap(client, namespace, correlationId, appDefinitionResourceName,
			appDefinitionResourceUID, instance, appDefinition);
	    }
	}

	/* Get existing deployments for this app definition */
	List<Deployment> existingDeployments = K8sUtil.getExistingDeployments(client, namespace,
		appDefinitionResourceName, appDefinitionResourceUID);

	/* Compute missing deployments */
	Set<Integer> missingDeploymentIds = TheiaCloudDeploymentUtil.computeIdsOfMissingDeployments(appDefinition,
		correlationId, instances, existingDeployments);

	/* Create missing deployments for this app definition */
	for (int instance : missingDeploymentIds) {
	    createAndApplyDeployment(client, namespace, correlationId, appDefinitionResourceName,
		    appDefinitionResourceUID, instance, appDefinition, arguments.isUseKeycloak());
	}
    }

    protected void createAndApplyService(DefaultKubernetesClient client, String namespace, String correlationId,
	    String appDefinitionResourceName, String appDefinitionResourceUID, int instance,
	    AppDefinitionSpecResource appDefinition, boolean useOAuth2Proxy) {
	Map<String, String> replacements = TheiaCloudServiceUtil.getServiceReplacements(namespace, appDefinition,
		instance);
	String templateYaml = useOAuth2Proxy ? AddedHandler.TEMPLATE_SERVICE_YAML
		: AddedHandler.TEMPLATE_SERVICE_WITHOUT_AOUTH2_PROXY_YAML;
	String serviceYaml;
	try {
	    serviceYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(templateYaml, replacements,
		    correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(
		    formatLogMessage(correlationId, "Error while adjusting template for instance number " + instance),
		    e);
	    return;
	}
	K8sUtil.loadAndCreateServiceWithOwnerReference(client, namespace, correlationId, serviceYaml,
		AppDefinitionSpec.API, AppDefinitionSpec.KIND, appDefinitionResourceName, appDefinitionResourceUID, 0);
    }

    protected void createAndApplyDeployment(DefaultKubernetesClient client, String namespace, String correlationId,
	    String appDefinitionResourceName, String appDefinitionResourceUID, int instance,
	    AppDefinitionSpecResource appDefinition, boolean useOAuth2Proxy) {
	Map<String, String> replacements = TheiaCloudDeploymentUtil.getDeploymentsReplacements(namespace, appDefinition,
		instance);
	String templateYaml = useOAuth2Proxy ? AddedHandler.TEMPLATE_DEPLOYMENT_YAML
		: AddedHandler.TEMPLATE_DEPLOYMENT_WITHOUT_AOUTH2_PROXY_YAML;
	String deploymentYaml;
	try {
	    deploymentYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(templateYaml, replacements,
		    correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(
		    formatLogMessage(correlationId, "Error while adjusting template for instance number " + instance),
		    e);
	    return;
	}
	K8sUtil.loadAndCreateDeploymentWithOwnerReference(client, namespace, correlationId, deploymentYaml,
		AppDefinitionSpec.API, AppDefinitionSpec.KIND, appDefinitionResourceName, appDefinitionResourceUID, 0,
		deployment -> {
		    bandwidthLimiter.limit(deployment, appDefinition.getSpec().getDownlinkLimit(),
			    appDefinition.getSpec().getUplinkLimit(), correlationId);
		    AddedHandler.removeEmptyResources(deployment);
		    if (appDefinition.getSpec().getPullSecret() != null
			    && !appDefinition.getSpec().getPullSecret().isEmpty()) {
			AddedHandler.addImagePullSecret(deployment, appDefinition.getSpec().getPullSecret());
		    }
		});
    }

    protected void createAndApplyProxyConfigMap(DefaultKubernetesClient client, String namespace, String correlationId,
	    String appDefinitionResourceName, String appDefinitionResourceUID, int instance,
	    AppDefinitionSpecResource appDefinition) {
	Map<String, String> replacements = TheiaCloudConfigMapUtil.getProxyConfigMapReplacements(namespace,
		appDefinition, instance);
	String configMapYaml;
	try {
	    configMapYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(AddedHandler.TEMPLATE_CONFIGMAP_YAML,
		    replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(
		    formatLogMessage(correlationId, "Error while adjusting template for instance number " + instance),
		    e);
	    return;
	}
	K8sUtil.loadAndCreateConfigMapWithOwnerReference(client, namespace, correlationId, configMapYaml,
		AppDefinitionSpec.API, AppDefinitionSpec.KIND, appDefinitionResourceName, appDefinitionResourceUID, 0,
		configMap -> {
		    String host = appDefinition.getSpec() + "-" + instance + appDefinition.getSpec().getHost()
			    + ingressPathProvider.getPath(appDefinition, instance);
		    int port = appDefinition.getSpec().getPort();
		    AddedHandler.updateProxyConfigMap(client, namespace, configMap, host, port);
		});
    }

    protected void createAndApplyEmailConfigMap(DefaultKubernetesClient client, String namespace, String correlationId,
	    String appDefinitionResourceName, String appDefinitionResourceUID, int instance,
	    AppDefinitionSpecResource appDefinition) {
	Map<String, String> replacements = TheiaCloudConfigMapUtil.getEmailConfigMapReplacements(namespace,
		appDefinition, instance);
	String configMapYaml;
	try {
	    configMapYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(
		    AddedHandler.TEMPLATE_CONFIGMAP_EMAILS_YAML, replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(
		    formatLogMessage(correlationId, "Error while adjusting template for instance number " + instance),
		    e);
	    return;
	}
	K8sUtil.loadAndCreateConfigMapWithOwnerReference(client, namespace, correlationId, configMapYaml,
		AppDefinitionSpec.API, AppDefinitionSpec.KIND, appDefinitionResourceName, appDefinitionResourceUID, 0);
    }

}
