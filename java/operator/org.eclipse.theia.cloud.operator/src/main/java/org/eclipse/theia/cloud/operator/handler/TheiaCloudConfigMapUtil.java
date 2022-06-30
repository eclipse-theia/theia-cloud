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
package org.eclipse.theia.cloud.operator.handler;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.operator.resource.AppDefinitionSpecResource;

import io.fabric8.kubernetes.api.model.ConfigMap;

public final class TheiaCloudConfigMapUtil {

    private static final Logger LOGGER = LogManager.getLogger(TheiaCloudConfigMapUtil.class);

    public static final String CONFIGMAP_PROXY_NAME = "-config-";
    public static final String CONFIGMAP_EMAIL_NAME = "-emailconfig-";

    private TheiaCloudConfigMapUtil() {
    }

    public static String getProxyConfigName(AppDefinitionSpecResource appDefinition, int instance) {
	return K8sUtil.validString(getProxyConfigNamePrefix(appDefinition) + instance);
    }

    public static String getProxyConfigName(Workspace workspace) {
	return K8sUtil.validString(getProxyConfigNamePrefix(workspace) + workspace.getMetadata().getUid());
    }

    public static String getEmailConfigName(AppDefinitionSpecResource appDefinition, int instance) {
	return K8sUtil.validString(getEmailConfigNamePrefix(appDefinition) + instance);
    }

    public static String getEmailConfigName(Workspace workspace) {
	return K8sUtil.validString(getEmailConfigNamePrefix(workspace) + workspace.getMetadata().getUid());
    }

    private static String getProxyConfigNamePrefix(AppDefinitionSpecResource appDefinition) {
	return appDefinition.getSpec().getName() + CONFIGMAP_PROXY_NAME;
    }

    private static String getProxyConfigNamePrefix(Workspace workspace) {
	return workspace.getSpec().getName() + CONFIGMAP_PROXY_NAME;
    }

    private static String getEmailConfigNamePrefix(AppDefinitionSpecResource appDefinition) {
	return appDefinition.getSpec().getName() + CONFIGMAP_EMAIL_NAME;
    }

    public static String getEmailConfigNamePrefix(Workspace workspace) {
	return workspace.getSpec().getName() + CONFIGMAP_EMAIL_NAME;
    }

    public static Integer getProxyId(String correlationId, AppDefinitionSpecResource appDefinition, ConfigMap item) {
	int namePrefixLength = getProxyConfigNamePrefix(appDefinition).length();
	String name = item.getMetadata().getName();
	String instance = name.substring(namePrefixLength);
	try {
	    return Integer.valueOf(instance);
	} catch (NumberFormatException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while getting integer value of " + instance), e);
	}
	return null;
    }

    public static Integer getEmailId(String correlationId, AppDefinitionSpecResource appDefinition, ConfigMap item) {
	int namePrefixLength = getEmailConfigNamePrefix(appDefinition).length();
	String name = item.getMetadata().getName();
	String instance = name.substring(namePrefixLength);
	try {
	    return Integer.valueOf(instance);
	} catch (NumberFormatException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while getting integer value of " + instance), e);
	}
	return null;
    }

    public static Set<Integer> computeIdsOfMissingProxyConfigMaps(AppDefinitionSpecResource appDefinition, String correlationId,
	    int instances, List<ConfigMap> existingItems) {
	return TheiaCloudHandlerUtil.computeIdsOfMissingItems(instances, existingItems,
		service -> getProxyId(correlationId, appDefinition, service));
    }

    public static Set<Integer> computeIdsOfMissingEmailConfigMaps(AppDefinitionSpecResource appDefinition, String correlationId,
	    int instances, List<ConfigMap> existingItems) {
	return TheiaCloudHandlerUtil.computeIdsOfMissingItems(instances, existingItems,
		service -> getEmailId(correlationId, appDefinition, service));
    }

    public static Map<String, String> getProxyConfigMapReplacements(String namespace, AppDefinitionSpecResource appDefinition,
	    int instance) {
	Map<String, String> replacements = new LinkedHashMap<String, String>();
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_CONFIGNAME, getProxyConfigName(appDefinition, instance));
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_NAMESPACE, namespace);
	return replacements;
    }

    public static Map<String, String> getProxyConfigMapReplacements(String namespace, Workspace workspace) {
	Map<String, String> replacements = new LinkedHashMap<String, String>();
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_CONFIGNAME, getProxyConfigName(workspace));
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_NAMESPACE, namespace);
	return replacements;
    }

    public static Map<String, String> getEmailConfigMapReplacements(String namespace, AppDefinitionSpecResource appDefinition,
	    int instance) {
	Map<String, String> replacements = new LinkedHashMap<String, String>();
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_EMAILSCONFIGNAME, getEmailConfigName(appDefinition, instance));
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_NAMESPACE, namespace);
	return replacements;
    }

    public static Map<String, String> getEmailConfigMapReplacements(String namespace, Workspace workspace) {
	Map<String, String> replacements = new LinkedHashMap<String, String>();
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_EMAILSCONFIGNAME, getEmailConfigName(workspace));
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_NAMESPACE, namespace);
	return replacements;
    }

}
