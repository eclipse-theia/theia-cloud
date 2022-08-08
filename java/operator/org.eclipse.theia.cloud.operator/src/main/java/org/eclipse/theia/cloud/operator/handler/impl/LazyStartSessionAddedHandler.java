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
import org.eclipse.theia.cloud.common.k8s.resource.Session;
import org.eclipse.theia.cloud.common.k8s.resource.SessionSpec;
import org.eclipse.theia.cloud.operator.TheiaCloudArguments;
import org.eclipse.theia.cloud.operator.handler.BandwidthLimiter;
import org.eclipse.theia.cloud.operator.handler.IngressPathProvider;
import org.eclipse.theia.cloud.operator.handler.K8sUtil;
import org.eclipse.theia.cloud.operator.handler.PersistentVolumeHandler;
import org.eclipse.theia.cloud.operator.handler.SessionAddedHandler;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudConfigMapUtil;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudDeploymentUtil;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudHandlerUtil;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudK8sUtil;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudPersistentVolumeUtil;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudServiceUtil;
import org.eclipse.theia.cloud.operator.resource.AppDefinitionSpecResource;
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

public class LazyStartSessionAddedHandler implements SessionAddedHandler {

    private static final Logger LOGGER = LogManager.getLogger(LazyStartSessionAddedHandler.class);

    protected PersistentVolumeHandler persistentVolumeHandler;
    protected IngressPathProvider ingressPathProvider;
    protected TheiaCloudArguments arguments;
    protected BandwidthLimiter bandwidthLimiter;

    @Inject
    public LazyStartSessionAddedHandler(PersistentVolumeHandler persistentVolumeHandler,
	    IngressPathProvider ingressPathProvider, TheiaCloudArguments arguments, BandwidthLimiter bandwidthLimiter) {
	this.persistentVolumeHandler = persistentVolumeHandler;
	this.ingressPathProvider = ingressPathProvider;
	this.arguments = arguments;
	this.bandwidthLimiter = bandwidthLimiter;
    }

