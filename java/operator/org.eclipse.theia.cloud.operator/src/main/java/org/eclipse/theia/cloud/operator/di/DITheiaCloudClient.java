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
package org.eclipse.theia.cloud.operator.di;

import javax.inject.Inject;

import org.eclipse.theia.cloud.common.k8s.client.AppDefinitionResourceClient;
import org.eclipse.theia.cloud.common.k8s.client.DefaultTheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.client.SessionResourceClient;
import org.eclipse.theia.cloud.common.k8s.client.WorkspaceResourceClient;

import io.fabric8.kubernetes.client.NamespacedKubernetesClient;

public class DITheiaCloudClient extends DefaultTheiaCloudClient {

    @Inject
    protected WorkspaceResourceClient workspaceClient;

    @Inject
    protected AppDefinitionResourceClient appDefinitionResourceClient;

    @Inject
    protected SessionResourceClient sessionClient;

    @Inject
    public DITheiaCloudClient(NamespacedKubernetesClient client) {
	super(client);
    }

    @Override
    public WorkspaceResourceClient workspaces() {
	return workspaceClient;
    }

    @Override
    public AppDefinitionResourceClient appDefinitions() {
	return appDefinitionResourceClient;
    }

    @Override
    public SessionResourceClient sessions() {
	return sessionClient;
    }
}
