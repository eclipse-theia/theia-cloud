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
import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatMetric;
import static org.eclipse.theia.cloud.operator.handler.util.TheiaCloudDeploymentUtil.HOST_PROTOCOL;

import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.AppDefinitionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.Session;
import org.eclipse.theia.cloud.common.k8s.resource.SessionSpec.InitOperation;
import org.eclipse.theia.cloud.common.k8s.resource.SessionSpecResourceList;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.common.util.LogMessageUtil;
import org.eclipse.theia.cloud.common.util.WorkspaceUtil;
import org.eclipse.theia.cloud.operator.handler.InitOperationHandler;
import org.eclipse.theia.cloud.operator.handler.util.TheiaCloudPersistentVolumeUtil;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapEnvSource;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.SecretEnvSource;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;

public final class AddedHandlerUtil {

    private static final Logger LOGGER = LogManager.getLogger(AddedHandlerUtil.class);

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    public static final String USER_DATA = "user-data";

    public static final String TEMPLATE_SERVICE_YAML = "/templateService.yaml";
    public static final String TEMPLATE_SERVICE_WITHOUT_AOUTH2_PROXY_YAML = "/templateServiceWithoutOAuthProxy.yaml";
    public static final String TEMPLATE_CONFIGMAP_EMAILS_YAML = "/templateConfigmapEmails.yaml";
    public static final String TEMPLATE_CONFIGMAP_YAML = "/templateConfigmap.yaml";
    public static final String TEMPLATE_DEPLOYMENT_YAML = "/templateDeployment.yaml";
    public static final String TEMPLATE_DEPLOYMENT_WITHOUT_AOUTH2_PROXY_YAML = "/templateDeploymentWithoutOAuthProxy.yaml";

    public static final String OAUTH2_PROXY_CFG = "oauth2-proxy.cfg";

    public static final String OAUTH2_PROXY_CONFIGMAP_NAME = "oauth2-proxy-config";

    public static final String CONFIGMAP_DATA_PLACEHOLDER_HOST = "https://placeholder";
    public static final String CONFIGMAP_DATA_PLACEHOLDER_PORT = "placeholder-port";

    public static final String FILENAME_AUTHENTICATED_EMAILS_LIST = "authenticated-emails-list";

    public static final String INGRESS_REWRITE_PATH = "(/|$)(.*)";

    private static final HostnameVerifier ALL_GOOD_HOSTNAME_VERIFIER = new HostnameVerifier() {
	@Override
	public boolean verify(String hostname, SSLSession session) {
	    return true;
	}
    };

