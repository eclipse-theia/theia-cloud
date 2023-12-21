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
package org.eclipse.theia.cloud.conversion.mappers.appdefinition;

import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.hub.AppDefinitionHub;

import io.javaoperatorsdk.webhook.conversion.Mapper;
import io.javaoperatorsdk.webhook.conversion.TargetVersion;

@TargetVersion("v1beta8")
public class AppDefinitionV1beta8Mapper implements Mapper<AppDefinition, AppDefinitionHub> {

    @Override
    public AppDefinitionHub toHub(AppDefinition resource) {
	return new AppDefinitionHub(resource);
    }

    @Override
    public AppDefinition fromHub(AppDefinitionHub hub) {
	return new AppDefinition(hub);
    }

}