    @Override
    public boolean handle(DefaultKubernetesClient client, Session session, String namespace, String correlationId) {
	/* session information */
	String sessionResourceName = session.getMetadata().getName();
	String sessionResourceUID = session.getMetadata().getUid();
	SessionSpec sessionSpec = session.getSpec();

	/* find app definition for session */
	String appDefinitionID = sessionSpec.getAppDefinition();
	Optional<AppDefinitionSpecResource> optionalAppDefinition = TheiaCloudHandlerUtil
		.getAppDefinitionSpecForSession(client, namespace, appDefinitionID);
	if (optionalAppDefinition.isEmpty()) {
	    LOGGER.error(formatLogMessage(correlationId, "No App Definition with name " + appDefinitionID + " found."));
	    return false;
	}

	/* check if max instances reached already */
	if (TheiaCloudK8sUtil.checkIfMaxInstancesReached(client, namespace, sessionSpec,
		optionalAppDefinition.get().getSpec(), correlationId)) {
	    LOGGER.info(formatLogMessage(correlationId,
		    "Max instances for " + appDefinitionID + " reached. Cannot create " + sessionSpec));
	    AddedHandler.updateSessionError(client, session, namespace,
		    "Max instances reached. Could not create session", correlationId);
	    return false;
	}

	/* handle storage */
	Optional<String> pvcName;
	if (arguments.isEphemeralStorage()) {
	    pvcName = Optional.empty();
	} else {
	    /* create persistent volume if not present already */
	    pvcName = Optional.of(TheiaCloudPersistentVolumeUtil.getPersistentVolumeName(session));
	    Optional<PersistentVolumeClaim> volume = K8sUtil.getPersistentVolumeClaim(client, namespace, pvcName.get());
	    if (volume.isEmpty()) {
		LOGGER.debug(formatLogMessage(correlationId,
			"No Volumce Claim with name " + pvcName.get() + " was found. Creating claims."));
		persistentVolumeHandler.createAndApplyPersistentVolume(client, namespace, correlationId,
			optionalAppDefinition.get().getSpec(), session);
		persistentVolumeHandler.createAndApplyPersistentVolumeClaim(client, namespace, correlationId,
			optionalAppDefinition.get().getSpec(), session);
	    }
	}

	/* find ingress */
	String appDefinitionResourceName = optionalAppDefinition.get().getMetadata().getName();
	String appDefinitionResourceUID = optionalAppDefinition.get().getMetadata().getUid();
	Optional<Ingress> ingress = K8sUtil.getExistingIngress(client, namespace, appDefinitionResourceName,
		appDefinitionResourceUID);
	if (ingress.isEmpty()) {
	    LOGGER.error(
		    formatLogMessage(correlationId, "No Ingress for app definition " + appDefinitionID + " found."));
	    return false;
	}

	/* Create service for this session */
	List<Service> existingServices = K8sUtil.getExistingServices(client, namespace, sessionResourceName,
		sessionResourceUID);
	if (!existingServices.isEmpty()) {
	    LOGGER.warn(formatLogMessage(correlationId,
		    "Existing service for " + sessionSpec + ". Session already running?"));
	    return true;
	}
	Optional<Service> serviceToUse = createAndApplyService(client, namespace, correlationId, sessionResourceName,
		sessionResourceUID, session, optionalAppDefinition.get().getSpec().getPort(),
		arguments.isUseKeycloak());
	if (serviceToUse.isEmpty()) {
	    LOGGER.error(formatLogMessage(correlationId, "Unable to create service for session " + sessionSpec));
	    return false;
	}

	if (arguments.isUseKeycloak()) {
	    /* Create config maps for this session */
	    List<ConfigMap> existingConfigMaps = K8sUtil.getExistingConfigMaps(client, namespace, sessionResourceName,
		    sessionResourceUID);
	    if (!existingConfigMaps.isEmpty()) {
		LOGGER.warn(formatLogMessage(correlationId,
			"Existing configmaps for " + sessionSpec + ". Session already running?"));
		return true;
	    }
	    createAndApplyEmailConfigMap(client, namespace, correlationId, sessionResourceName, sessionResourceUID,
		    session);
	    createAndApplyProxyConfigMap(client, namespace, correlationId, sessionResourceName, sessionResourceUID,
		    session, optionalAppDefinition.get());
	}

	/* Create deployment for this session */
	List<Deployment> existingDeployments = K8sUtil.getExistingDeployments(client, namespace, sessionResourceName,
		sessionResourceUID);
	if (!existingDeployments.isEmpty()) {
	    LOGGER.warn(formatLogMessage(correlationId,
		    "Existing deployments for " + sessionSpec + ". Session already running?"));
	    return true;
	}
	createAndApplyDeployment(client, namespace, correlationId, sessionResourceName, sessionResourceUID, session,
		optionalAppDefinition.get(), pvcName, arguments.isUseKeycloak());

	/* adjust the ingress */
	String host;
	try {
	    host = updateIngress(client, namespace, ingress, serviceToUse, session, optionalAppDefinition.get());
	} catch (KubernetesClientException e) {
	    LOGGER.error(formatLogMessage(correlationId,
		    "Error while editing ingress " + ingress.get().getMetadata().getName()), e);
	    return false;
	}

	/* Update session resource */
	try {
	    AddedHandler.updateSessionURLAsync(client, session, namespace, host, correlationId);
	} catch (KubernetesClientException e) {
	    LOGGER.error(
		    formatLogMessage(correlationId, "Error while editing session " + session.getMetadata().getName()),
		    e);
	    return false;
	}

	return true;
    }

