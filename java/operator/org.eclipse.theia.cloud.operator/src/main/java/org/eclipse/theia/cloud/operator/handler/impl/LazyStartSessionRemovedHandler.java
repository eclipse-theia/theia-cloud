/********************************************************************************
 * Copyright (C) 2022 EclipseSource and others.
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

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.resource.Session;
import org.eclipse.theia.cloud.common.k8s.resource.SessionSpec;
import org.eclipse.theia.cloud.operator.di.TheiaCloudOperatorModule;
import org.eclipse.theia.cloud.operator.handler.IngressPathProvider;
import org.eclipse.theia.cloud.operator.handler.K8sUtil;
import org.eclipse.theia.cloud.operator.handler.SessionRemovedHandler;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudHandlerUtil;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudIngressUtil;
import org.eclipse.theia.cloud.operator.resource.AppDefinition;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;

public class LazyStartSessionRemovedHandler implements SessionRemovedHandler {

    private static final Logger LOGGER = LogManager.getLogger(LazyStartSessionRemovedHandler.class);

    @Inject
    protected IngressPathProvider ingressPathProvider;
    @Inject
    protected NamespacedKubernetesClient client;
    @Inject
    @Named(TheiaCloudOperatorModule.NAMESPACE)
    protected String namespace;

    @Override
    public boolean handle(Session session, String correlationId) {
	/* session information */
	SessionSpec sessionSpec = session.getSpec();

	/* find appDefinition for session */
	String appDefinitionID = sessionSpec.getAppDefinition();
	Optional<AppDefinition> optionalAppDefinition = TheiaCloudHandlerUtil.getAppDefinitionForSession(client,
		namespace, appDefinitionID);
	if (optionalAppDefinition.isEmpty()) {
	    LOGGER.error(formatLogMessage(correlationId, "No App Definition with name " + appDefinitionID + " found."));
	    return false;
	}

	/* find ingress */
	String appDefinitionResourceName = optionalAppDefinition.get().getMetadata().getName();
	String appDefinitionResourceUID = optionalAppDefinition.get().getMetadata().getUid();
	Optional<Ingress> ingress = K8sUtil.getExistingIngress(client, namespace, appDefinitionResourceName,
		appDefinitionResourceUID);
	if (ingress.isEmpty()) {
	    LOGGER.error(
		    formatLogMessage(correlationId, "No Ingress for app definition " + appDefinitionID + " found."));
	    return false;
	}

	String path = ingressPathProvider.getPath(optionalAppDefinition.get(), session);
	TheiaCloudIngressUtil.removeIngressRule(client, namespace, ingress.get(), path, correlationId);

	return true;
    }

}
