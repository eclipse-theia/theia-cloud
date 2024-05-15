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
package org.eclipse.theia.cloud.operator.util;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.util.NamingUtil;

import io.fabric8.kubernetes.api.model.ConfigMap;

public final class TheiaCloudConfigMapUtil {

    private static final Logger LOGGER = LogManager.getLogger(TheiaCloudConfigMapUtil.class);

    public static final String CONFIGMAP_PROXY_NAME = "config";
    public static final String CONFIGMAP_EMAIL_NAME = "emailconfig";

    private TheiaCloudConfigMapUtil() {
    }

    public static String getProxyConfigName(AppDefinition appDefinition, int instance) {
	return NamingUtil.createName(appDefinition, instance, CONFIGMAP_PROXY_NAME);
    }

    public static String getProxyConfigName(Session session) {
	return NamingUtil.createName(session, CONFIGMAP_PROXY_NAME);
    }

    public static String getEmailConfigName(AppDefinition appDefinition, int instance) {
	return NamingUtil.createName(appDefinition, instance, CONFIGMAP_EMAIL_NAME);
    }

    public static String getEmailConfigName(Session session) {
	return NamingUtil.createName(session, CONFIGMAP_EMAIL_NAME);
    }

    public static Integer getProxyId(String correlationId, AppDefinition appDefinition, ConfigMap item) {
	String instance = TheiaCloudK8sUtil.extractIdFromName(item.getMetadata());
	try {
	    return Integer.valueOf(instance);
	} catch (NumberFormatException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while getting integer value of " + instance), e);
	}
	return null;
    }

    public static Integer getEmailId(String correlationId, AppDefinition appDefinition, ConfigMap item) {
	String instance = TheiaCloudK8sUtil.extractIdFromName(item.getMetadata());
	try {
	    return Integer.valueOf(instance);
	} catch (NumberFormatException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while getting integer value of " + instance), e);
	}
	return null;
    }

    public static Set<Integer> computeIdsOfMissingProxyConfigMaps(AppDefinition appDefinition, String correlationId,
	    int instances, List<ConfigMap> existingItems) {
	return TheiaCloudHandlerUtil.computeIdsOfMissingItems(instances, existingItems,
		service -> getProxyId(correlationId, appDefinition, service));
    }

    public static Set<Integer> computeIdsOfMissingEmailConfigMaps(AppDefinition appDefinition, String correlationId,
	    int instances, List<ConfigMap> existingItems) {
	return TheiaCloudHandlerUtil.computeIdsOfMissingItems(instances, existingItems,
		service -> getEmailId(correlationId, appDefinition, service));
    }

    public static Map<String, String> getProxyConfigMapReplacements(String namespace, AppDefinition appDefinition,
	    int instance) {
	Map<String, String> replacements = new LinkedHashMap<String, String>();
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_CONFIGNAME, getProxyConfigName(appDefinition, instance));
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_NAMESPACE, namespace);
	return replacements;
    }

    public static Map<String, String> getProxyConfigMapReplacements(String namespace, Session session) {
	Map<String, String> replacements = new LinkedHashMap<String, String>();
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_CONFIGNAME, getProxyConfigName(session));
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_NAMESPACE, namespace);
	return replacements;
    }

    public static Map<String, String> getEmailConfigMapReplacements(String namespace, AppDefinition appDefinition,
	    int instance) {
	Map<String, String> replacements = new LinkedHashMap<String, String>();
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_EMAILSCONFIGNAME,
		getEmailConfigName(appDefinition, instance));
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_NAMESPACE, namespace);
	return replacements;
    }

    public static Map<String, String> getEmailConfigMapReplacements(String namespace, Session session) {
	Map<String, String> replacements = new LinkedHashMap<String, String>();
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_EMAILSCONFIGNAME, getEmailConfigName(session));
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_NAMESPACE, namespace);
	return replacements;
    }

}
