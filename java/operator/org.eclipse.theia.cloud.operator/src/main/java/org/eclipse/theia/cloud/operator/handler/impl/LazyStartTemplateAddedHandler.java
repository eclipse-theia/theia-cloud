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
import org.eclipse.theia.cloud.operator.handler.TemplateAddedHandler;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudIngressUtil;
import org.eclipse.theia.cloud.operator.resource.TemplateSpec;
import org.eclipse.theia.cloud.operator.resource.TemplateSpecResource;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;

public class LazyStartTemplateAddedHandler implements TemplateAddedHandler {

    private static final Logger LOGGER = LogManager.getLogger(LazyStartTemplateAddedHandler.class);

    @Override
    public void handle(DefaultKubernetesClient client, TemplateSpecResource template, String namespace,
	    String correlationId) {
	TemplateSpec spec = template.getSpec();
	LOGGER.info(formatLogMessage(correlationId, "Handling " + spec));

	String templateResourceName = template.getMetadata().getName();
	String templateResourceUID = template.getMetadata().getUid();

	/* Create ingress if not existing */
	if (!TheiaCloudIngressUtil.hasExistingIngress(client, namespace, template)) {
	    LOGGER.trace(formatLogMessage(correlationId, "No existing Ingress"));
	    AddedHandler.createAndApplyIngress(client, namespace, correlationId, templateResourceName,
		    templateResourceUID, template);
	} else {
	    LOGGER.trace(formatLogMessage(correlationId, "Ingress available already"));
	}

    }

}
