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
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.common.k8s.resource.WorkspaceSpecResourceList;
import org.eclipse.theia.cloud.operator.handler.K8sUtil;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudIngressUtil;
import org.eclipse.theia.cloud.operator.resource.TemplateSpec;
import org.eclipse.theia.cloud.operator.resource.TemplateSpecResource;
import org.eclipse.theia.cloud.operator.util.JavaResourceUtil;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;

public final class AddedHandler {

    private static final Logger LOGGER = LogManager.getLogger(AddedHandler.class);

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    public static final String TEMPLATE_INGRESS_YAML = "/templateIngress.yaml";
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

    private AddedHandler() {
    }

    public static void createAndApplyIngress(DefaultKubernetesClient client, String namespace, String correlationId,
	    String templateResourceName, String templateResourceUID, TemplateSpecResource template) {
	Map<String, String> replacements = TheiaCloudIngressUtil.getIngressReplacements(namespace, template);
	String ingressYaml;
	try {
	    ingressYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(AddedHandler.class, TEMPLATE_INGRESS_YAML,
		    replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    return;
	}
	K8sUtil.loadAndCreateIngressWithOwnerReference(client, namespace, correlationId, ingressYaml, TemplateSpec.API,
		TemplateSpec.KIND, templateResourceName, templateResourceUID, 0);
    }

    public static void updateProxyConfigMap(DefaultKubernetesClient client, String namespace, ConfigMap configMap,
	    String host, int port) {
	ConfigMap templateConfigMap = client.configMaps().inNamespace(namespace).withName(OAUTH2_PROXY_CONFIGMAP_NAME)
		.get();
	Map<String, String> data = new LinkedHashMap<>(templateConfigMap.getData());
	data.put(OAUTH2_PROXY_CFG, data.get(OAUTH2_PROXY_CFG)//
		.replace(CONFIGMAP_DATA_PLACEHOLDER_HOST, "https://" + host)//
		.replace(CONFIGMAP_DATA_PLACEHOLDER_PORT, String.valueOf(port)));
	configMap.setData(data);
    }

    public static void updateWorkspaceURLAsync(DefaultKubernetesClient client, Workspace workspace, String namespace,
	    String url, String correlationId) {
	EXECUTOR.execute(() -> {
	    for (int i = 1; i <= 60; i++) {
		try {
		    Thread.sleep(i * 1000);
		} catch (InterruptedException e) {
		    /* silent */
		}

		HttpURLConnection connection;
		try {
		    connection = (HttpURLConnection) new URL("https://" + url).openConnection();
		} catch (IOException e) {
		    LOGGER.error(formatLogMessage(correlationId, "Error while checking workspace availability."), e);
		    continue;
		}
		int code;

		try {
		    connection.connect();
		    code = connection.getResponseCode();
		} catch (IOException e) {
		    LOGGER.trace(formatLogMessage(correlationId, url + " is NOT available yet."));
		    break;
		}

		LOGGER.trace(formatLogMessage(correlationId, url + " has response code " + code));

		if (code == 200) {
		    LOGGER.info(formatLogMessage(correlationId, url + " is available."));
		    client.customResources(Workspace.class, WorkspaceSpecResourceList.class).inNamespace(namespace)
			    .withName(workspace.getMetadata().getName())//
			    .edit(ws -> {
				ws.getSpec().setUrl(url);
				return ws;
			    });
		    break;
		} else {
		    LOGGER.trace(formatLogMessage(correlationId, url + " is NOT available yet."));
		}

	    }

	});

    }

}