    protected Optional<Service> createAndApplyService(DefaultKubernetesClient client, String namespace,
	    String correlationId, String sessionResourceName, String sessionResourceUID, Session session, int port,
	    boolean useOAuth2Proxy) {
	Map<String, String> replacements = TheiaCloudServiceUtil.getServiceReplacements(namespace, session, port);
	String templateYaml = useOAuth2Proxy ? AddedHandler.TEMPLATE_SERVICE_YAML
		: AddedHandler.TEMPLATE_SERVICE_WITHOUT_AOUTH2_PROXY_YAML;
	String serviceYaml;
	try {
	    serviceYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(templateYaml, replacements,
		    correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while adjusting template for session " + session), e);
	    return Optional.empty();
	}
	return K8sUtil.loadAndCreateServiceWithOwnerReference(client, namespace, correlationId, serviceYaml,
		SessionSpec.API, SessionSpec.KIND, sessionResourceName, sessionResourceUID, 0);
    }

    protected void createAndApplyEmailConfigMap(DefaultKubernetesClient client, String namespace, String correlationId,
	    String sessionResourceName, String sessionResourceUID, Session session) {
	Map<String, String> replacements = TheiaCloudConfigMapUtil.getEmailConfigMapReplacements(namespace, session);
	String configMapYaml;
	try {
	    configMapYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(
		    AddedHandler.TEMPLATE_CONFIGMAP_EMAILS_YAML, replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while adjusting template for session " + session), e);
	    return;
	}
	K8sUtil.loadAndCreateConfigMapWithOwnerReference(client, namespace, correlationId, configMapYaml,
		SessionSpec.API, SessionSpec.KIND, sessionResourceName, sessionResourceUID, 0, configmap -> {
		    configmap.setData(Collections.singletonMap(AddedHandler.FILENAME_AUTHENTICATED_EMAILS_LIST,
			    session.getSpec().getUser()));
		});
    }

    protected void createAndApplyProxyConfigMap(DefaultKubernetesClient client, String namespace, String correlationId,
	    String sessionResourceName, String sessionResourceUID, Session session,
	    AppDefinitionSpecResource appDefinition) {
	Map<String, String> replacements = TheiaCloudConfigMapUtil.getProxyConfigMapReplacements(namespace, session);
	String configMapYaml;
	try {
	    configMapYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(AddedHandler.TEMPLATE_CONFIGMAP_YAML,
		    replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while adjusting template for session " + session), e);
	    return;
	}
	K8sUtil.loadAndCreateConfigMapWithOwnerReference(client, namespace, correlationId, configMapYaml,
		SessionSpec.API, SessionSpec.KIND, sessionResourceName, sessionResourceUID, 0, configMap -> {
		    String host = session.getMetadata().getUid() + "." + appDefinition.getSpec().getHost()
			    + ingressPathProvider.getPath(appDefinition, session);
		    int port = appDefinition.getSpec().getPort();
		    AddedHandler.updateProxyConfigMap(client, namespace, configMap, host, port);
		});
    }

    protected void createAndApplyDeployment(DefaultKubernetesClient client, String namespace, String correlationId,
	    String sessionResourceName, String sessionResourceUID, Session session,
	    AppDefinitionSpecResource appDefinition, Optional<String> pvName, boolean useOAuth2Proxy) {
	Map<String, String> replacements = TheiaCloudDeploymentUtil.getDeploymentsReplacements(namespace, session,
		appDefinition);
	String templateYaml = useOAuth2Proxy ? AddedHandler.TEMPLATE_DEPLOYMENT_YAML
		: AddedHandler.TEMPLATE_DEPLOYMENT_WITHOUT_AOUTH2_PROXY_YAML;
	String deploymentYaml;
	try {
	    deploymentYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(templateYaml, replacements,
		    correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while adjusting template for session " + session), e);
	    return;
	}
	K8sUtil.loadAndCreateDeploymentWithOwnerReference(client, namespace, correlationId, deploymentYaml,
		SessionSpec.API, SessionSpec.KIND, sessionResourceName, sessionResourceUID, 0, deployment -> {
		    pvName.ifPresent(
			    name -> persistentVolumeHandler.addVolumeClaim(deployment, name, appDefinition.getSpec()));
		    bandwidthLimiter.limit(deployment, appDefinition.getSpec().getDownlinkLimit(),
			    appDefinition.getSpec().getUplinkLimit(), correlationId);
		    AddedHandler.removeEmptyResources(deployment);
		    if (appDefinition.getSpec().getPullSecret() != null
			    && !appDefinition.getSpec().getPullSecret().isEmpty()) {
			AddedHandler.addImagePullSecret(deployment, appDefinition.getSpec().getPullSecret());
		    }
		});
    }

    protected synchronized String updateIngress(DefaultKubernetesClient client, String namespace,
	    Optional<Ingress> ingress, Optional<Service> serviceToUse, Session session,
	    AppDefinitionSpecResource appDefinition) {
	String host = session.getMetadata().getUid() + "." + appDefinition.getSpec().getHost();
	String path = ingressPathProvider.getPath(appDefinition, session);
	client.network().v1().ingresses().inNamespace(namespace).withName(ingress.get().getMetadata().getName())
		.edit(ingressToUpdate -> {
		    IngressRule ingressRule = new IngressRule();
		    ingressToUpdate.getSpec().getRules().add(ingressRule);

		    ingressRule.setHost(host);

		    HTTPIngressRuleValue http = new HTTPIngressRuleValue();
		    ingressRule.setHttp(http);

		    HTTPIngressPath httpIngressPath = new HTTPIngressPath();
		    http.getPaths().add(httpIngressPath);
		    httpIngressPath.setPath(path);
		    httpIngressPath.setPathType("Prefix");

		    IngressBackend ingressBackend = new IngressBackend();
		    httpIngressPath.setBackend(ingressBackend);

		    IngressServiceBackend ingressServiceBackend = new IngressServiceBackend();
		    ingressBackend.setService(ingressServiceBackend);
		    ingressServiceBackend.setName(serviceToUse.get().getMetadata().getName());

		    ServiceBackendPort serviceBackendPort = new ServiceBackendPort();
		    ingressServiceBackend.setPort(serviceBackendPort);
		    serviceBackendPort.setNumber(appDefinition.getSpec().getPort());

		    return ingressToUpdate;
		});
	return host + path;
    }

}
