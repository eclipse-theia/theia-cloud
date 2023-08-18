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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionV8beta;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionV6beta;
import org.eclipse.theia.cloud.operator.TheiaCloudArguments;
import org.eclipse.theia.cloud.operator.handler.IngressPathProvider;

import com.google.inject.Inject;

public class IngressPathProviderImpl implements IngressPathProvider {

    private static final Logger LOGGER = LogManager.getLogger(IngressPathProviderImpl.class);

    @Inject
    TheiaCloudArguments arguments;

    @Override
    public String getPath(AppDefinitionV8beta appDefinition, int instance) {
	return getBasePath() + appDefinition.getSpec().getName() + "-" + instance;
    }

    @Override
    public String getPath(AppDefinitionV8beta appDefinition, SessionV6beta session) {
	return getBasePath() + session.getMetadata().getUid();
    }

    protected String getBasePath() {
	if (arguments.isUsePaths() && arguments.getInstancesPath() != null && !arguments.getInstancesPath().isBlank()) {
	    return "/" + arguments.getInstancesPath().trim() + "/";
	} else if (arguments.isUsePaths()) {
	    LOGGER.warn(
		    "Theia cloud is configured to use paths instead of subdomains but no instace subpath was provided");
	}
	return "/";
    }
}
