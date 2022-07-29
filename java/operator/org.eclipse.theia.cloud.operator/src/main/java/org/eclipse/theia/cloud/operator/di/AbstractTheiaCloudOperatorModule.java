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

import java.util.function.Consumer;

import org.eclipse.theia.cloud.common.k8s.client.AppDefinitionResourceClient;
import org.eclipse.theia.cloud.common.k8s.client.DefaultAppDefinitionResourceClient;
import org.eclipse.theia.cloud.common.k8s.client.DefaultSessionResourceClient;
import org.eclipse.theia.cloud.common.k8s.client.DefaultTheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.client.DefaultWorkspaceResourceClient;
import org.eclipse.theia.cloud.common.k8s.client.SessionResourceClient;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.client.WorkspaceResourceClient;
import org.eclipse.theia.cloud.common.util.CustomResourceUtil;
import org.eclipse.theia.cloud.operator.TheiaCloud;
import org.eclipse.theia.cloud.operator.TheiaCloudImpl;
import org.eclipse.theia.cloud.operator.handler.AppDefinitionHandler;
import org.eclipse.theia.cloud.operator.handler.BandwidthLimiter;
import org.eclipse.theia.cloud.operator.handler.DeploymentTemplateReplacements;
import org.eclipse.theia.cloud.operator.handler.IngressPathProvider;
import org.eclipse.theia.cloud.operator.handler.PersistentVolumeCreator;
import org.eclipse.theia.cloud.operator.handler.SessionHandler;
import org.eclipse.theia.cloud.operator.handler.TimeoutStrategy;
import org.eclipse.theia.cloud.operator.handler.WorkspaceHandler;
import org.eclipse.theia.cloud.operator.handler.impl.BandwidthLimiterImpl;
import org.eclipse.theia.cloud.operator.handler.impl.DefaultDeploymentTemplateReplacements;
import org.eclipse.theia.cloud.operator.handler.impl.DefaultPersistentVolumeCreator;
import org.eclipse.theia.cloud.operator.handler.impl.IngressPathProviderImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.fabric8.kubernetes.client.NamespacedKubernetesClient;

public abstract class AbstractTheiaCloudOperatorModule extends AbstractModule implements TheiaCloudOperatorModule {
    @Override
    protected void configure() {
	bind(TheiaCloud.class).to(bindTheiaCloud()).in(Singleton.class);

	bind(BandwidthLimiter.class).to(bindBandwidthLimiter()).in(Singleton.class);
	bind(PersistentVolumeCreator.class).to(bindPersistentVolumeHandler()).in(Singleton.class);
	bind(IngressPathProvider.class).to(bindIngressPathProvider()).in(Singleton.class);
	bind(DeploymentTemplateReplacements.class).to(bindDeploymentTemplateReplacements()).in(Singleton.class);

	bind(AppDefinitionHandler.class).to(bindAppDefinitionHandler()).in(Singleton.class);
	bind(SessionHandler.class).to(bindSessionHandler()).in(Singleton.class);
	bind(WorkspaceHandler.class).to(bindWorkspaceHandler()).in(Singleton.class);

	configure(MultiBinding.create(TimeoutStrategy.class), this::configureTimeoutStrategies);
    }

    protected <T> void configure(final MultiBinding<T> binding, final Consumer<MultiBinding<T>> configurator) {
	configurator.accept(binding);
	binding.applyBinding(binder());
    }

    protected Class<? extends TheiaCloud> bindTheiaCloud() {
	return TheiaCloudImpl.class;
    }

    protected Class<? extends BandwidthLimiter> bindBandwidthLimiter() {
	return BandwidthLimiterImpl.class;
    }

    protected Class<? extends PersistentVolumeCreator> bindPersistentVolumeHandler() {
	return DefaultPersistentVolumeCreator.class;
    }

    protected Class<? extends IngressPathProvider> bindIngressPathProvider() {
	return IngressPathProviderImpl.class;
    }

    protected Class<? extends DeploymentTemplateReplacements> bindDeploymentTemplateReplacements() {
	return DefaultDeploymentTemplateReplacements.class;
    }

    protected abstract Class<? extends WorkspaceHandler> bindWorkspaceHandler();

    protected abstract Class<? extends AppDefinitionHandler> bindAppDefinitionHandler();

    protected abstract Class<? extends SessionHandler> bindSessionHandler();

    protected void configureTimeoutStrategies(final MultiBinding<TimeoutStrategy> binding) {
	binding.add(TimeoutStrategy.FixedTime.class);
	binding.add(TimeoutStrategy.Inactivity.class);
    }

    @Provides
    @Singleton
    protected NamespacedKubernetesClient provideKubernetesClient() {
	return CustomResourceUtil.createClient();
    }

    @Provides
    @Singleton
    @Named(NAMESPACE)
    protected String getNamespace(final NamespacedKubernetesClient client) {
	return client.getNamespace();
    }

    @Provides
    @Singleton
    protected TheiaCloudClient provideTheiaCloudClient(final NamespacedKubernetesClient client) {
	return new DefaultTheiaCloudClient(client);
    }

    @Provides
    @Singleton
    protected AppDefinitionResourceClient provideAppDefinitionResourceClient(final NamespacedKubernetesClient client) {
	return new DefaultAppDefinitionResourceClient(client);
    }

    @Provides
    @Singleton
    protected WorkspaceResourceClient provideWorkspaceResourceClient(final NamespacedKubernetesClient client) {
	return new DefaultWorkspaceResourceClient(client);
    }

    @Provides
    @Singleton
    protected SessionResourceClient provideSessionResourceClient(final NamespacedKubernetesClient client) {
	return new DefaultSessionResourceClient(client);
    }

}
