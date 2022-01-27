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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.operator.handler.K8sUtil;
import org.eclipse.theia.cloud.operator.handler.TemplateAddedHandler;
import org.eclipse.theia.cloud.operator.resource.TemplateSpec;
import org.eclipse.theia.cloud.operator.resource.TemplateSpecResource;
import org.eclipse.theia.cloud.operator.util.ResourceUtil;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressList;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;

public class DefaultTemplateAddedHandler implements TemplateAddedHandler {

    private static final Logger LOGGER = LogManager.getLogger(DefaultTemplateAddedHandler.class);

    public static final String INGRESS_NAME = "-ingress";
    public static final String SERVICE_NAME = "-service-";
    public static final String DEPLOYMENT_NAME = "-deployment-";
    public static final String CONFIGMAP_PROXY_NAME = "-config-";
    public static final String CONFIGMAP_EMAIL_NAME = "-emailconfig-";

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

    protected static final String PLACEHOLDER_PORT = "placeholder-port";
    protected static final String PLACEHOLDER_EMAILSCONFIGNAME = "placeholder-emailsconfigname";
    protected static final String PLACEHOLDER_CONFIGNAME = "placeholder-configname";
    protected static final String PLACEHOLDER_INGRESSNAME = "placeholder-ingressname";
    protected static final String PLACEHOLDER_HOST = "placeholder-host";
    protected static final String PLACEHOLDER_SERVICENAME = "placeholder-servicename";
    protected static final String PLACEHOLDER_APP = "placeholder-app";
    protected static final String PLACEHOLDER_DEPLOYMENTNAME = "placeholder-depname";
    protected static final String PLACEHOLDER_NAMESPACE = "placeholder-namespace";
    protected static final String PLACEHOLDER_TEMPLATENAME = "placeholder-templatename";
    protected static final String PLACEHOLDER_IMAGE = "placeholder-image";

    protected static final String CONFIGMAP_DATA_PLACEHOLDER_HOST = "https://placeholder";
    protected static final String CONFIGMAP_DATA_PLACEHOLDER_PORT = "placeholder-port";

    @Override
    public void handle(DefaultKubernetesClient client, TemplateSpecResource template, String namespace,
	    String correlationId) {
	TemplateSpec spec = template.getSpec();
	LOGGER.info(formatLogMessage(correlationId, "Handling " + spec));

	String templateResourceName = template.getMetadata().getName();
	String templateResourceUID = template.getMetadata().getUid();
	String templateID = spec.getName();
	String image = spec.getImage();
	int instances = spec.getInstances();
	String host = spec.getHost();
	int port = spec.getPort();

	/* Create ingress if not existing */
	if (!hasExistingIngress(client, namespace, templateResourceName, templateResourceUID)) {
	    LOGGER.trace(formatLogMessage(correlationId, "No existing Ingress"));
	    createAndApplyIngress(client, namespace, correlationId, templateResourceName, templateResourceUID,
		    templateID, host);
	} else {
	    LOGGER.trace(formatLogMessage(correlationId, "Ingress available already"));
	}

	/* Get existing services for this template */
	List<Service> existingServices = K8sUtil.getExistingServices(client, namespace, templateResourceName,
		templateResourceUID);

	/* Create missing services for this template */
	createMissingServices(client, namespace, correlationId, templateResourceName, templateResourceUID, templateID,
		instances, port, existingServices);

	/* Get existing configmaps for this template */
	List<ConfigMap> existingConfigMaps = K8sUtil.getExistingConfigMaps(client, namespace, templateResourceName,
		templateResourceUID);

	/* Create missing configmaps for this template */
	createMissingConfigMaps(client, namespace, correlationId, templateResourceName, templateResourceUID, templateID,
		image, instances, host, port, existingConfigMaps);

	/* Get existing deployments for this template */
	List<Deployment> existingDeployments = K8sUtil.getExistingDeployments(client, namespace, templateResourceName,
		templateResourceUID);

	/* Create missing deployments for this template */
	createMissingDeployments(client, namespace, correlationId, templateResourceName, templateResourceUID,
		templateID, image, instances, port, existingDeployments);
    }

