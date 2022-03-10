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
import org.eclipse.theia.cloud.operator.handler.IngressPathProvider;
import org.eclipse.theia.cloud.operator.handler.K8sUtil;
import org.eclipse.theia.cloud.operator.handler.TemplateAddedHandler;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudConfigMapUtil;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudDeploymentUtil;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudIngressUtil;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudServiceUtil;
import org.eclipse.theia.cloud.operator.resource.TemplateSpec;
import org.eclipse.theia.cloud.operator.resource.TemplateSpecResource;
import org.eclipse.theia.cloud.operator.util.JavaResourceUtil;

import com.google.inject.Inject;

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

    protected TheiaCloudArguments arguments;
    protected IngressPathProvider ingressPathProvider;

    @Inject
    public EagerStartTemplateAddedHandler(TheiaCloudArguments arguments, IngressPathProvider ingressPathProvider) {
	this.arguments = arguments;
	this.ingressPathProvider = ingressPathProvider;
    }

    @Override
    public void handle(DefaultKubernetesClient client, TemplateSpecResource template, String namespace,
	    String correlationId) {
	TemplateSpec spec = template.getSpec();
	LOGGER.info(formatLogMessage(correlationId, "Handling " + spec));

	String templateResourceName = template.getMetadata().getName();
	String templateResourceUID = template.getMetadata().getUid();
	int instances = spec.getInstances();

	/* Create ingress if not existing */
	if (!TheiaCloudIngressUtil.checkForExistingIngressAndAddOwnerReferencesIfMissing(client, namespace, template,
		correlationId)) {
	    LOGGER.trace(formatLogMessage(correlationId, "No existing Ingress"));
	    AddedHandler.createAndApplyIngress(client, namespace, correlationId, templateResourceName,
		    templateResourceUID, template);
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
		    template, arguments.isUseKeycloak());
	}

	if (arguments.isUseKeycloak()) {
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
		createAndApplyProxyConfigMap(client, namespace, correlationId, templateResourceName,
			templateResourceUID, instance, template);
	    }
	    for (int instance : missingEmailIds) {
		createAndApplyEmailConfigMap(client, namespace, correlationId, templateResourceName,
			templateResourceUID, instance, template);
	    }
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
		    instance, template, arguments.isUseKeycloak());
	}
    }

    protected void createAndApplyService(DefaultKubernetesClient client, String namespace, String correlationId,
	    String templateResourceName, String templateResourceUID, int instance, TemplateSpecResource template,
	    boolean useOAuth2Proxy) {
	Map<String, String> replacements = TheiaCloudServiceUtil.getServiceReplacements(namespace, template, instance);
	String templateYaml = useOAuth2Proxy ? AddedHandler.TEMPLATE_SERVICE_YAML
		: AddedHandler.TEMPLATE_SERVICE_WITHOUT_AOUTH2_PROXY_YAML;
	String serviceYaml;
	try {
	    serviceYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(EagerStartTemplateAddedHandler.class,
		    templateYaml, replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(
		    formatLogMessage(correlationId, "Error while adjusting template for instance number " + instance),
		    e);
	    return;
	}
	K8sUtil.loadAndCreateServiceWithOwnerReference(client, namespace, correlationId, serviceYaml, TemplateSpec.API,
		TemplateSpec.KIND, templateResourceName, templateResourceUID, 0);
    }

    protected void createAndApplyDeployment(DefaultKubernetesClient client, String namespace, String correlationId,
	    String templateResourceName, String templateResourceUID, int instance, TemplateSpecResource template,
	    boolean useOAuth2Proxy) {
	Map<String, String> replacements = TheiaCloudDeploymentUtil.getDeploymentsReplacements(namespace, template,
		instance);
	String templateYaml = useOAuth2Proxy ? AddedHandler.TEMPLATE_DEPLOYMENT_YAML
		: AddedHandler.TEMPLATE_DEPLOYMENT_WITHOUT_AOUTH2_PROXY_YAML;
	String deploymentYaml;
	try {
	    deploymentYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(EagerStartTemplateAddedHandler.class,
		    templateYaml, replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(
		    formatLogMessage(correlationId, "Error while adjusting template for instance number " + instance),
		    e);
	    return;
	}
	K8sUtil.loadAndCreateDeploymentWithOwnerReference(client, namespace, correlationId, deploymentYaml,
		TemplateSpec.API, TemplateSpec.KIND, templateResourceName, templateResourceUID, 0, deployment -> {
		});
    }

    protected void createAndApplyProxyConfigMap(DefaultKubernetesClient client, String namespace, String correlationId,
	    String templateResourceName, String templateResourceUID, int instance, TemplateSpecResource template) {
	Map<String, String> replacements = TheiaCloudConfigMapUtil.getProxyConfigMapReplacements(namespace, template,
		instance);
	String configMapYaml;
	try {
	    configMapYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(EagerStartTemplateAddedHandler.class,
		    AddedHandler.TEMPLATE_CONFIGMAP_YAML, replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(
		    formatLogMessage(correlationId, "Error while adjusting template for instance number " + instance),
		    e);
	    return;
	}
	K8sUtil.loadAndCreateConfigMapWithOwnerReference(client, namespace, correlationId, configMapYaml,
		TemplateSpec.API, TemplateSpec.KIND, templateResourceName, templateResourceUID, 0, configMap -> {
		    String host = template.getSpec().getHost() + ingressPathProvider.getPath(template, instance);
		    int port = template.getSpec().getPort();
		    AddedHandler.updateProxyConfigMap(client, namespace, configMap, host, port);
		});
    }

    protected void createAndApplyEmailConfigMap(DefaultKubernetesClient client, String namespace, String correlationId,
	    String templateResourceName, String templateResourceUID, int instance, TemplateSpecResource template) {
	Map<String, String> replacements = TheiaCloudConfigMapUtil.getEmailConfigMapReplacements(namespace, template,
		instance);
	String configMapYaml;
	try {
	    configMapYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(EagerStartTemplateAddedHandler.class,
		    AddedHandler.TEMPLATE_CONFIGMAP_EMAILS_YAML, replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(
		    formatLogMessage(correlationId, "Error while adjusting template for instance number " + instance),
		    e);
	    return;
	}
	K8sUtil.loadAndCreateConfigMapWithOwnerReference(client, namespace, correlationId, configMapYaml,
		TemplateSpec.API, TemplateSpec.KIND, templateResourceName, templateResourceUID, 0);
    }

}
