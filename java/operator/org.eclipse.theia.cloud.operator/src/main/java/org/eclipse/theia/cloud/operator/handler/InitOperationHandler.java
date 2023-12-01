/********************************************************************************
 * Copyright (C) 2023 EclipseSource and others.
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

import java.util.List;

import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.Session;

import io.fabric8.kubernetes.api.model.apps.Deployment;

public interface InitOperationHandler {

    static final String THEIA_CLOUD_INIT_LABEL = "theiaCloudInit";
    static final String THEIA_CLOUD_USER_LABEL = "theiaCloudUser";

    String operationId();

    void handleInitOperation(String correlationId, TheiaCloudClient client, Deployment deployment,
	    AppDefinition appDefinition, Session session, List<String> args);

}
