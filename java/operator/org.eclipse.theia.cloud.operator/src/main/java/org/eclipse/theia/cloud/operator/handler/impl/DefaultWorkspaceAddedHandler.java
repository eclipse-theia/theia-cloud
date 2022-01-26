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
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.operator.handler.K8sUtil;
import org.eclipse.theia.cloud.operator.handler.WorkspaceAddedHandler;
import org.eclipse.theia.cloud.operator.resource.TemplateSpecResource;
import org.eclipse.theia.cloud.operator.resource.TemplateSpecResourceList;
import org.eclipse.theia.cloud.operator.resource.WorkspaceSpec;
import org.eclipse.theia.cloud.operator.resource.WorkspaceSpecResource;
import org.eclipse.theia.cloud.operator.util.JavaUtil;
import org.eclipse.theia.cloud.operator.util.ResourceUtil;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressRuleValue;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBackend;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRule;
import io.fabric8.kubernetes.api.model.networking.v1.IngressServiceBackend;
import io.fabric8.kubernetes.api.model.networking.v1.ServiceBackendPort;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;

public class DefaultWorkspaceAddedHandler implements WorkspaceAddedHandler {

    private static final Logger LOGGER = LogManager.getLogger(DefaultWorkspaceAddedHandler.class);

    public static final String OAUTH2_PROXY_CONFIGMAP_NAME = "oauth2-proxy-config";
    public static final String OAUTH2_PROXY_CFG = "oauth2-proxy.cfg";

    protected static final String PLACEHOLDER_CONFIGNAME = "placeholder-configname";
    protected static final String PLACEHOLDER_NAMESPACE = DefaultTemplateAddedHandler.PLACEHOLDER_NAMESPACE;

    protected static final String TEMPLATE_O_AUTH_SIDECAR_YAML = "/templateOAuthSidecar.yaml";
    protected static final String TEMPLATE_CONFIGMAP_YAML = "/templateConfigmap.yaml";

