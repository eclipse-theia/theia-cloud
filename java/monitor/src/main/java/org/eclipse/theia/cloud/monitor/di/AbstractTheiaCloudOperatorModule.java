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

import org.eclipse.theia.cloud.common.k8s.client.DefaultTheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.AppDefinitionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.SessionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.WorkspaceSpec;
import org.eclipse.theia.cloud.common.util.CustomResourceUtil;
import org.eclipse.theia.cloud.monitor.TheiaCloudMonitor;
import org.eclipse.theia.cloud.monitor.TheiaCloudMonitorImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.client.NamespacedKubernetesClient;

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

    @Provides
    @Singleton
    protected NamespacedKubernetesClient provideKubernetesClient() {
	NamespacedKubernetesClient client = CustomResourceUtil.createClient();
	CustomResourceUtil.validateCustomResource(client, SessionSpec.CRD_NAME);
	CustomResourceUtil.validateCustomResource(client, WorkspaceSpec.CRD_NAME);
	CustomResourceUtil.validateCustomResource(client, AppDefinitionSpec.CRD_NAME);
	return client;
    }

    @Provides
    @Singleton
    protected TheiaCloudClient provideTheiaCloudClient(final NamespacedKubernetesClient client) {
	return new DefaultTheiaCloudClient(client);
    }
}
