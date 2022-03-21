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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.common.k8s.resource.WorkspaceSpec;
import org.eclipse.theia.cloud.operator.TheiaCloudArguments;
import org.eclipse.theia.cloud.operator.handler.IngressPathProvider;
import org.eclipse.theia.cloud.operator.handler.K8sUtil;
import org.eclipse.theia.cloud.operator.handler.PersistentVolumeHandler;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudConfigMapUtil;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudDeploymentUtil;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudHandlerUtil;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudK8sUtil;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudPersistentVolumeUtil;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudServiceUtil;
import org.eclipse.theia.cloud.operator.handler.WorkspaceAddedHandler;
import org.eclipse.theia.cloud.operator.resource.TemplateSpecResource;
import org.eclipse.theia.cloud.operator.util.JavaResourceUtil;

import com.google.inject.Inject;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressRuleValue;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBackend;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRule;
import io.fabric8.kubernetes.api.model.networking.v1.IngressServiceBackend;
import io.fabric8.kubernetes.api.model.networking.v1.ServiceBackendPort;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;

public class LazyStartWorkspaceAddedHandler implements WorkspaceAddedHandler {

    private static final Logger LOGGER = LogManager.getLogger(LazyStartWorkspaceAddedHandler.class);

    protected PersistentVolumeHandler persistentVolumeHandler;
    protected IngressPathProvider ingressPathProvider;
    protected TheiaCloudArguments arguments;

    @Inject
    public LazyStartWorkspaceAddedHandler(PersistentVolumeHandler persistentVolumeHandler,
	    IngressPathProvider ingressPathProvider, TheiaCloudArguments arguments) {
	this.persistentVolumeHandler = persistentVolumeHandler;
	this.ingressPathProvider = ingressPathProvider;
	this.arguments = arguments;
    }