    @Override
    public boolean handle(DefaultKubernetesClient client, WorkspaceSpecResource workspace, String namespace,
	    String correlationId) {
	WorkspaceSpec spec = workspace.getSpec();
	LOGGER.info(formatLogMessage(correlationId, "Handling " + spec));

	String workspaceResourceName = workspace.getMetadata().getName();
	String workspaceResourceUID = workspace.getMetadata().getUid();

	String workspaceName = spec.getName();
	String templateID = spec.getTemplate();
	String user = spec.getUser();

	/* find template for workspace */
	Optional<TemplateSpecResource> template = client
		.customResources(TemplateSpecResource.class, TemplateSpecResourceList.class).inNamespace(namespace)
		.list().getItems().stream()//
		.filter(templateSpecResource -> templateID.equals(templateSpecResource.getSpec().getName()))//
		.findAny();
	if (template.isEmpty()) {
	    LOGGER.error(formatLogMessage(correlationId, "No Template with name " + templateID + " found."));
	    return false;
	}

	String templateResourceName = template.get().getMetadata().getName();
	String templateResourceUID = template.get().getMetadata().getUid();

	/* find ingress */
	Optional<Ingress> ingress = K8sUtil.getExistingIngress(client, namespace, templateResourceName,
		templateResourceUID);
	if (ingress.isEmpty()) {
	    LOGGER.error(formatLogMessage(correlationId, "No Ingress for template " + templateID + " found."));
	    return false;
	}

	/* get a service to use */
	Entry<Optional<Service>, Boolean> reserveServiceResult = reserveService(client, namespace, templateResourceName,
		templateResourceUID, templateID, workspaceResourceName, workspaceResourceUID, correlationId);
	if (reserveServiceResult.getValue()) {
	    LOGGER.info(formatLogMessage(correlationId, "Found an already reserved service"));
	    return true;
	}
	Optional<Service> serviceToUse = reserveServiceResult.getKey();
	if (serviceToUse.isEmpty()) {
	    LOGGER.error(formatLogMessage(correlationId, "No Service for template " + templateID + " found."));
	    return false;
	}

	/* Create oauth config map and set hostname */
	String host = getHost(user, workspaceName, ingress.get());
	ConfigMap templateConfigMap = client.configMaps().inNamespace(namespace).withName(OAUTH2_PROXY_CONFIGMAP_NAME)
		.get();
	Map<String, String> data = new LinkedHashMap<>(templateConfigMap.getData());
	data.put(OAUTH2_PROXY_CFG, data.get(OAUTH2_PROXY_CFG).replace("https://placeholder", "https://" + host));
	boolean configMapCreationSuccessful = createAndApplyConfigMap(client, namespace, correlationId,
		workspaceResourceName, workspaceResourceUID, user, workspaceName, data);
	if (!configMapCreationSuccessful) {
	    LOGGER.error(formatLogMessage(correlationId, "Could not create configmap"));
	    return false;
	}

	/* get the deployment for the service */
	int namePrefixLength = (templateID + DefaultTemplateAddedHandler.SERVICE_NAME).length();
	String instanceString = serviceToUse.get().getMetadata().getName().substring(namePrefixLength);
	Integer instance;
	try {
	    instance = Integer.valueOf(serviceToUse.get().getMetadata().getName().substring(namePrefixLength));
	} catch (NumberFormatException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while getting integer value of " + instanceString), e);
	    return false;
	}

	try {
	    client.apps().deployments().inNamespace(namespace)
		    .withName(templateID + DefaultTemplateAddedHandler.DEPLOYMENT_NAME + instance)
		    .edit(deployment -> adjustDeployment(client, correlationId, workspaceResourceName,
			    workspaceResourceUID, namespace, user, workspaceName, deployment));
	} catch (KubernetesClientException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while editing deployment "
		    + (templateID + DefaultTemplateAddedHandler.DEPLOYMENT_NAME + instance)), e);
	    return false;
	}

	/* adjust the ingress */
	try {
	    updateIngress(client, namespace, user, workspaceName, ingress, serviceToUse);
	} catch (KubernetesClientException e) {
	    LOGGER.error(formatLogMessage(correlationId,
		    "Error while editing ingress " + ingress.get().getMetadata().getName()), e);
	    return false;
	}

	return true;
    }

    protected synchronized Entry<Optional<Service>, Boolean> reserveService(DefaultKubernetesClient client,
	    String namespace, String templateResourceName, String templateResourceUID, String templateID,
	    String workspaceResourceName, String workspaceResourceUID, String correlationId) {
	List<Service> existingServices = K8sUtil.getExistingServices(client, namespace, templateResourceName,
		templateResourceUID);

	Optional<Service> alreadyReservedService = existingServices.stream()//
		.filter(service -> {
		    if (isUnusedService(service)) {
			return false;
		    }
		    for (OwnerReference ownerReference : service.getMetadata().getOwnerReferences()) {
			if (workspaceResourceName.equals(ownerReference.getName())
				&& workspaceResourceUID.equals(ownerReference.getUid())) {
			    return true;
			}
		    }
		    return false;
		})//
		.findAny();
	if (alreadyReservedService.isPresent()) {
	    return JavaUtil.tuple(alreadyReservedService, true);
	}

	Optional<Service> serviceToUse = existingServices.stream()//
		.filter(DefaultWorkspaceAddedHandler::isUnusedService)//
		.findAny();
	if (serviceToUse.isEmpty()) {
	    return JavaUtil.tuple(serviceToUse, false);
	}
	try {
	    client.services().inNamespace(namespace).withName(serviceToUse.get().getMetadata().getName())
		    .edit(service -> addOwnerReferenceToService(correlationId, workspaceResourceName,
			    workspaceResourceUID, serviceToUse, service));
	} catch (KubernetesClientException e) {
	    LOGGER.error(formatLogMessage(correlationId,
		    "Error while editing service " + (serviceToUse.get().getMetadata().getName())), e);
	    return JavaUtil.tuple(Optional.empty(), false);
	}
	return JavaUtil.tuple(serviceToUse, false);
    }

    protected synchronized void updateIngress(DefaultKubernetesClient client, String namespace, String user,
	    String workspaceName, Optional<Ingress> ingress, Optional<Service> serviceToUse) {
	client.network().v1().ingresses().inNamespace(namespace).withName(ingress.get().getMetadata().getName())
		.edit(ingressToUpdate -> {
		    String host = getHost(user, workspaceName, ingressToUpdate);

		    IngressRule ingressRule = new IngressRule();
		    ingressToUpdate.getSpec().getRules().add(ingressRule);
		    ingressRule.setHost(host);

		    HTTPIngressRuleValue http = new HTTPIngressRuleValue();
		    ingressRule.setHttp(http);

		    HTTPIngressPath httpIngressPath = new HTTPIngressPath();
		    http.getPaths().add(httpIngressPath);
		    httpIngressPath.setPath("/");
		    httpIngressPath.setPathType("Prefix");

		    IngressBackend ingressBackend = new IngressBackend();
		    httpIngressPath.setBackend(ingressBackend);

		    IngressServiceBackend ingressServiceBackend = new IngressServiceBackend();
		    ingressBackend.setService(ingressServiceBackend);
		    ingressServiceBackend.setName(serviceToUse.get().getMetadata().getName());

		    ServiceBackendPort serviceBackendPort = new ServiceBackendPort();
		    ingressServiceBackend.setPort(serviceBackendPort);
		    serviceBackendPort.setNumber(3000);// TODO port should move to spec

		    return ingressToUpdate;
		});
    }

    protected String getHost(String user, String workspaceName, Ingress ingress) {
	IngressRule firstIngressRule = ingress.getSpec().getRules().get(0);
	return user + "." + workspaceName + "." + firstIngressRule.getHost();
    }

    protected String getConfigMapName(String user, String workspaceName) {
	return user + "-" + workspaceName + "-oauth2-proxy-config";
    }

    protected Deployment adjustDeployment(DefaultKubernetesClient client, String correlationId,
	    String workspaceResourceName, String workspaceResourceUID, String namespace, String user,
	    String workspaceName, Deployment deployment) {

	deployment = addOwnerReferenceToDeployment(correlationId, workspaceResourceName, workspaceResourceUID,
		deployment);

	/* create yaml based on template */
	Map<String, String> replacements = getSidecarReplacements(user, workspaceName);
	String deploymentYaml;
	try {
	    deploymentYaml = ResourceUtil.readResourceAndReplacePlaceholders(DefaultWorkspaceAddedHandler.class,
		    TEMPLATE_O_AUTH_SIDECAR_YAML, replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while adjusting template"), e);
	    return deployment;
	}

	try (ByteArrayInputStream inputStream = new ByteArrayInputStream(deploymentYaml.getBytes())) {
	    /* prepare new configmap */
	    NonNamespaceOperation<Deployment, DeploymentList, RollableScalableResource<Deployment>> deployments = client
		    .apps().deployments().inNamespace(namespace);
	    LOGGER.trace(formatLogMessage(correlationId, "Loading new side car deployment:\n" + deploymentYaml));
	    Deployment sidecar = deployments.load(inputStream).get();

	    PodSpec sidecarSpec = sidecar.getSpec().getTemplate().getSpec();
	    PodSpec deploymentSpec = deployment.getSpec().getTemplate().getSpec();

	    Container oAuthContainer = sidecarSpec.getContainers().get(0);
	    deploymentSpec.getContainers().add(oAuthContainer);

	    List<Volume> volumes = sidecarSpec.getVolumes();
	    deploymentSpec.getVolumes().addAll(volumes);

	} catch (IOException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error with template stream"), e);
	    return deployment;
	}

	return deployment;
    }

    protected Deployment addOwnerReferenceToDeployment(String correlationId, String workspaceResourceName,
	    String workspaceResourceUID, Deployment deployment) {
	OwnerReference ownerReference = createOwnerReference(workspaceResourceName, workspaceResourceUID);
	LOGGER.info(formatLogMessage(correlationId,
		"Adding a new owner reference to deployment " + deployment.getMetadata().getName()));
	deployment.addOwnerReference(ownerReference);
	return deployment;
    }

    protected Service addOwnerReferenceToService(String correlationId, String workspaceResourceName,
	    String workspaceResourceUID, Optional<Service> serviceToUse, Service service) {
	OwnerReference serviceOwnerReference = createOwnerReference(workspaceResourceName, workspaceResourceUID);
	LOGGER.info(formatLogMessage(correlationId,
		"Adding a new owner reference to service " + serviceToUse.get().getMetadata().getName()));
	service.getMetadata().getOwnerReferences().add(serviceOwnerReference);
	return service;
    }

    protected OwnerReference createOwnerReference(String workspaceResourceName, String workspaceResourceUID) {
	OwnerReference ownerReference = new OwnerReference();
	ownerReference.setApiVersion(HasMetadata.getApiVersion(WorkspaceSpecResource.class));
	ownerReference.setKind(WorkspaceSpec.KIND);
	ownerReference.setName(workspaceResourceName);
	ownerReference.setUid(workspaceResourceUID);
	return ownerReference;
    }

    protected boolean createAndApplyConfigMap(DefaultKubernetesClient client, String namespace, String correlationId,
	    String workspaceResourceName, String workspaceResourceUID, String user, String workspaceName,
	    Map<String, String> data) {
	/* create yaml based on template */
	Map<String, String> replacements = getConfigMapReplacements(user, workspaceName, namespace);
	String configMapYaml;
	try {
	    configMapYaml = ResourceUtil.readResourceAndReplacePlaceholders(DefaultWorkspaceAddedHandler.class,
		    TEMPLATE_CONFIGMAP_YAML, replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while adjusting template"), e);
	    return false;
	}

	try (ByteArrayInputStream inputStream = new ByteArrayInputStream(configMapYaml.getBytes())) {
	    /* prepare new configmap */
	    NonNamespaceOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>> configMaps = client.configMaps()
		    .inNamespace(namespace);
	    LOGGER.trace(formatLogMessage(correlationId, "Loading new config map:\n" + configMapYaml));
	    ConfigMap configMap = configMaps.load(inputStream).get();
	    configMap.getMetadata().getOwnerReferences().get(0).setUid(workspaceResourceUID);
	    configMap.getMetadata().getOwnerReferences().get(0).setName(workspaceResourceName);
	    configMap.setData(data);

	    /* apply new deployment */
	    LOGGER.trace(formatLogMessage(correlationId, "Creating new config map"));
	    configMaps.create(configMap);
	    LOGGER.info(formatLogMessage(correlationId, "Created a new config map"));
	} catch (IOException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error with template stream"), e);
	    return false;
	}
	return true;
    }

    protected Map<String, String> getConfigMapReplacements(String user, String workspaceName, String namespace) {
	Map<String, String> replacements = new LinkedHashMap<String, String>();
	replacements.put(PLACEHOLDER_CONFIGNAME, getConfigMapName(user, workspaceName));
	replacements.put(PLACEHOLDER_NAMESPACE, namespace);
	return replacements;
    }

    protected Map<String, String> getSidecarReplacements(String user, String workspaceName) {
	Map<String, String> replacements = new LinkedHashMap<String, String>();
	replacements.put(PLACEHOLDER_CONFIGNAME, getConfigMapName(user, workspaceName));
	return replacements;
    }

    protected static boolean isUnusedService(Service service) {
	return service.getMetadata().getOwnerReferences().size() == 1;
    }

}
