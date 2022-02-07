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
import org.eclipse.theia.cloud.operator.resource.TemplateSpecResource;

import io.fabric8.kubernetes.api.model.ConfigMap;

public final class TheiaCloudConfigMapUtil {

    private static final Logger LOGGER = LogManager.getLogger(TheiaCloudConfigMapUtil.class);

    public static final String CONFIGMAP_PROXY_NAME = "-config-";
    public static final String CONFIGMAP_EMAIL_NAME = "-emailconfig-";

    private TheiaCloudConfigMapUtil() {
    }

    public static String getProxyConfigName(TemplateSpecResource template, int instance) {
	return getProxyConfigNamePrefix(template) + instance;
    }

    public static String getEmailConfigName(TemplateSpecResource template, int instance) {
	return getEmailConfigNamePrefix(template) + instance;
    }

    private static String getProxyConfigNamePrefix(TemplateSpecResource template) {
	return template.getSpec().getName() + CONFIGMAP_PROXY_NAME;
    }

    private static String getEmailConfigNamePrefix(TemplateSpecResource template) {
	return template.getSpec().getName() + CONFIGMAP_EMAIL_NAME;
    }

    public static Integer getProxyId(String correlationId, TemplateSpecResource template, ConfigMap item) {
	int namePrefixLength = getProxyConfigNamePrefix(template).length();
	String name = item.getMetadata().getName();
	String instance = name.substring(namePrefixLength);
	try {
	    return Integer.valueOf(instance);
	} catch (NumberFormatException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while getting integer value of " + instance), e);
	}
	return null;
    }

    public static Integer getEmailId(String correlationId, TemplateSpecResource template, ConfigMap item) {
	int namePrefixLength = getEmailConfigNamePrefix(template).length();
	String name = item.getMetadata().getName();
	String instance = name.substring(namePrefixLength);
	try {
	    return Integer.valueOf(instance);
	} catch (NumberFormatException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while getting integer value of " + instance), e);
	}
	return null;
    }

    public static Set<Integer> computeIdsOfMissingProxyConfigMaps(TemplateSpecResource template, String correlationId,
	    int instances, List<ConfigMap> existingItems) {
	return TheiaCloudHandlerUtil.computeIdsOfMissingItems(instances, existingItems,
		service -> getProxyId(correlationId, template, service));
    }

    public static Set<Integer> computeIdsOfMissingEmailConfigMaps(TemplateSpecResource template, String correlationId,
	    int instances, List<ConfigMap> existingItems) {
	return TheiaCloudHandlerUtil.computeIdsOfMissingItems(instances, existingItems,
		service -> getEmailId(correlationId, template, service));
    }

    public static Map<String, String> getProxyConfigMapReplacements(String namespace, TemplateSpecResource template,
            int instance) {
        Map<String, String> replacements = new LinkedHashMap<String, String>();
        replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_CONFIGNAME,
        	getProxyConfigName(template, instance));
        replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_NAMESPACE, namespace);
        return replacements;
    }

    public static Map<String, String> getEmailConfigMapReplacements(String namespace, TemplateSpecResource template,
            int instance) {
        Map<String, String> replacements = new LinkedHashMap<String, String>();
        replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_EMAILSCONFIGNAME,
        	getEmailConfigName(template, instance));
        replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_NAMESPACE, namespace);
        return replacements;
    }

}
