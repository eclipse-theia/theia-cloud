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

import static org.eclipse.theia.cloud.operator.util.LogMessageUtil.formatLogMessage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.operator.handler.K8sUtil;
import org.eclipse.theia.cloud.operator.handler.TemplateAddedHandler;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudConfigMapUtil;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudDeploymentUtil;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudIngressUtil;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudServiceUtil;
import org.eclipse.theia.cloud.operator.resource.TemplateSpec;
import org.eclipse.theia.cloud.operator.resource.TemplateSpecResource;
import org.eclipse.theia.cloud.operator.util.JavaResourceUtil;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;

/**
 * A {@link TemplateAddedHandler} that will eagerly start up deployments ahead
 * of usage time which will later be used as workspaces.
 */
public class EagerStartTemplateAddedHandler implements TemplateAddedHandler {

    private static final Logger LOGGER = LogManager.getLogger(EagerStartTemplateAddedHandler.class);

    public static final String LABEL_KEY = "theiacloud";
    public static final String LABEL_VALUE_PROXY = "proxy";
    public static final String LABEL_VALUE_EMAILS = "emails";

    protected static final String OAUTH2_PROXY_CONFIGMAP_NAME = "oauth2-proxy-config";
    protected static final String OAUTH2_PROXY_CFG = "oauth2-proxy.cfg";

    protected static final String TEMPLATE_CONFIGMAP_YAML = "/templateConfigmap.yaml";
    protected static final String TEMPLATE_CONFIGMAP_EMAILS_YAML = "/templateConfigmapEmails.yaml";
    protected static final String TEMPLATE_INGRESS_YAML = "/templateIngress.yaml";
    protected static final String TEMPLATE_SERVICE_YAML = "/templateService.yaml";
    protected static final String TEMPLATE_DEPLOYMENT_YAML = "/templateDeployment.yaml";

    protected static final String CONFIGMAP_DATA_PLACEHOLDER_HOST = "https://placeholder";
    protected static final String CONFIGMAP_DATA_PLACEHOLDER_PORT = "placeholder-port";

    @Override
    public void handle(DefaultKubernetesClient client, TemplateSpecResource template, String namespace,
	    String correlationId) {
	TemplateSpec spec = template.getSpec();
	LOGGER.info(formatLogMessage(correlationId, "Handling " + spec));

	String templateResourceName = template.getMetadata().getName();
	String templateResourceUID = template.getMetadata().getUid();
	int instances = spec.getInstances();

	/* Create ingress if not existing */
	if (!TheiaCloudIngressUtil.hasExistingIngress(client, namespace, template)) {
	    LOGGER.trace(formatLogMessage(correlationId, "No existing Ingress"));
	    createAndApplyIngress(client, namespace, correlationId, templateResourceName, templateResourceUID,
		    template);
	} else {
	    LOGGER.trace(formatLogMessage(correlationId, "Ingress available already"));
	}

	/* Get existing services for this template */
	List<Service> existingServices = K8sUtil.getExistingServices(client, namespace, templateResourceName,
		templateResourceUID);

	/* Compute missing services */
	Set<Integer> missingServiceIds = TheiaCloudServiceUtil.computeIdsOfMissingServices(template, correlationId,
		instances, existingServices);

	/* Create missing services for this template */
	for (int instance : missingServiceIds) {
	    createAndApplyService(client, namespace, correlationId, templateResourceName, templateResourceUID, instance,
		    template);
	}

	/* Get existing configmaps for this template */
	List<ConfigMap> existingConfigMaps = K8sUtil.getExistingConfigMaps(client, namespace, templateResourceName,
		templateResourceUID);
	List<ConfigMap> existingProxyConfigMaps = existingConfigMaps.stream()//
		.filter(configmap -> LABEL_VALUE_PROXY.equals(configmap.getMetadata().getLabels().get(LABEL_KEY)))//
		.collect(Collectors.toList());
	List<ConfigMap> existingEmailsConfigMaps = existingConfigMaps.stream()//
		.filter(configmap -> LABEL_VALUE_EMAILS.equals(configmap.getMetadata().getLabels().get(LABEL_KEY)))//
		.collect(Collectors.toList());

	/* Compute missing configmaps */
	Set<Integer> missingProxyIds = TheiaCloudConfigMapUtil.computeIdsOfMissingProxyConfigMaps(template,
		correlationId, instances, existingProxyConfigMaps);
	Set<Integer> missingEmailIds = TheiaCloudConfigMapUtil.computeIdsOfMissingEmailConfigMaps(template,
		correlationId, instances, existingEmailsConfigMaps);

	/* Create missing configmaps for this template */
	for (int instance : missingProxyIds) {
	    createAndApplyProxyConfigMap(client, namespace, correlationId, templateResourceName, templateResourceUID,
		    instance, template);
	}
	for (int instance : missingEmailIds) {
	    createAndApplyEmailConfigMap(client, namespace, correlationId, templateResourceName, templateResourceUID,
		    instance, template);
	}

	/* Get existing deployments for this template */
	List<Deployment> existingDeployments = K8sUtil.getExistingDeployments(client, namespace, templateResourceName,
		templateResourceUID);

	/* Compute missing deployments */
	Set<Integer> missingDeploymentIds = TheiaCloudDeploymentUtil.computeIdsOfMissingDeployments(template,
		correlationId, instances, existingDeployments);

	/* Create missing deployments for this template */
	for (int instance : missingDeploymentIds) {
	    createAndApplyDeployment(client, namespace, correlationId, templateResourceName, templateResourceUID,
		    instance, template);
	}
    }