    protected boolean hasExistingIngress(DefaultKubernetesClient client, String namespace, String templateResourceName,
	    String templateResourceUID) {
	return K8sUtil.getExistingIngress(client, namespace, templateResourceName, templateResourceUID).isPresent();
    }

    protected void createMissingServices(DefaultKubernetesClient client, String namespace, String correlationId,
	    String templateResourceName, String templateResourceUID, String templateID, int instances, int port,
	    List<Service> existingServices) {
	if (existingServices.size() == 0) {
	    LOGGER.trace(formatLogMessage(correlationId, "No existing Services"));
	    for (int i = 1; i <= instances; i++) {
		createAndApplyService(client, namespace, correlationId, templateResourceName, templateResourceUID,
			templateID, i, port);
	    }
	} else {
	    List<Integer> missingInstances = IntStream.rangeClosed(1, instances).boxed().collect(Collectors.toList());
	    int namePrefixLength = (templateID + SERVICE_NAME).length();
	    for (Service service : existingServices) {
		String name = service.getMetadata().getName();
		String instance = name.substring(namePrefixLength);
		try {
		    missingInstances.remove(Integer.valueOf(instance));
		} catch (NumberFormatException e) {
		    LOGGER.error(formatLogMessage(correlationId, "Error while getting integer value of " + instance),
			    e);
		}
	    }
	    if (missingInstances.isEmpty()) {
		LOGGER.trace(formatLogMessage(correlationId, "All Services existing already"));
	    } else {
		LOGGER.trace(formatLogMessage(correlationId, "Some Services need to be created"));
	    }
	    for (int i : missingInstances) {
		createAndApplyService(client, namespace, correlationId, templateResourceName, templateResourceUID,
			templateID, i, port);
	    }
	}
    }

    protected void createMissingDeployments(DefaultKubernetesClient client, String namespace, String correlationId,
	    String templateResourceName, String templateResourceUID, String templateID, String image, int instances,
	    int port, List<Deployment> existingDeployments) {
	if (existingDeployments.size() == 0) {
	    LOGGER.trace(formatLogMessage(correlationId, "No existing Deployments"));
	    for (int i = 1; i <= instances; i++) {
		createAndApplyDeployment(client, namespace, correlationId, templateResourceName, templateResourceUID,
			templateID, image, i, port);
	    }
	} else {
	    List<Integer> missingDeployments = IntStream.rangeClosed(1, instances).boxed().collect(Collectors.toList());
	    int namePrefixLength = (templateID + DEPLOYMENT_NAME).length();
	    for (Deployment deployment : existingDeployments) {
		String name = deployment.getMetadata().getName();
		String instance = name.substring(namePrefixLength);
		try {
		    missingDeployments.remove(Integer.valueOf(instance));
		} catch (NumberFormatException e) {
		    LOGGER.error(formatLogMessage(correlationId, "Error while getting integer value of " + instance),
			    e);
		}
	    }
	    if (missingDeployments.isEmpty()) {
		LOGGER.trace(formatLogMessage(correlationId, "All Deployments existing already"));
	    } else {
		LOGGER.trace(formatLogMessage(correlationId, "Some Deployments need to be created"));
	    }
	    for (int i : missingDeployments) {
		createAndApplyDeployment(client, namespace, correlationId, templateResourceName, templateResourceUID,
			templateID, image, i, port);
	    }
	}
    }

    protected void createMissingConfigMaps(DefaultKubernetesClient client, String namespace, String correlationId,
	    String templateResourceName, String templateResourceUID, String templateID, String image, int instances,
	    String host, int port, List<ConfigMap> existingConfigMaps) {

	List<ConfigMap> existingProxyConfigMaps = existingConfigMaps.stream()//
		.filter(configmap -> LABEL_VALUE_PROXY.equals(configmap.getMetadata().getLabels().get(LABEL_KEY)))//
		.collect(Collectors.toList());

	createMissingProxyConfigMaps(client, namespace, correlationId, templateResourceName, templateResourceUID,
		templateID, image, instances, host, port, existingProxyConfigMaps);

	List<ConfigMap> existingEmailsConfigMaps = existingConfigMaps.stream()//
		.filter(configmap -> LABEL_VALUE_EMAILS.equals(configmap.getMetadata().getLabels().get(LABEL_KEY)))//
		.collect(Collectors.toList());

	createMissingEmailsConfigMaps(client, namespace, correlationId, templateResourceName, templateResourceUID,
		templateID, image, instances, host, port, existingEmailsConfigMaps);
    }