    @Override
    public boolean handle(DefaultKubernetesClient client, Workspace workspace, String namespace, String correlationId) {
	/* workspace information */
	String workspaceResourceName = workspace.getMetadata().getName();
	String workspaceResourceUID = workspace.getMetadata().getUid();
	WorkspaceSpec workspaceSpec = workspace.getSpec();

	/* find template for workspace */
	String templateID = workspaceSpec.getTemplate();
	Optional<TemplateSpecResource> optionalTemplate = TheiaCloudHandlerUtil.getTemplateSpecForWorkspace(client,
		namespace, templateID);
	if (optionalTemplate.isEmpty()) {
	    LOGGER.error(formatLogMessage(correlationId, "No Template with name " + templateID + " found."));
	    return false;
	}

	/* check if max instances reached already */
	if (TheiaCloudK8sUtil.checkIfMaxInstancesReached(client, namespace, workspaceSpec,
		optionalTemplate.get().getSpec(), correlationId)) {
	    LOGGER.info(formatLogMessage(correlationId,
		    "Max instances for " + templateID + " reached. Cannot create " + workspaceSpec));
	    AddedHandler.updateWorkspaceError(client, workspace, namespace,
		    "Max instances reached. Could not create workspace", correlationId);
	    return false;
	}

	/* handle storage */
	Optional<String> pvcName;
	if (arguments.isEphemeralStorage()) {
	    pvcName = Optional.empty();
	} else {
	    /* create persistent volume if not present already */
	    pvcName = Optional.of(TheiaCloudPersistentVolumeUtil.getPersistentVolumeName(workspace));
	    Optional<PersistentVolumeClaim> volume = K8sUtil.getPersistentVolumeClaim(client, namespace, pvcName.get());
	    if (volume.isEmpty()) {
		LOGGER.debug(formatLogMessage(correlationId,
			"No Volumce Claim with name " + pvcName.get() + " was found. Creating claims."));
		persistentVolumeHandler.createAndApplyPersistentVolume(client, namespace, correlationId, workspace);
		persistentVolumeHandler.createAndApplyPersistentVolumeClaim(client, namespace, correlationId,
			workspace);
	    }
	}

	/* find ingress */
	String templateResourceName = optionalTemplate.get().getMetadata().getName();
	String templateResourceUID = optionalTemplate.get().getMetadata().getUid();
	Optional<Ingress> ingress = K8sUtil.getExistingIngress(client, namespace, templateResourceName,
		templateResourceUID);
	if (ingress.isEmpty()) {
	    LOGGER.error(formatLogMessage(correlationId, "No Ingress for template " + templateID + " found."));
	    return false;
	}

	/* Create service for this workspace */
	List<Service> existingServices = K8sUtil.getExistingServices(client, namespace, workspaceResourceName,
		workspaceResourceUID);
	if (!existingServices.isEmpty()) {
	    LOGGER.warn(formatLogMessage(correlationId,
		    "Existing service for " + workspaceSpec + ". Workspace already running?"));
	    return true;
	}
	Optional<Service> serviceToUse = createAndApplyService(client, namespace, correlationId, workspaceResourceName,
		workspaceResourceUID, workspace, optionalTemplate.get().getSpec().getPort(), arguments.isUseKeycloak());
	if (serviceToUse.isEmpty()) {
	    LOGGER.error(formatLogMessage(correlationId, "Unable to create service for workspace " + workspaceSpec));
	    return false;
	}

	if (arguments.isUseKeycloak()) {
	    /* Create config maps for this workspace */
	    List<ConfigMap> existingConfigMaps = K8sUtil.getExistingConfigMaps(client, namespace, workspaceResourceName,
		    workspaceResourceUID);
	    if (!existingConfigMaps.isEmpty()) {
		LOGGER.warn(formatLogMessage(correlationId,
			"Existing configmaps for " + workspaceSpec + ". Workspace already running?"));
		return true;
	    }
	    createAndApplyEmailConfigMap(client, namespace, correlationId, workspaceResourceName, workspaceResourceUID,
		    workspace);
	    createAndApplyProxyConfigMap(client, namespace, correlationId, workspaceResourceName, workspaceResourceUID,
		    workspace, optionalTemplate.get());
	}

	/* Create deployment for this workspace */
	List<Deployment> existingDeployments = K8sUtil.getExistingDeployments(client, namespace, workspaceResourceName,
		workspaceResourceUID);
	if (!existingDeployments.isEmpty()) {
	    LOGGER.warn(formatLogMessage(correlationId,
		    "Existing deployments for " + workspaceSpec + ". Workspace already running?"));
	    return true;
	}
	createAndApplyDeployment(client, namespace, correlationId, workspaceResourceName, workspaceResourceUID,
		workspace, optionalTemplate.get(), pvcName, arguments.isUseKeycloak());

	/* adjust the ingress */
	String host;
	try {
	    host = updateIngress(client, namespace, ingress, serviceToUse, workspace, optionalTemplate.get());
	} catch (KubernetesClientException e) {
	    LOGGER.error(formatLogMessage(correlationId,
		    "Error while editing ingress " + ingress.get().getMetadata().getName()), e);
	    return false;
	}

	/* Update workspace resource */
	try {
	    AddedHandler.updateWorkspaceURLAsync(client, workspace, namespace, host, correlationId);
	} catch (KubernetesClientException e) {
	    LOGGER.error(formatLogMessage(correlationId,
		    "Error while editing workspace " + workspace.getMetadata().getName()), e);
	    return false;
	}

	return true;
    }

    protected Optional<Service> createAndApplyService(DefaultKubernetesClient client, String namespace,
	    String correlationId, String workspaceResourceName, String workspaceResourceUID, Workspace workspace,
	    int port, boolean useOAuth2Proxy) {
	Map<String, String> replacements = TheiaCloudServiceUtil.getServiceReplacements(namespace, workspace, port);
	String templateYaml = useOAuth2Proxy ? AddedHandler.TEMPLATE_SERVICE_YAML
		: AddedHandler.TEMPLATE_SERVICE_WITHOUT_AOUTH2_PROXY_YAML;
	String serviceYaml;
	try {
	    serviceYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(LazyStartWorkspaceAddedHandler.class,
		    templateYaml, replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while adjusting template for workspace " + workspace),
		    e);
	    return Optional.empty();
	}
	return K8sUtil.loadAndCreateServiceWithOwnerReference(client, namespace, correlationId, serviceYaml,
		WorkspaceSpec.API, WorkspaceSpec.KIND, workspaceResourceName, workspaceResourceUID, 0);
    }

    protected void createAndApplyEmailConfigMap(DefaultKubernetesClient client, String namespace, String correlationId,
	    String workspaceResourceName, String workspaceResourceUID, Workspace workspace) {
	Map<String, String> replacements = TheiaCloudConfigMapUtil.getEmailConfigMapReplacements(namespace, workspace);
	String configMapYaml;
	try {
	    configMapYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(LazyStartWorkspaceAddedHandler.class,
		    AddedHandler.TEMPLATE_CONFIGMAP_EMAILS_YAML, replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while adjusting template for workspace " + workspace),
		    e);
	    return;
	}
	K8sUtil.loadAndCreateConfigMapWithOwnerReference(client, namespace, correlationId, configMapYaml,
		WorkspaceSpec.API, WorkspaceSpec.KIND, workspaceResourceName, workspaceResourceUID, 0, configmap -> {
		    configmap.setData(Collections.singletonMap(AddedHandler.FILENAME_AUTHENTICATED_EMAILS_LIST,
			    workspace.getSpec().getUser()));
		});
    }

