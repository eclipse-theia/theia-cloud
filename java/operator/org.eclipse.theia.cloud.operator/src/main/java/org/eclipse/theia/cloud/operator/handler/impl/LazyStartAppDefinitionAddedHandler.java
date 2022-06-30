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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.operator.handler.AppDefinitionAddedHandler;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudIngressUtil;
import org.eclipse.theia.cloud.operator.resource.AppDefinitionSpec;
import org.eclipse.theia.cloud.operator.resource.AppDefinitionSpecResource;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;

public class LazyStartAppDefinitionAddedHandler implements AppDefinitionAddedHandler {

    private static final Logger LOGGER = LogManager.getLogger(LazyStartAppDefinitionAddedHandler.class);

    @Override
    public void handle(DefaultKubernetesClient client, AppDefinitionSpecResource appDefinition, String namespace,
	    String correlationId) {
	AppDefinitionSpec spec = appDefinition.getSpec();
	LOGGER.info(formatLogMessage(correlationId, "Handling " + spec));

	String appDefinitionResourceName = appDefinition.getMetadata().getName();
	String appDefinitionResourceUID = appDefinition.getMetadata().getUid();

	/* Create ingress if not existing */
	if (!TheiaCloudIngressUtil.checkForExistingIngressAndAddOwnerReferencesIfMissing(client, namespace, appDefinition,
		correlationId)) {
	    LOGGER.trace(formatLogMessage(correlationId, "No existing Ingress"));
	    AddedHandler.createAndApplyIngress(client, namespace, correlationId, appDefinitionResourceName,
		    appDefinitionResourceUID, appDefinition);
	} else {
	    LOGGER.trace(formatLogMessage(correlationId, "Ingress available already"));
	}

    }

}
