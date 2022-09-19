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
package org.eclipse.theia.cloud.operator.handler.util;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.resource.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.Session;
import org.eclipse.theia.cloud.common.util.NamingUtil;
import org.eclipse.theia.cloud.operator.handler.IngressPathProvider;

import io.fabric8.kubernetes.api.model.apps.Deployment;

public final class TheiaCloudDeploymentUtil {

    private static final Logger LOGGER = LogManager.getLogger(TheiaCloudDeploymentUtil.class);

    public static final String HOST_PROTOCOL = "https://";
    public static final String DEPLOYMENT_NAME = "deployment";

    private TheiaCloudDeploymentUtil() {
    }

    public static String getSessionURL(IngressPathProvider ingressPathProvider, AppDefinition appDefinition,
	    Session session) {
	return getSessionURL(appDefinition.getSpec().getHost(), ingressPathProvider.getPath(appDefinition, session));
    }

    public static String getSessionURL(IngressPathProvider ingressPathProvider, AppDefinition appDefinition,
	    int instance) {
	return getSessionURL(appDefinition.getSpec().getHost(), ingressPathProvider.getPath(appDefinition, instance));
    }

    private static String getSessionURL(String host, String path) {
	return HOST_PROTOCOL + host + path + "/";
    }

    public static String getDeploymentName(AppDefinition appDefinition, int instance) {
	return NamingUtil.createName(appDefinition, instance, DEPLOYMENT_NAME);
    }

    public static String getDeploymentName(Session session) {
	return NamingUtil.createName(session, DEPLOYMENT_NAME);
    }

    public static Integer getId(String correlationId, AppDefinition appDefinition, Deployment deployment) {
	String instance = TheiaCloudK8sUtil.extractIdFromName(deployment.getMetadata());
	try {
	    return Integer.valueOf(instance);
	} catch (NumberFormatException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while getting integer value of " + instance), e);
	}
	return null;
    }

    public static Set<Integer> computeIdsOfMissingDeployments(AppDefinition appDefinition, String correlationId,
	    int instances, List<Deployment> existingItems) {
	return TheiaCloudHandlerUtil.computeIdsOfMissingItems(instances, existingItems,
		service -> getId(correlationId, appDefinition, service));
    }
}
