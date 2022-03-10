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

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.common.k8s.resource.WorkspaceSpec;
import org.eclipse.theia.cloud.operator.TheiaCloudArguments;
import org.eclipse.theia.cloud.operator.handler.IngressPathProvider;
import org.eclipse.theia.cloud.operator.handler.K8sUtil;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudConfigMapUtil;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudDeploymentUtil;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudHandlerUtil;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudServiceUtil;
import org.eclipse.theia.cloud.operator.handler.WorkspaceAddedHandler;
import org.eclipse.theia.cloud.operator.resource.TemplateSpecResource;
import org.eclipse.theia.cloud.operator.util.JavaUtil;

import com.google.inject.Inject;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressRuleValue;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBackend;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRule;
import io.fabric8.kubernetes.api.model.networking.v1.IngressServiceBackend;
import io.fabric8.kubernetes.api.model.networking.v1.ServiceBackendPort;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;

/**
 * A {@link WorkspaceAddedHandler} that relies on the fact that the template
 * handler created spare deployments to use.
 */
public class EagerStartWorkspaceAddedHandler implements WorkspaceAddedHandler {

    private static final Logger LOGGER = LogManager.getLogger(EagerStartWorkspaceAddedHandler.class);

    protected TheiaCloudArguments arguments;
    protected IngressPathProvider ingressPathProvider;

    @Inject
    public EagerStartWorkspaceAddedHandler(TheiaCloudArguments arguments, IngressPathProvider ingressPathProvider) {
	this.arguments = arguments;
	this.ingressPathProvider = ingressPathProvider;
    }

    @Override
    public boolean handle(DefaultKubernetesClient client, Workspace workspace, String namespace, String correlationId) {
	WorkspaceSpec spec = workspace.getSpec();
	LOGGER.info(formatLogMessage(correlationId, "Handling " + spec));

	String workspaceResourceName = workspace.getMetadata().getName();
	String workspaceResourceUID = workspace.getMetadata().getUid();

	String templateID = spec.getTemplate();
	String userEmail = spec.getUser();

	/* find template for workspace */
	Optional<TemplateSpecResource> template = TheiaCloudHandlerUtil.getTemplateSpecForWorkspace(client, namespace,
		templateID);
	if (template.isEmpty()) {
	    LOGGER.error(formatLogMessage(correlationId, "No Template with name " + templateID + " found."));
	    return false;
	}

	String templateResourceName = template.get().getMetadata().getName();
	String templateResourceUID = template.get().getMetadata().getUid();
	int port = template.get().getSpec().getPort();

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
	}

	/* get the deployment for the service and add as owner */
	Integer instance = TheiaCloudServiceUtil.getId(correlationId, template.get(), serviceToUse.get());
	if (instance == null) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while getting instance from Service"));
	    return false;
	}

	try {
	    client.apps().deployments().inNamespace(namespace)
		    .withName(TheiaCloudDeploymentUtil.getDeploymentName(template.get(), instance))
		    .edit(deployment -> TheiaCloudHandlerUtil.addOwnerReferenceToItem(correlationId,
			    workspaceResourceName, workspaceResourceUID, deployment));
	} catch (KubernetesClientException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while editing deployment "
		    + (templateID + TheiaCloudDeploymentUtil.DEPLOYMENT_NAME + instance)), e);
	    return false;
	}

	if (arguments.isUseKeycloak()) {
	    /* add user to allowed emails */
	    try {
		client.configMaps().inNamespace(namespace)
			.withName(TheiaCloudConfigMapUtil.getEmailConfigName(template.get(), instance))
			.edit(configmap -> {
			    configmap.setData(Collections.singletonMap(AddedHandler.FILENAME_AUTHENTICATED_EMAILS_LIST,
				    userEmail));
			    return configmap;
			});
	    } catch (KubernetesClientException e) {
		LOGGER.error(formatLogMessage(correlationId, "Error while editing email configmap "
			+ (templateID + TheiaCloudConfigMapUtil.CONFIGMAP_EMAIL_NAME + instance)), e);
		return false;
	    }
	}

	/* adjust the ingress */
	String host;
	try {
	    host = updateIngress(client, namespace, ingress, serviceToUse, templateID, instance, port, template.get());
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

    protected synchronized Entry<Optional<Service>, Boolean> reserveService(DefaultKubernetesClient client,
	    String namespace, String templateResourceName, String templateResourceUID, String templateID,
	    String workspaceResourceName, String workspaceResourceUID, String correlationId) {
	List<Service> existingServices = K8sUtil.getExistingServices(client, namespace, templateResourceName,
		templateResourceUID);

	Optional<Service> alreadyReservedService = TheiaCloudServiceUtil
		.getServiceOwnedByWorkspace(workspaceResourceName, workspaceResourceUID, existingServices);
	if (alreadyReservedService.isPresent()) {
	    return JavaUtil.tuple(alreadyReservedService, true);
	}

	Optional<Service> serviceToUse = TheiaCloudServiceUtil.getUnusedService(existingServices);
	if (serviceToUse.isEmpty()) {
	    return JavaUtil.tuple(serviceToUse, false);
	}

	/* add our workspace as owner to the service */
	try {
	    client.services().inNamespace(namespace).withName(serviceToUse.get().getMetadata().getName())
		    .edit(service -> TheiaCloudHandlerUtil.addOwnerReferenceToItem(correlationId, workspaceResourceName,
			    workspaceResourceUID, service));
	} catch (KubernetesClientException e) {
	    LOGGER.error(formatLogMessage(correlationId,
		    "Error while editing service " + (serviceToUse.get().getMetadata().getName())), e);
	    return JavaUtil.tuple(Optional.empty(), false);
	}
	return JavaUtil.tuple(serviceToUse, false);
    }

    protected synchronized String updateIngress(DefaultKubernetesClient client, String namespace,
	    Optional<Ingress> ingress, Optional<Service> serviceToUse, String templateID, int instance, int port,
	    TemplateSpecResource template) {
	String host = template.getSpec().getHost();
	String path = ingressPathProvider.getPath(template, instance);
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
		    serviceBackendPort.setNumber(port);

		    return ingressToUpdate;
		});
	return host + path + "/";
    }

}