    protected void createAndApplyIngress(DefaultKubernetesClient client, String namespace, String correlationId,
	    String templateResourceName, String templateResourceUID, TemplateSpecResource template) {
	Map<String, String> replacements = TheiaCloudIngressUtil.getIngressReplacements(namespace, template);
	String ingressYaml;
	try {
	    ingressYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(EagerStartTemplateAddedHandler.class,
		    TEMPLATE_INGRESS_YAML, replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while adjusting template for ingress."), e);
	    return;
	}
	K8sUtil.loadAndCreateIngressWithOwnerReference(client, namespace, correlationId, ingressYaml,
		templateResourceName, templateResourceUID, 0);
    }

    protected void createAndApplyService(DefaultKubernetesClient client, String namespace, String correlationId,
	    String templateResourceName, String templateResourceUID, int instance, TemplateSpecResource template) {
	Map<String, String> replacements = TheiaCloudServiceUtil.getServiceReplacements(namespace, template, instance);
	String serviceYaml;
	try {
	    serviceYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(EagerStartTemplateAddedHandler.class,
		    TEMPLATE_SERVICE_YAML, replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(
		    formatLogMessage(correlationId, "Error while adjusting template for instance number " + instance),
		    e);
	    return;
	}
	K8sUtil.loadAndCreateServiceWithOwnerReference(client, namespace, correlationId, serviceYaml,
		templateResourceName, templateResourceUID, 0);
    }

    protected void createAndApplyDeployment(DefaultKubernetesClient client, String namespace, String correlationId,
	    String templateResourceName, String templateResourceUID, int instance, TemplateSpecResource template) {
	Map<String, String> replacements = TheiaCloudDeploymentUtil.getDeploymentsReplacements(namespace, template,
		instance);
	String deploymentYaml;
	try {
	    deploymentYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(EagerStartTemplateAddedHandler.class,
		    TEMPLATE_DEPLOYMENT_YAML, replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(
		    formatLogMessage(correlationId, "Error while adjusting template for instance number " + instance),
		    e);
	    return;
	}
	K8sUtil.loadAndCreateDeploymentWithOwnerReference(client, namespace, correlationId, deploymentYaml,
		templateResourceName, templateResourceUID, 0);
    }

    protected void createAndApplyProxyConfigMap(DefaultKubernetesClient client, String namespace, String correlationId,
	    String templateResourceName, String templateResourceUID, int instance, TemplateSpecResource template) {
	Map<String, String> replacements = TheiaCloudConfigMapUtil.getProxyConfigMapReplacements(namespace, template, instance);
	String configMapYaml;
	try {
	    configMapYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(EagerStartTemplateAddedHandler.class,
		    TEMPLATE_CONFIGMAP_YAML, replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(
		    formatLogMessage(correlationId, "Error while adjusting template for instance number " + instance),
		    e);
	    return;
	}
	K8sUtil.loadAndCreateConfigMapWithOwnerReference(client, namespace, correlationId, configMapYaml,
		templateResourceName, templateResourceUID, 0, configMap -> {
		    String host = TheiaCloudIngressUtil.getHostName(template, instance);
		    ConfigMap templateConfigMap = client.configMaps().inNamespace(namespace)
			    .withName(OAUTH2_PROXY_CONFIGMAP_NAME).get();
		    Map<String, String> data = new LinkedHashMap<>(templateConfigMap.getData());
		    data.put(OAUTH2_PROXY_CFG, data.get(OAUTH2_PROXY_CFG)//
			    .replace(CONFIGMAP_DATA_PLACEHOLDER_HOST, "https://" + host)//
			    .replace(CONFIGMAP_DATA_PLACEHOLDER_PORT, String.valueOf(template.getSpec().getPort())));
		    configMap.setData(data);
		});
    }

    protected void createAndApplyEmailConfigMap(DefaultKubernetesClient client, String namespace, String correlationId,
	    String templateResourceName, String templateResourceUID, int instance, TemplateSpecResource template) {
	Map<String, String> replacements = TheiaCloudConfigMapUtil.getEmailConfigMapReplacements(namespace, template, instance);
	String configMapYaml;
	try {
	    configMapYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(EagerStartTemplateAddedHandler.class,
		    TEMPLATE_CONFIGMAP_EMAILS_YAML, replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(
		    formatLogMessage(correlationId, "Error while adjusting template for instance number " + instance),
		    e);
	    return;
	}
	K8sUtil.loadAndCreateConfigMapWithOwnerReference(client, namespace, correlationId, configMapYaml,
		templateResourceName, templateResourceUID, 0);
    }

}
