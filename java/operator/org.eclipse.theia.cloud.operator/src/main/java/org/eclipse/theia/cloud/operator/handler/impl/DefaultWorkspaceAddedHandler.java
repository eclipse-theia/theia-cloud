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

import java.util.Collections;
import java.util.List;
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

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
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

public class DefaultWorkspaceAddedHandler implements WorkspaceAddedHandler {

    private static final Logger LOGGER = LogManager.getLogger(DefaultWorkspaceAddedHandler.class);

    protected static final String FILENAME_AUTHENTICATED_EMAILS_LIST = "authenticated-emails-list";

    @Override
    public boolean handle(DefaultKubernetesClient client, WorkspaceSpecResource workspace, String namespace,
	    String correlationId) {
	WorkspaceSpec spec = workspace.getSpec();
	LOGGER.info(formatLogMessage(correlationId, "Handling " + spec));

	String workspaceResourceName = workspace.getMetadata().getName();
	String workspaceResourceUID = workspace.getMetadata().getUid();

	String templateID = spec.getTemplate();
	String userEmail = spec.getUser();

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
		    .edit(deployment -> addOwnerReferenceToDeployment(correlationId, workspaceResourceName,
			    workspaceResourceUID, deployment));
	} catch (KubernetesClientException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while editing deployment "
		    + (templateID + DefaultTemplateAddedHandler.DEPLOYMENT_NAME + instance)), e);
	    return false;
	}

	/* add user to allowed emails */
	try {
	    client.configMaps().inNamespace(namespace)
		    .withName(templateID + DefaultTemplateAddedHandler.CONFIGMAP_EMAIL_NAME + instance)
		    .edit(configmap -> {
			configmap.setData(Collections.singletonMap(FILENAME_AUTHENTICATED_EMAILS_LIST, userEmail));
			return configmap;
		    });
	} catch (KubernetesClientException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while editing email configmap "
		    + (templateID + DefaultTemplateAddedHandler.CONFIGMAP_EMAIL_NAME + instance)), e);
	    return false;
	}

	/* adjust the ingress */
	try {
	    updateIngress(client, namespace, ingress, serviceToUse, templateID, instance, port);
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

    protected synchronized void updateIngress(DefaultKubernetesClient client, String namespace,
	    Optional<Ingress> ingress, Optional<Service> serviceToUse, String templateID, int instance, int port) {
	client.network().v1().ingresses().inNamespace(namespace).withName(ingress.get().getMetadata().getName())
		.edit(ingressToUpdate -> {
		    IngressRule firstIngressRule = ingressToUpdate.getSpec().getRules().get(0);

		    IngressRule ingressRule = new IngressRule();
		    ingressToUpdate.getSpec().getRules().add(ingressRule);

		    String host = templateID + "." + instance + "." + firstIngressRule.getHost();
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
		    serviceBackendPort.setNumber(port);

		    return ingressToUpdate;
		});
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

    protected static boolean isUnusedService(Service service) {
	return service.getMetadata().getOwnerReferences().size() == 1;
    }

}