    private static final X509TrustManager TRUST_ALL_MANAGER = new X509TrustManager() {

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
	    /* no op */
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
	    /* no op */
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
	    return new X509Certificate[0];
	}
    };

    private AddedHandlerUtil() {

    }

    public static void updateProxyConfigMap(NamespacedKubernetesClient client, String namespace, ConfigMap configMap,
	    String host, int port) {
	ConfigMap templateConfigMap = client.configMaps().inNamespace(namespace).withName(OAUTH2_PROXY_CONFIGMAP_NAME)
		.get();
	Map<String, String> data = new LinkedHashMap<>(templateConfigMap.getData());
	data.put(OAUTH2_PROXY_CFG, data.get(OAUTH2_PROXY_CFG)//
		.replace(CONFIGMAP_DATA_PLACEHOLDER_HOST, HOST_PROTOCOL + host)//
		.replace(CONFIGMAP_DATA_PLACEHOLDER_PORT, String.valueOf(port)));
	configMap.setData(data);
    }

    public static void updateSessionURLAsync(NamespacedKubernetesClient client, Session session, String namespace,
	    String url, String correlationId) {
	EXECUTOR.execute(() -> {
	    boolean updateURL = false;
	    for (int i = 1; i <= 100; i++) {
		try {
		    /*
		     * On the first 15 loops we will check every 2.5s whether URL is available. This
		     * will take at least 37.5s.
		     * 
		     * On the second 15 loops we will check every 5s. This will take at least 75s.
		     * 
		     * On the next 15 loops we will check every 10s. This will take at least further
		     * 150s.
		     * 
		     * If the pod has not started within the first 4-5 minutes, we will continue to
		     * check every minute. We give up after an hour.
		     * 
		     */
		    if (i <= 15) {
			Thread.sleep(2500);
		    } else if (i <= 30) {
			Thread.sleep(5000);
		    } else if (i <= 45) {
			Thread.sleep(10000);
		    } else {
			Thread.sleep(60000);
		    }
		} catch (InterruptedException e) {
		    /* silent */
		}

		HttpsURLConnection connection;
		try {
		    connection = (HttpsURLConnection) new URL(HOST_PROTOCOL + url).openConnection();
		} catch (IOException e) {
		    LOGGER.error(formatLogMessage(correlationId, "Error while checking session availability."), e);
		    continue;
		}
		int code;

		try {
		    connection.setHostnameVerifier(ALL_GOOD_HOSTNAME_VERIFIER);
		    SSLContext sc = SSLContext.getInstance("SSL");
		    sc.init(null, new TrustManager[] { TRUST_ALL_MANAGER }, new java.security.SecureRandom());
		    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		    connection.setSSLSocketFactory(sc.getSocketFactory());
		    connection.connect();
		    code = connection.getResponseCode();
		} catch (IOException e) {
		    LOGGER.error(formatLogMessage(correlationId, url + " is NOT available yet."), e);
		    continue;
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
		    LOGGER.error(formatLogMessage(correlationId,
			    "Error while checking session availability with SSL ignore."), e);
		    continue;
		}

		LOGGER.trace(formatLogMessage(correlationId, url + " has response code " + code));

		if (code == 200) {
		    updateURL = true;
		} else if (code != 404 && code != 503 && !updateURL) {
		    /*
		     * we don't get a 404 or 503, so something is available. Try accessing the URL
		     * once more then update URL anyway
		     */
		    updateURL = true;
		    continue;
		}

		if (updateURL) {
		    LOGGER.info(formatLogMessage(correlationId, url + " is available."));
		    client.resources(Session.class, SessionSpecResourceList.class).inNamespace(namespace)
			    .withName(session.getMetadata().getName())//
			    .edit(ws -> {
				ws.getSpec().setUrl(url);
				return ws;
			    });
		    LOGGER.info(
			    formatMetric(correlationId, "Running session for " + session.getSpec().getAppDefinition()));
		    break;
		} else {
		    LOGGER.trace(formatLogMessage(correlationId, url + " is NOT available yet."));
		}

	    }

	});

    }

    public static void removeEmptyResources(Deployment deployment) {
	for (Container container : deployment.getSpec().getTemplate().getSpec().getContainers()) {
	    ResourceRequirements resources = container.getResources();
	    if (resources == null) {
		continue;
	    }
	    Map<String, Quantity> limits = resources.getLimits();
	    if (limits != null) {
		Set<String> toRemove = new LinkedHashSet<>();
		for (String key : limits.keySet()) {
		    Quantity quantity = limits.get(key);
		    if (quantity == null) {
			toRemove.add(key);
		    }
		}
		toRemove.forEach(limits::remove);
	    }
	    Map<String, Quantity> requests = resources.getRequests();
	    if (requests != null) {
		Set<String> toRemove = new LinkedHashSet<>();
		for (String key : requests.keySet()) {
		    Quantity quantity = requests.get(key);
		    if (quantity == null) {
			toRemove.add(key);
		    }
		}
		toRemove.forEach(requests::remove);
	    }
	}
    }

    public static void addImagePullSecret(Deployment deployment, String secret) {
	List<LocalObjectReference> imagePullSecrets = deployment.getSpec().getTemplate().getSpec()
		.getImagePullSecrets();
	if (imagePullSecrets == null) {
	    imagePullSecrets = new ArrayList<LocalObjectReference>();
	    deployment.getSpec().getTemplate().getSpec().setImagePullSecrets(imagePullSecrets);
	}
	imagePullSecrets.add(new LocalObjectReference(secret));
    }

    /* ------------------- Addition of env vars to Deployments ------------------ */
    public static void addCustomEnvVarsToDeploymentFromSession(String correlationId, Deployment deployment,
	    Session session, AppDefinition appDefinition) {
	String containerName = appDefinition.getSpec().getName();
	Optional<Integer> maybeContainerIdx = findContainerIdxInDeployment(deployment, containerName);

	if (maybeContainerIdx.isEmpty()) {
	    LOGGER.error(LogMessageUtil.formatLogMessage(correlationId,
		    "Trying to add custom env vars to Deployment from Session. Could not find the container "
			    + containerName + " in Deployment named" + deployment.getMetadata().getName()));
	    return;
	}
	int containerIdx = maybeContainerIdx.get();
	Container container = deployment.getSpec().getTemplate().getSpec().getContainers().get(containerIdx);

	container = withDirectEnvVarsToContainer(container, session.getSpec().getEnvVars());
	container = withEnvVarsFromRefsToContainer(container, session.getSpec().getEnvVarsFromConfigMaps(), false);
	container = withEnvVarsFromRefsToContainer(container, session.getSpec().getEnvVarsFromSecrets(), true);

	deployment.getSpec().getTemplate().getSpec().getContainers().set(containerIdx, container);
    }

    private static Container withEnvVarsFromRefsToContainer(Container container, List<String> refs,
	    boolean isFromSecret) {
	if (refs == null || refs.size() == 0)
	    return container;

	List<EnvFromSource> newEnvFromSources = refs.stream()
		.map((ref) -> isFromSecret ? new EnvFromSource(null, null, new SecretEnvSource(ref, null))
			: new EnvFromSource(new ConfigMapEnvSource(ref, null), null, null))
		.collect(Collectors.toList());

	List<EnvFromSource> combinedEnvVarFromSources = new LinkedList<>();
	combinedEnvVarFromSources.addAll(container.getEnvFrom());
	combinedEnvVarFromSources.addAll(newEnvFromSources);

	container.setEnvFrom(combinedEnvVarFromSources);

	return container;
    }

    private static Container withDirectEnvVarsToContainer(Container container, Map<String, String> mapEnvVars) {
	if (mapEnvVars == null || mapEnvVars.size() == 0)
	    return container;

	List<EnvVar> newEnvVars = mapEnvVars.entrySet().stream()
		.map((kv) -> new EnvVar(kv.getKey(), kv.getValue(), null)).collect(Collectors.toList());

	List<EnvVar> combinedEnvVars = new LinkedList<>();
	combinedEnvVars.addAll(container.getEnv());
	combinedEnvVars.addAll(newEnvVars);

	container.setEnv(combinedEnvVars);

	return container;
    }

    private static Optional<Integer> findContainerIdxInDeployment(Deployment deployment, String containerName) {
	List<Container> containers = deployment.getSpec().getTemplate().getSpec().getContainers();
	for (int i = 0; i < containers.size(); i++) {
	    if (containers.get(i).getName().equals(containerName)) {
		return Optional.of(i);
	    }
	}

	return Optional.empty();
    }

    public static void addInitContainers(String correlationId, TheiaCloudClient client, Deployment deployment,
	    AppDefinition appDefinition, Session session, Set<InitOperationHandler> initOperationHandlers) {
	List<InitOperation> initOperations = session.getSpec().getinitOperations();
	if (initOperations == null) {
	    return;
	}
	for (InitOperation initOperation : initOperations) {
	    Optional<InitOperationHandler> handler = initOperationHandlers.stream()
		    .filter(h -> h.operationId().equalsIgnoreCase(initOperation.getId())).findAny();
	    if (handler.isEmpty()) {
		LOGGER.warn(LogMessageUtil.formatLogMessage(correlationId, MessageFormat
			.format("No Init Handler found for operation with id {0}.", initOperation.getId())));
		continue;
	    }
	    handler.get().addInitContainer(correlationId, client, deployment, appDefinition, session,
		    initOperation.getArguments());
	    LOGGER.info(formatLogMessage(correlationId,
		    MessageFormat.format("Added init container with id {0} to deployment.", initOperation.getId())));
	}
    }

    public static Volume createUserDataVolume(String pvcName) {
	Volume volume = new Volume();
	volume.setName(USER_DATA);
	PersistentVolumeClaimVolumeSource persistentVolumeClaim = new PersistentVolumeClaimVolumeSource();
	volume.setPersistentVolumeClaim(persistentVolumeClaim);
	persistentVolumeClaim.setClaimName(pvcName);
	return volume;
    }

    public static VolumeMount createUserDataVolumeMount(AppDefinitionSpec appDefinition) {
	VolumeMount volumeMount = new VolumeMount();
	volumeMount.setName(AddedHandlerUtil.USER_DATA);
	volumeMount.setMountPath(TheiaCloudPersistentVolumeUtil.getMountPath(appDefinition));
	return volumeMount;
    }

    public static Optional<String> getStorageName(TheiaCloudClient client, Session session, String correlationId) {
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

}
