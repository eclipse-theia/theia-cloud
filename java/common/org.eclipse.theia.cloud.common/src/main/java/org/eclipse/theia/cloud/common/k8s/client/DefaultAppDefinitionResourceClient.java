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

import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionV8beta;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionV8betaSpec;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionV8betaSpecResourceList;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionV8betaStatus;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;

public class DefaultAppDefinitionResourceClient extends BaseResourceClient<AppDefinitionV8beta, AppDefinitionV8betaSpecResourceList>
	implements AppDefinitionResourceClient {

    public DefaultAppDefinitionResourceClient(NamespacedKubernetesClient client) {
	super(client, AppDefinitionV8beta.class, AppDefinitionV8betaSpecResourceList.class);
    }

    @Override
    public AppDefinitionV8beta create(String correlationId, AppDefinitionV8betaSpec spec) {
	AppDefinitionV8beta appDefinition = new AppDefinitionV8beta();
	appDefinition.setSpec(spec);

	ObjectMeta metadata = new ObjectMeta();
	metadata.setName(spec.getName());
	appDefinition.setMetadata(metadata);

	info(correlationId, "Create AppDefinition " + appDefinition.getSpec());
	return operation().create(appDefinition);
    }

    @Override
    public AppDefinitionV8betaStatus createDefaultStatus() {
	return new AppDefinitionV8betaStatus();
    }

}
