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
import org.eclipse.theia.cloud.operator.resource.TemplateSpecResource;

import io.fabric8.kubernetes.api.model.apps.Deployment;

public final class TheiaCloudDeploymentUtil {

    private static final Logger LOGGER = LogManager.getLogger(TheiaCloudDeploymentUtil.class);

    public static final String DEPLOYMENT_NAME = "-deployment-";

    public static final String PLACEHOLDER_DEPLOYMENTNAME = "placeholder-depname";
    public static final String PLACEHOLDER_TEMPLATENAME = "placeholder-templatename";
    public static final String PLACEHOLDER_IMAGE = "placeholder-image";
    public static final String PLACEHOLDER_CPU_LIMITS = "placeholder-cpu-limits";
    public static final String PLACEHOLDER_MEMORY_LIMITS = "placeholder-memory-limits";
    public static final String PLACEHOLDER_CPU_REQUESTS = "placeholder-cpu-requests";
    public static final String PLACEHOLDER_MEMORY_REQUESTS = "placeholder-memory-requests";

    private TheiaCloudDeploymentUtil() {
    }

    private static String getDeploymentNamePrefix(TemplateSpecResource template) {
	return template.getSpec().getName() + DEPLOYMENT_NAME;
    }

    private static String getDeploymentNamePrefix(Workspace workspace) {
	return workspace.getSpec().getName() + DEPLOYMENT_NAME;
    }

    public static String getDeploymentName(TemplateSpecResource template, int instance) {
	return K8sUtil.validString(getDeploymentNamePrefix(template) + instance);
    }

    public static String getDeploymentName(Workspace workspace) {
	return K8sUtil.validString(getDeploymentNamePrefix(workspace) + workspace.getMetadata().getUid());
    }

    public static Integer getId(String correlationId, TemplateSpecResource template, Deployment deployment) {
	int namePrefixLength = getDeploymentNamePrefix(template).length();
	String name = deployment.getMetadata().getName();
	String instance = name.substring(namePrefixLength);
	try {
	    return Integer.valueOf(instance);
	} catch (NumberFormatException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while getting integer value of " + instance), e);
	}
	return null;
    }

    public static Set<Integer> computeIdsOfMissingDeployments(TemplateSpecResource template, String correlationId,
	    int instances, List<Deployment> existingItems) {
	return TheiaCloudHandlerUtil.computeIdsOfMissingItems(instances, existingItems,
		service -> getId(correlationId, template, service));
    }

    public static Map<String, String> getDeploymentsReplacements(String namespace, TemplateSpecResource template,
	    int instance) {
	Map<String, String> replacements = new LinkedHashMap<String, String>();
	replacements.put(PLACEHOLDER_DEPLOYMENTNAME, getDeploymentName(template, instance));
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_NAMESPACE, namespace);
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_APP,
		TheiaCloudHandlerUtil.getAppSelector(template, instance));
	replacements.put(PLACEHOLDER_TEMPLATENAME, template.getSpec().getName());
	replacements.put(PLACEHOLDER_IMAGE, template.getSpec().getImage());
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_CONFIGNAME,
		TheiaCloudConfigMapUtil.getProxyConfigName(template, instance));
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_EMAILSCONFIGNAME,
		TheiaCloudConfigMapUtil.getEmailConfigName(template, instance));
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_PORT, String.valueOf(template.getSpec().getPort()));
	replacements.put(PLACEHOLDER_CPU_LIMITS, orEmpty(template.getSpec().getLimitsCpu()));
	replacements.put(PLACEHOLDER_MEMORY_LIMITS, orEmpty(template.getSpec().getLimitsMemory()));
	replacements.put(PLACEHOLDER_CPU_REQUESTS, orEmpty(template.getSpec().getRequestsCpu()));
	replacements.put(PLACEHOLDER_MEMORY_REQUESTS, orEmpty(template.getSpec().getRequestsMemory()));
	return replacements;
    }

    public static Map<String, String> getDeploymentsReplacements(String namespace, Workspace workspace,
	    TemplateSpecResource template) {
	Map<String, String> replacements = new LinkedHashMap<String, String>();
	replacements.put(PLACEHOLDER_DEPLOYMENTNAME, getDeploymentName(workspace));
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_NAMESPACE, namespace);
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_APP, TheiaCloudHandlerUtil.getAppSelector(workspace));
	replacements.put(PLACEHOLDER_TEMPLATENAME, template.getSpec().getName());
	replacements.put(PLACEHOLDER_IMAGE, template.getSpec().getImage());
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_CONFIGNAME,
		TheiaCloudConfigMapUtil.getProxyConfigName(workspace));
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_EMAILSCONFIGNAME,
		TheiaCloudConfigMapUtil.getEmailConfigName(workspace));
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_PORT, String.valueOf(template.getSpec().getPort()));
	replacements.put(PLACEHOLDER_CPU_LIMITS, orEmpty(template.getSpec().getLimitsCpu()));
	replacements.put(PLACEHOLDER_MEMORY_LIMITS, orEmpty(template.getSpec().getLimitsMemory()));
	replacements.put(PLACEHOLDER_CPU_REQUESTS, orEmpty(template.getSpec().getRequestsCpu()));
	replacements.put(PLACEHOLDER_MEMORY_REQUESTS, orEmpty(template.getSpec().getRequestsMemory()));
	return replacements;
    }

    private static String orEmpty(String string) {
	return string == null ? "" : string;
    }

}
