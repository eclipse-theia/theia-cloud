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
package org.eclipse.theia.cloud.operator.di;

import org.eclipse.theia.cloud.operator.TheiaCloudArguments;
import org.eclipse.theia.cloud.operator.handler.PersistentVolumeHandler;
import org.eclipse.theia.cloud.operator.handler.TemplateAddedHandler;
import org.eclipse.theia.cloud.operator.handler.WorkspaceAddedHandler;
import org.eclipse.theia.cloud.operator.handler.WorkspaceRemovedHandler;
import org.eclipse.theia.cloud.operator.handler.impl.EagerStartTemplateAddedHandler;
import org.eclipse.theia.cloud.operator.handler.impl.EagerStartWorkspaceAddedHandler;
import org.eclipse.theia.cloud.operator.handler.impl.GKEPersistentVolumeHandlerImpl;
import org.eclipse.theia.cloud.operator.handler.impl.LazyStartTemplateAddedHandler;
import org.eclipse.theia.cloud.operator.handler.impl.LazyStartWorkspaceAddedHandler;
import org.eclipse.theia.cloud.operator.handler.impl.LazyStartWorkspaceRemovedHandler;

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
    protected Class<? extends PersistentVolumeHandler> bindPersistentVolumeHandler() {
	if (arguments.isEphemeralStorage()) {
	    return super.bindPersistentVolumeHandler();
	}
	if (arguments.getCloudProvider() == null) {
	    return super.bindPersistentVolumeHandler();
	}
	switch (arguments.getCloudProvider()) {
	case GKE:
	    return GKEPersistentVolumeHandlerImpl.class;
	case K8S:
	default:
	    return super.bindPersistentVolumeHandler();
	}
    }

    @Override
    protected Class<? extends TemplateAddedHandler> bindTemplateAddedHandler() {
	if (arguments.isEagerStart()) {
	    return EagerStartTemplateAddedHandler.class;
	} else {
	    return LazyStartTemplateAddedHandler.class;
	}
    }

    @Override
    protected Class<? extends WorkspaceAddedHandler> bindWorkspaceAddedHandler() {
	if (arguments.isEagerStart()) {
	    return EagerStartWorkspaceAddedHandler.class;
	} else {
	    return LazyStartWorkspaceAddedHandler.class;
	}
    }

    @Override
    protected Class<? extends WorkspaceRemovedHandler> bindWorkspaceRemovedHandler() {
	if (arguments.isEagerStart()) {
	    throw new UnsupportedOperationException("Not implemented yet");
	} else {
	    return LazyStartWorkspaceRemovedHandler.class;
	}
    }

}
