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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.resource.Session;
import org.eclipse.theia.cloud.common.k8s.resource.SessionSpecResourceList;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;

public final class AddedHandlerUtil {

    private static final Logger LOGGER = LogManager.getLogger(AddedHandlerUtil.class);

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

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
	    for (int i = 1; i <= 60; i++) {
		try {
		    Thread.sleep(i * 1000);
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

}
