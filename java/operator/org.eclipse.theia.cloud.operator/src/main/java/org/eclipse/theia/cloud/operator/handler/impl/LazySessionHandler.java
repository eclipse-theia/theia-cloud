/********************************************************************************
 * Copyright (C) 2022-2023 EclipseSource, Lockular, Ericsson, STMicroelectronics and 
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
import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatMetric;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.AppDefinitionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.OperatorStatus;
import org.eclipse.theia.cloud.common.k8s.resource.ResourceStatus;
import org.eclipse.theia.cloud.common.k8s.resource.Session;
import org.eclipse.theia.cloud.common.k8s.resource.SessionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.SessionStatus;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.common.util.TheiaCloudError;
import org.eclipse.theia.cloud.common.util.WorkspaceUtil;
import org.eclipse.theia.cloud.operator.TheiaCloudArguments;
import org.eclipse.theia.cloud.operator.handler.BandwidthLimiter;
import org.eclipse.theia.cloud.operator.handler.DeploymentTemplateReplacements;
import org.eclipse.theia.cloud.operator.handler.IngressPathProvider;
import org.eclipse.theia.cloud.operator.handler.SessionHandler;
import org.eclipse.theia.cloud.operator.handler.util.K8sUtil;
import org.eclipse.theia.cloud.operator.handler.util.TheiaCloudConfigMapUtil;
import org.eclipse.theia.cloud.operator.handler.util.TheiaCloudIngressUtil;
import org.eclipse.theia.cloud.operator.handler.util.TheiaCloudK8sUtil;
import org.eclipse.theia.cloud.operator.handler.util.TheiaCloudPersistentVolumeUtil;
import org.eclipse.theia.cloud.operator.handler.util.TheiaCloudServiceUtil;
import org.eclipse.theia.cloud.operator.util.JavaResourceUtil;

import com.google.inject.Inject;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressRuleValue;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBackend;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRule;
import io.fabric8.kubernetes.api.model.networking.v1.IngressServiceBackend;
import io.fabric8.kubernetes.api.model.networking.v1.ServiceBackendPort;
import io.fabric8.kubernetes.client.KubernetesClientException;

public class LazySessionHandler implements SessionHandler {

    private static final Logger LOGGER = LogManager.getLogger(LazySessionHandler.class);
    protected static final String USER_DATA = "user-data";

    @Inject
    protected IngressPathProvider ingressPathProvider;
    @Inject
    protected TheiaCloudArguments arguments;
    @Inject
    protected BandwidthLimiter bandwidthLimiter;
    @Inject
    protected DeploymentTemplateReplacements deploymentReplacements;

    @Inject
    protected TheiaCloudClient client;

    @Override
    public boolean sessionAdded(Session session, String correlationId) {
	try {
	    return doSessionAdded(session, correlationId);
	} catch (Throwable ex) {
	    LOGGER.error(formatLogMessage(correlationId,
		    "An unexpected exception occurred while adding Session: " + session), ex);
	    client.sessions().updateStatus(correlationId, session, status -> {
		status.setOperatorStatus(OperatorStatus.ERROR);
		status.setOperatorMessage(
			"Unexpected error. Please check the logs for correlationId: " + correlationId);
	    });
	    return false;
	}
    }

    protected boolean doSessionAdded(Session session, String correlationId) {
	/* session information */
	String sessionResourceName = session.getMetadata().getName();
	String sessionResourceUID = session.getMetadata().getUid();

	// Check current session status and ignore if handling failed or finished before
	Optional<SessionStatus> status = Optional.ofNullable(session.getStatus());
	String operatorStatus = status.map(ResourceStatus::getOperatorStatus).orElse(OperatorStatus.NEW);
	if (OperatorStatus.HANDLED.equals(operatorStatus)) {
	    LOGGER.trace(formatLogMessage(correlationId,
		    "Session was successfully handled before and is skipped now. Session: " + session));
	    return true;
	}
	if (OperatorStatus.ERROR.equals(operatorStatus) || OperatorStatus.HANDLING.equals(operatorStatus)) {
	    // TODO In the HANDLING case we should not return but continue where we left
	    // off.
	    LOGGER.warn(formatLogMessage(correlationId,
		    "Session could not be handled before and is skipped now. Current status: " + operatorStatus
			    + ". Session: " + session));
	    return false;
	}

	// Set session status to being handled
	client.sessions().updateStatus(correlationId, session, s -> {
	    s.setOperatorStatus(OperatorStatus.HANDLING);
	});

	SessionSpec sessionSpec = session.getSpec();

	/* find app definition for session */
	String appDefinitionID = sessionSpec.getAppDefinition();
	Optional<AppDefinition> optionalAppDefinition = client.appDefinitions().get(appDefinitionID);
	if (optionalAppDefinition.isEmpty()) {
	    LOGGER.error(formatLogMessage(correlationId, "No App Definition with name " + appDefinitionID + " found."));
	    client.sessions().updateStatus(correlationId, session, s -> {
		s.setOperatorStatus(OperatorStatus.ERROR);
		s.setOperatorMessage("App Definition not found.");
	    });
	    return false;
	}
	AppDefinition appDefinition = optionalAppDefinition.get();

	if (hasMaxInstancesReached(appDefinition, session, correlationId)) {
	    client.sessions().updateStatus(correlationId, session, s -> {
		s.setOperatorStatus(OperatorStatus.ERROR);
		s.setOperatorMessage("Max instances reached.");
	    });
	    return false;
	}

	if (hasMaxSessionsReached(session, correlationId)) {
	    client.sessions().updateStatus(correlationId, session, s -> {
		s.setOperatorStatus(OperatorStatus.ERROR);
		s.setOperatorMessage("Max sessions reached.");
	    });
	    return false;
	}

	Optional<Ingress> ingress = getIngress(appDefinition, correlationId);
	if (ingress.isEmpty()) {
	    client.sessions().updateStatus(correlationId, session, s -> {
		s.setOperatorStatus(OperatorStatus.ERROR);
		s.setOperatorMessage("Ingress not available.");
	    });
	    return false;
	}

	syncSessionDataToWorkspace(session, correlationId);

	/* Create service for this session */
	List<Service> existingServices = K8sUtil.getExistingServices(client.kubernetes(), client.namespace(),
		sessionResourceName, sessionResourceUID);
	if (!existingServices.isEmpty()) {
	    LOGGER.warn(formatLogMessage(correlationId,
		    "Existing service for " + sessionSpec + ". Session already running?"));
	    client.sessions().updateStatus(correlationId, session, s -> {
		s.setOperatorStatus(OperatorStatus.HANDLED);
		s.setOperatorMessage("Service already exists.");
	    });
	    // TODO do not return true if the sessions was in handling state at the start of
	    // this handler
	    return true;
	}

	Optional<Service> serviceToUse = createAndApplyService(correlationId, sessionResourceName, sessionResourceUID,
		session, appDefinition.getSpec(), arguments.isUseKeycloak());
	if (serviceToUse.isEmpty()) {
	    LOGGER.error(formatLogMessage(correlationId, "Unable to create service for session " + sessionSpec));
	    client.sessions().updateStatus(correlationId, session, s -> {
		s.setOperatorStatus(OperatorStatus.ERROR);
		s.setOperatorMessage("Failed to create service.");
	    });
	    return false;
	}

	if (arguments.isUseKeycloak()) {
	    /* Create config maps for this session */
	    List<ConfigMap> existingConfigMaps = K8sUtil.getExistingConfigMaps(client.kubernetes(), client.namespace(),
		    sessionResourceName, sessionResourceUID);
	    if (!existingConfigMaps.isEmpty()) {
		LOGGER.warn(formatLogMessage(correlationId,
			"Existing configmaps for " + sessionSpec + ". Session already running?"));
		client.sessions().updateStatus(correlationId, session, s -> {
		    s.setOperatorStatus(OperatorStatus.HANDLED);
		    s.setOperatorMessage("Configmaps already exist.");
		});
		// TODO do not return true if the sessions was in handling state at the start of
		// this handler
		return true;
	    }
	    createAndApplyEmailConfigMap(correlationId, sessionResourceName, sessionResourceUID, session);
	    createAndApplyProxyConfigMap(correlationId, sessionResourceName, sessionResourceUID, session,
		    appDefinition);
	}

	/* Create deployment for this session */
	List<Deployment> existingDeployments = K8sUtil.getExistingDeployments(client.kubernetes(), client.namespace(),
		sessionResourceName, sessionResourceUID);
	if (!existingDeployments.isEmpty()) {
	    LOGGER.warn(formatLogMessage(correlationId,
		    "Existing deployments for " + sessionSpec + ". Session already running?"));
	    client.sessions().updateStatus(correlationId, session, s -> {
		s.setOperatorStatus(OperatorStatus.HANDLED);
		s.setOperatorMessage("Deployment already exists.");
	    });
	    return true;
	}

	Optional<String> storageName = getStorageName(session, correlationId);
	createAndApplyDeployment(correlationId, sessionResourceName, sessionResourceUID, session, appDefinition,
		storageName, arguments.isUseKeycloak());

	/* adjust the ingress */
	String host;
	try {
	    host = updateIngress(ingress, serviceToUse, session, appDefinition, correlationId);
	} catch (KubernetesClientException e) {
	    LOGGER.error(formatLogMessage(correlationId,
		    "Error while editing ingress " + ingress.get().getMetadata().getName()), e);
	    client.sessions().updateStatus(correlationId, session, s -> {
		s.setOperatorStatus(OperatorStatus.ERROR);
		s.setOperatorMessage("Failed to edit ingress");
	    });
	    return false;
	}

	/* Update session resource */
	try {
	    AddedHandlerUtil.updateSessionURLAsync(client.kubernetes(), session, client.namespace(), host,
		    correlationId);
	} catch (KubernetesClientException e) {
	    LOGGER.error(
		    formatLogMessage(correlationId, "Error while editing session " + session.getMetadata().getName()),
		    e);
	    client.sessions().updateStatus(correlationId, session, s -> {
		s.setOperatorStatus(OperatorStatus.ERROR);
		s.setOperatorMessage("Failed to set session URL.");
	    });
	    return false;
	}

	client.sessions().updateStatus(correlationId, session, s -> {
	    s.setOperatorStatus(OperatorStatus.HANDLED);
	});
	return true;
    }

    protected void syncSessionDataToWorkspace(Session session, String correlationId) {
	if (!session.getSpec().isEphemeral() && session.getSpec().hasAppDefinition()) {
	    // update last used workspace
	    client.workspaces().edit(correlationId, session.getSpec().getWorkspace(), workspace -> {
		workspace.getSpec().setAppDefinition(session.getSpec().getAppDefinition());
	    });
	}
    }

    protected boolean hasMaxInstancesReached(AppDefinition appDefinition, Session session, String correlationId) {
	if (TheiaCloudK8sUtil.checkIfMaxInstancesReached(client.kubernetes(), client.namespace(), session.getSpec(),
		appDefinition.getSpec(), correlationId)) {
	    LOGGER.info(formatMetric(correlationId, "Max instances reached for " + appDefinition.getSpec().getName()));
	    client.sessions().edit(correlationId, session.getMetadata().getName(),
		    toEdit -> toEdit.getSpec().setError(TheiaCloudError.SESSION_SERVER_LIMIT_REACHED));
	    return true;
	}
	return false;
    }

    protected boolean hasMaxSessionsReached(Session session, String correlationId) {
	/* check if max sessions reached already */
	if (arguments.getSessionsPerUser() != null && arguments.getSessionsPerUser() >= 0) {
	    if (arguments.getSessionsPerUser() == 0) {
		LOGGER.info(formatLogMessage(correlationId,
			"No sessions allowed for this user. Could not create session " + session.getSpec()));
		client.sessions().edit(correlationId, session.getMetadata().getName(),
			ws -> ws.getSpec().setError(TheiaCloudError.SESSION_USER_NO_SESSIONS));
		return true;
	    }

	    long userSessions = client.sessions().list(session.getSpec().getUser()).size();
	    if (userSessions > arguments.getSessionsPerUser()) {
		LOGGER.info(formatLogMessage(correlationId,
			"No more sessions allowed for this user, limit is  " + arguments.getSessionsPerUser()));
		client.sessions().edit(correlationId, session.getMetadata().getName(),
			toEdit -> toEdit.getSpec().setError(TheiaCloudError.SESSION_USER_LIMIT_REACHED));
		return true;
	    }
	}
	return false;
    }

    protected Optional<Ingress> getIngress(AppDefinition appDefinition, String correlationId) {
	String appDefinitionResourceName = appDefinition.getMetadata().getName();
	String appDefinitionResourceUID = appDefinition.getMetadata().getUid();
	Optional<Ingress> ingress = K8sUtil.getExistingIngress(client.kubernetes(), client.namespace(),
		appDefinitionResourceName, appDefinitionResourceUID);
	if (ingress.isEmpty()) {
	    LOGGER.error(formatLogMessage(correlationId,
		    "No Ingress for app definition " + appDefinition.getSpec().getName() + " found."));
	}
	return ingress;
    }

    protected Optional<String> getStorageName(Session session, String correlationId) {
	if (session.getSpec().isEphemeral()) {
	    return Optional.empty();
	}
	Optional<Workspace> workspace = client.workspaces().get(session.getSpec().getWorkspace());
	if (!workspace.isPresent()) {
	    LOGGER.info(formatLogMessage(correlationId, "No workspace with name " + session.getSpec().getWorkspace()
		    + " found for session " + session.getSpec().getName(), correlationId));
	    return Optional.empty();

	}
	String storageName = WorkspaceUtil.getStorageName(workspace.get());
	if (!client.persistentVolumeClaims().has(storageName)) {
	    LOGGER.info(formatLogMessage(correlationId,
		    "No storage found for started session, will use ephemeral storage instead", correlationId));
	    return Optional.empty();
	}
	return Optional.of(storageName);
    }

    protected Optional<Service> createAndApplyService(String correlationId, String sessionResourceName,
	    String sessionResourceUID, Session session, AppDefinitionSpec appDefinitionSpec, boolean useOAuth2Proxy) {
	Map<String, String> replacements = TheiaCloudServiceUtil.getServiceReplacements(client.namespace(), session,
		appDefinitionSpec);
	String templateYaml = useOAuth2Proxy ? AddedHandlerUtil.TEMPLATE_SERVICE_YAML
		: AddedHandlerUtil.TEMPLATE_SERVICE_WITHOUT_AOUTH2_PROXY_YAML;
	String serviceYaml;
	try {
	    serviceYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(templateYaml, replacements,
		    correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while adjusting template for session " + session), e);
	    return Optional.empty();
	}
	return K8sUtil.loadAndCreateServiceWithOwnerReference(client.kubernetes(), client.namespace(), correlationId,
		serviceYaml, Session.API, Session.KIND, sessionResourceName, sessionResourceUID, 0);
    }

    protected void createAndApplyEmailConfigMap(String correlationId, String sessionResourceName,
	    String sessionResourceUID, Session session) {
	Map<String, String> replacements = TheiaCloudConfigMapUtil.getEmailConfigMapReplacements(client.namespace(),
		session);
	String configMapYaml;
	try {
	    configMapYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(
		    AddedHandlerUtil.TEMPLATE_CONFIGMAP_EMAILS_YAML, replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while adjusting template for session " + session), e);
	    return;
	}
	K8sUtil.loadAndCreateConfigMapWithOwnerReference(client.kubernetes(), client.namespace(), correlationId,
		configMapYaml, Session.API, Session.KIND, sessionResourceName, sessionResourceUID, 0, configmap -> {
		    configmap.setData(Collections.singletonMap(AddedHandlerUtil.FILENAME_AUTHENTICATED_EMAILS_LIST,
			    session.getSpec().getUser()));
		});
    }

    protected void createAndApplyProxyConfigMap(String correlationId, String sessionResourceName,
	    String sessionResourceUID, Session session, AppDefinition appDefinition) {
	Map<String, String> replacements = TheiaCloudConfigMapUtil.getProxyConfigMapReplacements(client.namespace(),
		session);
	String configMapYaml;
	try {
	    configMapYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(
		    AddedHandlerUtil.TEMPLATE_CONFIGMAP_YAML, replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while adjusting template for session " + session), e);
	    return;
	}
	K8sUtil.loadAndCreateConfigMapWithOwnerReference(client.kubernetes(), client.namespace(), correlationId,
		configMapYaml, Session.API, Session.KIND, sessionResourceName, sessionResourceUID, 0, configMap -> {
		    String host = arguments.getInstancesHost() + ingressPathProvider.getPath(appDefinition, session);
		    int port = appDefinition.getSpec().getPort();
		    AddedHandlerUtil.updateProxyConfigMap(client.kubernetes(), client.namespace(), configMap, host,
			    port);
		});
    }

    protected void createAndApplyDeployment(String correlationId, String sessionResourceName, String sessionResourceUID,
	    Session session, AppDefinition appDefinition, Optional<String> pvName, boolean useOAuth2Proxy) {
	Map<String, String> replacements = deploymentReplacements.getReplacements(client.namespace(), appDefinition,
		session);
	String templateYaml = useOAuth2Proxy ? AddedHandlerUtil.TEMPLATE_DEPLOYMENT_YAML
		: AddedHandlerUtil.TEMPLATE_DEPLOYMENT_WITHOUT_AOUTH2_PROXY_YAML;
	String deploymentYaml;
	try {
	    deploymentYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(templateYaml, replacements,
		    correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while adjusting template for session " + session), e);
	    return;
	}
	K8sUtil.loadAndCreateDeploymentWithOwnerReference(client.kubernetes(), client.namespace(), correlationId,
		deploymentYaml, Session.API, Session.KIND, sessionResourceName, sessionResourceUID, 0, deployment -> {
		    pvName.ifPresent(name -> addVolumeClaim(deployment, name, appDefinition.getSpec()));
		    bandwidthLimiter.limit(deployment, appDefinition.getSpec().getDownlinkLimit(),
			    appDefinition.getSpec().getUplinkLimit(), correlationId);
		    AddedHandlerUtil.removeEmptyResources(deployment);

		    AddedHandlerUtil.addCustomEnvVarsToDeploymentFromSession(correlationId, deployment, session,
			    appDefinition);

		    if (appDefinition.getSpec().getPullSecret() != null
			    && !appDefinition.getSpec().getPullSecret().isEmpty()) {
			AddedHandlerUtil.addImagePullSecret(deployment, appDefinition.getSpec().getPullSecret());
		    }
		});
    }

    protected void addVolumeClaim(Deployment deployment, String pvcName, AppDefinitionSpec appDefinition) {
	PodSpec podSpec = deployment.getSpec().getTemplate().getSpec();

	Volume volume = new Volume();
	podSpec.getVolumes().add(volume);
	volume.setName(USER_DATA);
	PersistentVolumeClaimVolumeSource persistentVolumeClaim = new PersistentVolumeClaimVolumeSource();
	volume.setPersistentVolumeClaim(persistentVolumeClaim);
	persistentVolumeClaim.setClaimName(pvcName);

	Container theiaContainer = TheiaCloudPersistentVolumeUtil.getTheiaContainer(podSpec, appDefinition);

	VolumeMount volumeMount = new VolumeMount();
	theiaContainer.getVolumeMounts().add(volumeMount);
	volumeMount.setName(USER_DATA);
	volumeMount.setMountPath(TheiaCloudPersistentVolumeUtil.getMountPath(appDefinition));
    }

    protected synchronized String updateIngress(Optional<Ingress> ingress, Optional<Service> serviceToUse,
	    Session session, AppDefinition appDefinition, String correlationId) {
	final String host = arguments.getInstancesHost();
	String path = ingressPathProvider.getPath(appDefinition, session);
	client.ingresses().edit(correlationId, ingress.get().getMetadata().getName(), ingressToUpdate -> {
	    IngressRule ingressRule = new IngressRule();
	    ingressToUpdate.getSpec().getRules().add(ingressRule);

	    ingressRule.setHost(host);

	    HTTPIngressRuleValue http = new HTTPIngressRuleValue();
	    ingressRule.setHttp(http);

	    HTTPIngressPath httpIngressPath = new HTTPIngressPath();
	    http.getPaths().add(httpIngressPath);
	    httpIngressPath.setPath(path + AddedHandlerUtil.INGRESS_REWRITE_PATH);
	    httpIngressPath.setPathType("Prefix");

	    IngressBackend ingressBackend = new IngressBackend();
	    httpIngressPath.setBackend(ingressBackend);

	    IngressServiceBackend ingressServiceBackend = new IngressServiceBackend();
	    ingressBackend.setService(ingressServiceBackend);
	    ingressServiceBackend.setName(serviceToUse.get().getMetadata().getName());

	    ServiceBackendPort serviceBackendPort = new ServiceBackendPort();
	    ingressServiceBackend.setPort(serviceBackendPort);
	    serviceBackendPort.setNumber(appDefinition.getSpec().getPort());
	});
	return host + path + "/";
    }

    @Override
    public boolean sessionDeleted(Session session, String correlationId) {
	/* session information */
	SessionSpec sessionSpec = session.getSpec();

	/* find appDefinition for session */
	String appDefinitionID = sessionSpec.getAppDefinition();

	Optional<AppDefinition> optionalAppDefinition = client.appDefinitions().get(appDefinitionID);
	if (optionalAppDefinition.isEmpty()) {
	    LOGGER.error(formatLogMessage(correlationId, "No App Definition with name " + appDefinitionID + " found."));
	    return false;
	}

	/* find ingress */
	String appDefinitionResourceName = optionalAppDefinition.get().getMetadata().getName();
	String appDefinitionResourceUID = optionalAppDefinition.get().getMetadata().getUid();
	Optional<Ingress> ingress = K8sUtil.getExistingIngress(client.kubernetes(), client.namespace(),
		appDefinitionResourceName, appDefinitionResourceUID);
	if (ingress.isEmpty()) {
	    LOGGER.error(
		    formatLogMessage(correlationId, "No Ingress for app definition " + appDefinitionID + " found."));
	    return false;
	}

	String path = ingressPathProvider.getPath(optionalAppDefinition.get(), session);
	TheiaCloudIngressUtil.removeIngressRule(client.kubernetes(), client.namespace(), ingress.get(), path,
		correlationId);

	return true;
    }

}
