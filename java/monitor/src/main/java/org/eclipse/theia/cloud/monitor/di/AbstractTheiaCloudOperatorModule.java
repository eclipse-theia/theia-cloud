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
package org.eclipse.theia.cloud.monitor.di;

import java.util.function.Consumer;

import org.eclipse.theia.cloud.monitor.TheiaCloudMonitor;
import org.eclipse.theia.cloud.monitor.TheiaCloudMonitorImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public abstract class AbstractTheiaCloudOperatorModule extends AbstractModule {
    @Override
    protected void configure() {
	bind(TheiaCloudMonitor.class).to(bindTheiaCloudMonitor()).in(Singleton.class);
    }

    protected <T> void configure(final MultiBinding<T> binding, final Consumer<MultiBinding<T>> configurator) {
	configurator.accept(binding);
	binding.applyBinding(binder());
    }

    protected Class<? extends TheiaCloudMonitor> bindTheiaCloudMonitor() {
	return TheiaCloudMonitorImpl.class;
    }
}