    protected void createMissingProxyConfigMaps(DefaultKubernetesClient client, String namespace, String correlationId,
	    String templateResourceName, String templateResourceUID, String templateID, String image, int instances,
	    String host, int port, List<ConfigMap> existingConfigMaps) {
	if (existingConfigMaps.size() == 0) {
	    LOGGER.trace(formatLogMessage(correlationId, "No existing Proxy Config Maps"));
	    for (int i = 1; i <= instances; i++) {
		createAndApplyProxyConfigMap(client, namespace, correlationId, templateResourceName,
			templateResourceUID, templateID, image, i, host, port);
	    }
	} else {
	    List<Integer> missingConfigMaps = IntStream.rangeClosed(1, instances).boxed().collect(Collectors.toList());
	    int namePrefixLength = (templateID + CONFIGMAP_PROXY_NAME).length();
	    for (ConfigMap configMap : existingConfigMaps) {
		String name = configMap.getMetadata().getName();
		String instance = name.substring(namePrefixLength);
		try {
		    missingConfigMaps.remove(Integer.valueOf(instance));
		} catch (NumberFormatException e) {
		    LOGGER.error(formatLogMessage(correlationId, "Error while getting integer value of " + instance),
			    e);
		}
	    }
	    if (missingConfigMaps.isEmpty()) {
		LOGGER.trace(formatLogMessage(correlationId, "All Proxy Config Maps existing already"));
	    } else {
		LOGGER.trace(formatLogMessage(correlationId, "Some Proxy Config Maps need to be created"));
	    }
	    for (int i : missingConfigMaps) {
		createAndApplyProxyConfigMap(client, namespace, correlationId, templateResourceName,
			templateResourceUID, templateID, image, i, host, port);
	    }
	}
    }

    protected void createMissingEmailsConfigMaps(DefaultKubernetesClient client, String namespace, String correlationId,
	    String templateResourceName, String templateResourceUID, String templateID, String image, int instances,
	    String host, int port, List<ConfigMap> existingConfigMaps) {
	if (existingConfigMaps.size() == 0) {
	    LOGGER.trace(formatLogMessage(correlationId, "No existing Email Config Maps"));
	    for (int i = 1; i <= instances; i++) {
		createAndApplyEmailConfigMap(client, namespace, correlationId, templateResourceName,
			templateResourceUID, templateID, image, i, host, port);
	    }
	} else {
	    List<Integer> missingConfigMaps = IntStream.rangeClosed(1, instances).boxed().collect(Collectors.toList());
	    int namePrefixLength = (templateID + CONFIGMAP_EMAIL_NAME).length();
	    for (ConfigMap configMap : existingConfigMaps) {
		String name = configMap.getMetadata().getName();
		String instance = name.substring(namePrefixLength);
		try {
		    missingConfigMaps.remove(Integer.valueOf(instance));
		} catch (NumberFormatException e) {
		    LOGGER.error(formatLogMessage(correlationId, "Error while getting integer value of " + instance),
			    e);
		}
	    }
	    if (missingConfigMaps.isEmpty()) {
		LOGGER.trace(formatLogMessage(correlationId, "All Email Config Maps existing already"));
	    } else {
		LOGGER.trace(formatLogMessage(correlationId, "Some Email Config Maps need to be created"));
	    }
	    for (int i : missingConfigMaps) {
		createAndApplyEmailConfigMap(client, namespace, correlationId, templateResourceName,
			templateResourceUID, templateID, image, i, host, port);
	    }
	}
    }

