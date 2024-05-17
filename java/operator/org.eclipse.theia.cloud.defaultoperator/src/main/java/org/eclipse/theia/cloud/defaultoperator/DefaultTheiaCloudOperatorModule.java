/********************************************************************************
 * Copyright (C) 2022-2023 EclipseSource, Lockular, Ericsson, STMicroelectronics and 
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
package org.eclipse.theia.cloud.defaultoperator;

import org.eclipse.theia.cloud.operator.TheiaCloudOperatorArguments;
import org.eclipse.theia.cloud.operator.TheiaCloudOperator;
import org.eclipse.theia.cloud.operator.di.AbstractTheiaCloudOperatorModule;
import org.eclipse.theia.cloud.operator.handler.appdef.AppDefinitionHandler;
import org.eclipse.theia.cloud.operator.handler.appdef.EagerStartAppDefinitionAddedHandler;
import org.eclipse.theia.cloud.operator.handler.appdef.LazyStartAppDefinitionHandler;
import org.eclipse.theia.cloud.operator.handler.session.EagerStartSessionHandler;
import org.eclipse.theia.cloud.operator.handler.session.LazySessionHandler;
import org.eclipse.theia.cloud.operator.handler.session.SessionHandler;
import org.eclipse.theia.cloud.operator.handler.ws.LazyWorkspaceHandler;
import org.eclipse.theia.cloud.operator.handler.ws.WorkspaceHandler;
import org.eclipse.theia.cloud.operator.pv.MinikubePersistentVolumeCreator;
import org.eclipse.theia.cloud.operator.pv.PersistentVolumeCreator;

public class DefaultTheiaCloudOperatorModule extends AbstractTheiaCloudOperatorModule {

    private TheiaCloudOperatorArguments arguments;

    public DefaultTheiaCloudOperatorModule(TheiaCloudOperatorArguments arguments) {
	this.arguments = arguments;
    }

    @Override
    protected void configure() {
	bind(TheiaCloudOperatorArguments.class).toInstance(arguments);
	super.configure();
    }

    @Override
    protected Class<? extends PersistentVolumeCreator> bindPersistentVolumeHandler() {
	switch (arguments.getCloudProvider()) {
	case MINIKUBE:
	    return MinikubePersistentVolumeCreator.class;
	case K8S:
	default:
	    return super.bindPersistentVolumeHandler();
	}
    }

    @Override
    protected Class<? extends AppDefinitionHandler> bindAppDefinitionHandler() {
	if (arguments.isEagerStart()) {
	    return EagerStartAppDefinitionAddedHandler.class;
	} else {
	    return LazyStartAppDefinitionHandler.class;
	}
    }

    @Override
    protected Class<? extends WorkspaceHandler> bindWorkspaceHandler() {
	return LazyWorkspaceHandler.class;
    }

    @Override
    protected Class<? extends SessionHandler> bindSessionHandler() {
	if (arguments.isEagerStart()) {
	    return EagerStartSessionHandler.class;
	} else {
	    return LazySessionHandler.class;
	}
    }

    @Override
    protected Class<? extends TheiaCloudOperator> bindTheiaCloudOperator() {
	return DefaultTheiaCloudOperator.class;
    }

}
