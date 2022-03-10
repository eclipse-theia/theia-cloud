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
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.common.k8s.resource.WorkspaceSpec;
import org.eclipse.theia.cloud.operator.handler.IngressPathProvider;
import org.eclipse.theia.cloud.operator.handler.K8sUtil;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudHandlerUtil;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudIngressUtil;
import org.eclipse.theia.cloud.operator.handler.WorkspaceRemovedHandler;
import org.eclipse.theia.cloud.operator.resource.TemplateSpecResource;

import com.google.inject.Inject;

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;

public class LazyStartWorkspaceRemovedHandler implements WorkspaceRemovedHandler {

    private static final Logger LOGGER = LogManager.getLogger(LazyStartWorkspaceRemovedHandler.class);

    private IngressPathProvider ingressPathProvider;

    @Inject
    public LazyStartWorkspaceRemovedHandler(IngressPathProvider ingressPathProvider) {
	this.ingressPathProvider = ingressPathProvider;
    }

    @Override
    public boolean handle(DefaultKubernetesClient client, Workspace workspace, String namespace, String correlationId) {
	/* workspace information */
	WorkspaceSpec workspaceSpec = workspace.getSpec();

	/* find template for workspace */
	String templateID = workspaceSpec.getTemplate();
	Optional<TemplateSpecResource> optionalTemplate = TheiaCloudHandlerUtil.getTemplateSpecForWorkspace(client,
		namespace, templateID);
	if (optionalTemplate.isEmpty()) {
	    LOGGER.error(formatLogMessage(correlationId, "No Template with name " + templateID + " found."));
	    return false;
	}

	/* find ingress */
	String templateResourceName = optionalTemplate.get().getMetadata().getName();
	String templateResourceUID = optionalTemplate.get().getMetadata().getUid();
	Optional<Ingress> ingress = K8sUtil.getExistingIngress(client, namespace, templateResourceName,
		templateResourceUID);
	if (ingress.isEmpty()) {
	    LOGGER.error(formatLogMessage(correlationId, "No Ingress for template " + templateID + " found."));
	    return false;
	}

	String path = ingressPathProvider.getPath(optionalTemplate.get(), workspace);
	TheiaCloudIngressUtil.removeIngressRule(client, namespace, ingress.get(), path, correlationId);

	return true;
    }

}