    protected void createAndApplyProxyConfigMap(DefaultKubernetesClient client, String namespace, String correlationId,
	    String workspaceResourceName, String workspaceResourceUID, Workspace workspace,
	    TemplateSpecResource template) {
	Map<String, String> replacements = TheiaCloudConfigMapUtil.getProxyConfigMapReplacements(namespace, workspace);
	String configMapYaml;
	try {
	    configMapYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(LazyStartWorkspaceAddedHandler.class,
		    AddedHandler.TEMPLATE_CONFIGMAP_YAML, replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while adjusting template for workspace " + workspace),
		    e);
	    return;
	}
	K8sUtil.loadAndCreateConfigMapWithOwnerReference(client, namespace, correlationId, configMapYaml,
		WorkspaceSpec.API, WorkspaceSpec.KIND, workspaceResourceName, workspaceResourceUID, 0, configMap -> {
		    String host = template.getSpec().getHost() + ingressPathProvider.getPath(template, workspace);
		    int port = template.getSpec().getPort();
		    AddedHandler.updateProxyConfigMap(client, namespace, configMap, host, port);
		});
    }

    protected void createAndApplyDeployment(DefaultKubernetesClient client, String namespace, String correlationId,
	    String workspaceResourceName, String workspaceResourceUID, Workspace workspace,
	    TemplateSpecResource template, Optional<String> pvName, boolean useOAuth2Proxy) {
	Map<String, String> replacements = TheiaCloudDeploymentUtil.getDeploymentsReplacements(namespace, workspace,
		template);
	String templateYaml = useOAuth2Proxy ? AddedHandler.TEMPLATE_DEPLOYMENT_YAML
		: AddedHandler.TEMPLATE_DEPLOYMENT_WITHOUT_AOUTH2_PROXY_YAML;
	String deploymentYaml;
	try {
	    deploymentYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(LazyStartWorkspaceAddedHandler.class,
		    templateYaml, replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while adjusting template for workspace " + workspace),
		    e);
	    return;
	}
	K8sUtil.loadAndCreateDeploymentWithOwnerReference(client, namespace, correlationId, deploymentYaml,
		WorkspaceSpec.API, WorkspaceSpec.KIND, workspaceResourceName, workspaceResourceUID, 0, deployment -> {
		    pvName.ifPresent(name -> persistentVolumeHandler.addVolumeClaim(deployment, name));
		});
    }

    protected synchronized String updateIngress(DefaultKubernetesClient client, String namespace,
	    Optional<Ingress> ingress, Optional<Service> serviceToUse, Workspace workspace,
	    TemplateSpecResource template) {
	String host = template.getSpec().getHost();
	String path = ingressPathProvider.getPath(template, workspace);
	client.network().v1().ingresses().inNamespace(namespace).withName(ingress.get().getMetadata().getName())
		.edit(ingressToUpdate -> {
		    IngressRule ingressRule = new IngressRule();
		    ingressToUpdate.getSpec().getRules().add(ingressRule);

		    ingressRule.setHost(host);

		    HTTPIngressRuleValue http = new HTTPIngressRuleValue();
		    ingressRule.setHttp(http);

		    HTTPIngressPath httpIngressPath = new HTTPIngressPath();
		    http.getPaths().add(httpIngressPath);
		    httpIngressPath.setPath(path + AddedHandler.INGRESS_REWRITE_PATH);
		    httpIngressPath.setPathType("Prefix");

		    IngressBackend ingressBackend = new IngressBackend();
		    httpIngressPath.setBackend(ingressBackend);

		    IngressServiceBackend ingressServiceBackend = new IngressServiceBackend();
		    ingressBackend.setService(ingressServiceBackend);
		    ingressServiceBackend.setName(serviceToUse.get().getMetadata().getName());

		    ServiceBackendPort serviceBackendPort = new ServiceBackendPort();
		    ingressServiceBackend.setPort(serviceBackendPort);
		    serviceBackendPort.setNumber(template.getSpec().getPort());

		    return ingressToUpdate;
		});
	return host + path + "/";
    }

}
