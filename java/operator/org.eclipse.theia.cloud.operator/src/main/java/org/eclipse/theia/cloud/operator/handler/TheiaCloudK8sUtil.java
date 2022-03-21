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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.common.k8s.resource.WorkspaceSpec;
import org.eclipse.theia.cloud.common.k8s.resource.WorkspaceSpecResourceList;
import org.eclipse.theia.cloud.operator.resource.TemplateSpec;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;

public final class TheiaCloudK8sUtil {

    private static final Logger LOGGER = LogManager.getLogger(TheiaCloudK8sUtil.class);

    private TheiaCloudK8sUtil() {
    }

    public static boolean checkIfMaxInstancesReached(DefaultKubernetesClient client, String namespace,
	    WorkspaceSpec workspaceSpec, TemplateSpec templateSpec, String correlationId) {

	if (templateSpec.getMaxInstances() < 1) {
	    LOGGER.info(formatLogMessage(correlationId,
		    "Template " + templateSpec.getName() + " allows indefinite workspaces."));
	    return false;
	}

	long currentInstances = client.customResources(Workspace.class, WorkspaceSpecResourceList.class)
		.inNamespace(namespace).list().getItems().stream()//
		.filter(w -> {
		    String template = w.getSpec().getTemplate();
		    boolean result = template.equals(templateSpec.getName());
		    LOGGER.trace(formatLogMessage(correlationId, "Counting instances of template "
			    + templateSpec.getName() + ": Is " + w.getSpec() + " of template? " + result));
		    return result;
		})//
		.count();
	return currentInstances > templateSpec.getMaxInstances();
    }

}