    protected void createAndApplyIngress(DefaultKubernetesClient client, String namespace, String correlationId,
	    String templateResourceName, String templateResourceUID, String templateID, String host) {
	/* create yaml based on template */
	Map<String, String> replacements = getIngressReplacements(templateID, namespace, host);
	String ingressYaml;
	try {
	    ingressYaml = ResourceUtil.readResourceAndReplacePlaceholders(DefaultTemplateAddedHandler.class,
		    TEMPLATE_INGRESS_YAML, replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while adjusting template for ingress."), e);
	    return;
	}

	try (ByteArrayInputStream inputStream = new ByteArrayInputStream(ingressYaml.getBytes())) {
	    /* prepare new ingress */
	    NonNamespaceOperation<Ingress, IngressList, Resource<Ingress>> ingresses = client.network().v1().ingresses()
		    .inNamespace(namespace);
	    LOGGER.trace(formatLogMessage(correlationId, "Loading new ingress:\n" + ingressYaml));
	    Ingress newIngress = ingresses.load(inputStream).get();
	    newIngress.getMetadata().getOwnerReferences().get(0).setUid(templateResourceUID);
	    newIngress.getMetadata().getOwnerReferences().get(0).setName(templateResourceName);

	    /* apply new deployment */
	    LOGGER.trace(formatLogMessage(correlationId, "Creating new ingress."));
	    ingresses.create(newIngress);
	    LOGGER.info(formatLogMessage(correlationId, "Created a new ingress"));
	} catch (IOException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error with template stream"), e);
	    return;
	}
	return;
    }

    protected void createAndApplyService(DefaultKubernetesClient client, String namespace, String correlationId,
	    String templateResourceName, String templateResourceUID, String templateID, int instance, int port) {
	/* create yaml based on template */
	Map<String, String> replacements = getServiceReplacements(templateID, instance, port, namespace);
	String serviceYaml;
	try {
	    serviceYaml = ResourceUtil.readResourceAndReplacePlaceholders(DefaultTemplateAddedHandler.class,
		    TEMPLATE_SERVICE_YAML, replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(
		    formatLogMessage(correlationId, "Error while adjusting template for instance number " + instance),
		    e);
	    return;
	}

	try (ByteArrayInputStream inputStream = new ByteArrayInputStream(serviceYaml.getBytes())) {
	    /* prepare new service */
	    NonNamespaceOperation<Service, ServiceList, ServiceResource<Service>> services = client.services()
		    .inNamespace(namespace);
	    LOGGER.trace(formatLogMessage(correlationId,
		    "Loading new service for instance number " + instance + " :\n" + serviceYaml));
	    Service newService = services.load(inputStream).get();
	    newService.getMetadata().getOwnerReferences().get(0).setUid(templateResourceUID);
	    newService.getMetadata().getOwnerReferences().get(0).setName(templateResourceName);

	    /* apply new deployment */
	    LOGGER.trace(formatLogMessage(correlationId, "Creating new service for instance number " + instance));
	    services.create(newService);
	    LOGGER.info(formatLogMessage(correlationId, "Created a new service for instance number " + instance));
	} catch (IOException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error with template stream for instance number " + instance),
		    e);
	    return;
	}
	return;
    }

    protected void createAndApplyDeployment(DefaultKubernetesClient client, String namespace, String correlationId,
	    String templateResourceName, String templateResourceUID, String templateID, String image, int instance,
	    int port) {
	/* create yaml based on template */
	Map<String, String> replacements = getDeploymentsReplacements(templateID, image, instance, port, namespace);
	String deploymentYaml;
	try {
	    deploymentYaml = ResourceUtil.readResourceAndReplacePlaceholders(DefaultTemplateAddedHandler.class,
		    TEMPLATE_DEPLOYMENT_YAML, replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(
		    formatLogMessage(correlationId, "Error while adjusting template for instance number " + instance),
		    e);
	    return;
	}

	try (ByteArrayInputStream inputStream = new ByteArrayInputStream(deploymentYaml.getBytes())) {
	    /* prepare new deployment */
	    NonNamespaceOperation<Deployment, DeploymentList, RollableScalableResource<Deployment>> deployments = client
		    .apps().deployments().inNamespace(namespace);
	    LOGGER.trace(formatLogMessage(correlationId,
		    "Loading new deployment for instance number " + instance + " :\n" + deploymentYaml));
	    Deployment newDeployment = deployments.load(inputStream).get();
	    newDeployment.getMetadata().getOwnerReferences().get(0).setUid(templateResourceUID);
	    newDeployment.getMetadata().getOwnerReferences().get(0).setName(templateResourceName);

	    /* apply new deployment */
	    LOGGER.trace(formatLogMessage(correlationId, "Creating new deployment for instance number " + instance));
	    deployments.create(newDeployment);
	    LOGGER.info(formatLogMessage(correlationId, "Created a new deployment for instance number " + instance));
	} catch (IOException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error with template stream for instance number " + instance),
		    e);
	    return;
	}
	return;
    }

    protected void createAndApplyProxyConfigMap(DefaultKubernetesClient client, String namespace, String correlationId,
	    String templateResourceName, String templateResourceUID, String templateID, String image, int instance,
	    String templateHost, int port) {
	/* create yaml based on template */
	Map<String, String> replacements = getProxyConfigMapReplacements(templateID, image, instance, namespace);
	String configMapYaml;
	try {
	    configMapYaml = ResourceUtil.readResourceAndReplacePlaceholders(DefaultTemplateAddedHandler.class,
		    TEMPLATE_CONFIGMAP_YAML, replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(
		    formatLogMessage(correlationId, "Error while adjusting template for instance number " + instance),
		    e);
	    return;
	}

	try (ByteArrayInputStream inputStream = new ByteArrayInputStream(configMapYaml.getBytes())) {
	    /* prepare new configmap */
	    NonNamespaceOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>> configMaps = client.configMaps()
		    .inNamespace(namespace);
	    LOGGER.trace(formatLogMessage(correlationId,
		    "Loading new proxy config map for instance number " + instance + " :\n" + configMapYaml));
	    ConfigMap newConfigMap = configMaps.load(inputStream).get();
	    newConfigMap.getMetadata().getOwnerReferences().get(0).setUid(templateResourceUID);
	    newConfigMap.getMetadata().getOwnerReferences().get(0).setName(templateResourceName);

	    /* get data from global config and update host */
	    String host = templateID + "." + instance + "." + templateHost;
	    ConfigMap templateConfigMap = client.configMaps().inNamespace(namespace)
		    .withName(OAUTH2_PROXY_CONFIGMAP_NAME).get();
	    Map<String, String> data = new LinkedHashMap<>(templateConfigMap.getData());
	    data.put(OAUTH2_PROXY_CFG, data.get(OAUTH2_PROXY_CFG)//
		    .replace(CONFIGMAP_DATA_PLACEHOLDER_HOST, "https://" + host)//
		    .replace(CONFIGMAP_DATA_PLACEHOLDER_PORT, String.valueOf(port)));
	    newConfigMap.setData(data);

	    /* apply new config map */
	    LOGGER.trace(
		    formatLogMessage(correlationId, "Creating new proxy config map for instance number " + instance));
	    configMaps.create(newConfigMap);
	    LOGGER.info(
		    formatLogMessage(correlationId, "Created a new proxy config map for instance number " + instance));
	} catch (IOException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error with template stream for instance number " + instance),
		    e);
	    return;
	}
	return;
    }

    protected void createAndApplyEmailConfigMap(DefaultKubernetesClient client, String namespace, String correlationId,
	    String templateResourceName, String templateResourceUID, String templateID, String image, int instance,
	    String templateHost, int port) {
	/* create yaml based on template */
	Map<String, String> replacements = getEmailConfigMapReplacements(templateID, image, instance, namespace);
	String configMapYaml;
	try {
	    configMapYaml = ResourceUtil.readResourceAndReplacePlaceholders(DefaultTemplateAddedHandler.class,
		    TEMPLATE_CONFIGMAP_EMAILS_YAML, replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(
		    formatLogMessage(correlationId, "Error while adjusting template for instance number " + instance),
		    e);
	    return;
	}

	try (ByteArrayInputStream inputStream = new ByteArrayInputStream(configMapYaml.getBytes())) {
	    /* prepare new configmap */
	    NonNamespaceOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>> configMaps = client.configMaps()
		    .inNamespace(namespace);
	    LOGGER.trace(formatLogMessage(correlationId,
		    "Loading new email config map for instance number " + instance + " :\n" + configMapYaml));
	    ConfigMap newConfigMap = configMaps.load(inputStream).get();
	    newConfigMap.getMetadata().getOwnerReferences().get(0).setUid(templateResourceUID);
	    newConfigMap.getMetadata().getOwnerReferences().get(0).setName(templateResourceName);

	    /* apply new config map */
	    LOGGER.trace(
		    formatLogMessage(correlationId, "Creating new email config map for instance number " + instance));
	    configMaps.create(newConfigMap);
	    LOGGER.info(
		    formatLogMessage(correlationId, "Created a new email config map for instance number " + instance));
	} catch (IOException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error with template stream for instance number " + instance),
		    e);
	    return;
	}
	return;
    }

    protected Map<String, String> getIngressReplacements(String templateID, String namespace, String host) {
	Map<String, String> replacements = new LinkedHashMap<String, String>();
	replacements.put(PLACEHOLDER_INGRESSNAME, templateID + INGRESS_NAME);
	replacements.put(PLACEHOLDER_NAMESPACE, namespace);
	replacements.put(PLACEHOLDER_HOST, host);
	return replacements;
    }

    protected Map<String, String> getServiceReplacements(String templateID, int instance, int port, String namespace) {
	Map<String, String> replacements = new LinkedHashMap<String, String>();
	replacements.put(PLACEHOLDER_SERVICENAME, templateID + SERVICE_NAME + instance);
	replacements.put(PLACEHOLDER_APP, templateID + "-" + instance);
	replacements.put(PLACEHOLDER_NAMESPACE, namespace);
	replacements.put(PLACEHOLDER_PORT, String.valueOf(port));
	return replacements;
    }

    protected Map<String, String> getDeploymentsReplacements(String templateID, String image, int instance, int port,
	    String namespace) {
	Map<String, String> replacements = new LinkedHashMap<String, String>();
	replacements.put(PLACEHOLDER_DEPLOYMENTNAME, templateID + DEPLOYMENT_NAME + instance);
	replacements.put(PLACEHOLDER_NAMESPACE, namespace);
	replacements.put(PLACEHOLDER_APP, templateID + "-" + instance);
	replacements.put(PLACEHOLDER_TEMPLATENAME, templateID);
	replacements.put(PLACEHOLDER_IMAGE, image);
	replacements.put(PLACEHOLDER_CONFIGNAME, templateID + CONFIGMAP_PROXY_NAME + instance);
	replacements.put(PLACEHOLDER_EMAILSCONFIGNAME, templateID + CONFIGMAP_EMAIL_NAME + instance);
	replacements.put(PLACEHOLDER_PORT, String.valueOf(port));
	return replacements;
    }

    protected Map<String, String> getProxyConfigMapReplacements(String templateID, String image, int instance,
	    String namespace) {
	Map<String, String> replacements = new LinkedHashMap<String, String>();
	replacements.put(PLACEHOLDER_CONFIGNAME, templateID + CONFIGMAP_PROXY_NAME + instance);
	replacements.put(PLACEHOLDER_NAMESPACE, namespace);
	return replacements;
    }

    protected Map<String, String> getEmailConfigMapReplacements(String templateID, String image, int instance,
	    String namespace) {
	Map<String, String> replacements = new LinkedHashMap<String, String>();
	replacements.put(PLACEHOLDER_EMAILSCONFIGNAME, templateID + CONFIGMAP_EMAIL_NAME + instance);
	replacements.put(PLACEHOLDER_NAMESPACE, namespace);
	return replacements;
    }

}
