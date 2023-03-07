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
package org.eclipse.theia.cloud.operator.di;

import org.eclipse.theia.cloud.operator.TheiaCloudArguments;
import org.eclipse.theia.cloud.operator.handler.AppDefinitionHandler;
import org.eclipse.theia.cloud.operator.handler.PersistentVolumeCreator;
import org.eclipse.theia.cloud.operator.handler.SessionHandler;
import org.eclipse.theia.cloud.operator.handler.WorkspaceHandler;
import org.eclipse.theia.cloud.operator.handler.impl.EagerStartAppDefinitionAddedHandler;
import org.eclipse.theia.cloud.operator.handler.impl.EagerStartSessionHandler;
import org.eclipse.theia.cloud.operator.handler.impl.LazySessionHandler;
import org.eclipse.theia.cloud.operator.handler.impl.LazyStartAppDefinitionHandler;
import org.eclipse.theia.cloud.operator.handler.impl.LazyWorkspaceHandler;
import org.eclipse.theia.cloud.operator.handler.impl.MinikubePersistentVolumeCreator;

public class DefaultTheiaCloudOperatorModule extends AbstractTheiaCloudOperatorModule {

    private TheiaCloudArguments arguments;

    public DefaultTheiaCloudOperatorModule(TheiaCloudArguments arguments) {
	this.arguments = arguments;
    }

    @Override
    protected void configure() {
	bind(TheiaCloudArguments.class).toInstance(arguments);
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

}
