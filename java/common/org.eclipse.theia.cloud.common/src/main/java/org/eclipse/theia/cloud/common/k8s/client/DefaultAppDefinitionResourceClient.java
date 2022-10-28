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

import org.eclipse.theia.cloud.common.k8s.resource.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.AppDefinitionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.AppDefinitionSpecResourceList;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;

public class DefaultAppDefinitionResourceClient extends BaseResourceClient<AppDefinition, AppDefinitionSpecResourceList>
	implements AppDefinitionResourceClient {

    public DefaultAppDefinitionResourceClient(NamespacedKubernetesClient client) {
	super(client, AppDefinition.class, AppDefinitionSpecResourceList.class);
    }

    @Override
    public AppDefinition create(String correlationId, AppDefinitionSpec spec) {
	AppDefinition appDefinition = new AppDefinition();
	appDefinition.setSpec(spec);

	ObjectMeta metadata = new ObjectMeta();
	metadata.setName(spec.getName());
	appDefinition.setMetadata(metadata);

	info(correlationId, "Create AppDefinition " + appDefinition.getSpec());
	// TODO ES validate before creating
	return operation().create(appDefinition);
    }

}
