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
package org.eclipse.theia.cloud.operator.util;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionSpecResourceList;
import org.eclipse.theia.cloud.common.util.NamingUtil;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;

public final class TheiaCloudK8sUtil {

    private static final Logger LOGGER = LogManager.getLogger(TheiaCloudK8sUtil.class);

    private TheiaCloudK8sUtil() {
    }

    public static boolean checkIfMaxInstancesReached(NamespacedKubernetesClient client, String namespace,
	    SessionSpec sessionSpec, AppDefinitionSpec appDefinitionSpec, String correlationId) {

	if (appDefinitionSpec.getMaxInstances() == null || appDefinitionSpec.getMaxInstances() < 0) {
	    LOGGER.debug(formatLogMessage(correlationId,
		    "App Definition " + appDefinitionSpec.getName() + " allows indefinite sessions."));
	    return false;
	}

	final String appDefinitionName = appDefinitionSpec.getName();
	if (appDefinitionName == null || appDefinitionName.isBlank()) {
	    LOGGER.error(
		    formatLogMessage(correlationId, "The App Definition does not have a name: " + appDefinitionSpec));
	    return true;
	}

	long currentInstances = client.resources(Session.class, SessionSpecResourceList.class).inNamespace(namespace)
		.list().getItems().stream()//
		.filter(w -> {
		    String sessionAppDefinition = w.getSpec().getAppDefinition();
		    boolean result = appDefinitionName.equals(sessionAppDefinition);
		    LOGGER.trace(formatLogMessage(correlationId, "Counting instances of app definition "
			    + appDefinitionSpec.getName() + ": Is " + w.getSpec() + " of app definition? " + result));
		    return result;
		})//
		.count();
	return currentInstances > appDefinitionSpec.getMaxInstances();
    }

    public static String extractIdFromName(ObjectMeta metadata) {
	String name = metadata.getName();
	String[] split = name.split("-");
	String instance = split.length == 0 ? "" : split[0];
	// kubernetes names must not start with letter, remove automatically added
	// prefix
	instance = instance.length() == 0 ? ""
		: instance.charAt(0) == NamingUtil.VALID_NAME_PREFIX ? instance.substring(1) : instance;
	return instance;
    }

}
