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
package org.eclipse.theia.cloud.common.k8s.client;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.Client;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.impl.KubernetesClientImpl;

public class DefaultTheiaCloudClient extends KubernetesClientImpl implements TheiaCloudClient {

    private NamespacedKubernetesClient client;

    public DefaultTheiaCloudClient(Client client) {
	super(client.adapt(KubernetesClientImpl.class));
	this.client = client.adapt(NamespacedKubernetesClient.class);
    }

    @Override
    public TheiaCloudClient inNamespace(String namespace) {
	this.client = this.client.inNamespace(namespace);
	return this;
    }

    @Override
    public String namespace() {
	return this.client.getNamespace();
    }

    @Override
    public NamespacedKubernetesClient kubernetes() {
	return client;
    }

    @Override
    public <T extends HasMetadata, L extends KubernetesResourceList<T>> ResourceClient<T, L> client(Class<T> typeClass,
	    Class<L> listClass) {
	return new BaseResourceClient<T, L>(this.client, typeClass, listClass);
    }

    @Override
    public WorkspaceResourceClient workspaces() {
	return new DefaultWorkspaceResourceClient(kubernetes());
    }

    @Override
    public SessionResourceClient sessions() {
	return new DefaultSessionResourceClient(kubernetes());
    }

    @Override
    public AppDefinitionResourceClient appDefinitions() {
	return new DefaultAppDefinitionResourceClient(kubernetes());
    }
}
