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

import org.eclipse.theia.cloud.common.k8s.resource.Session;
import org.eclipse.theia.cloud.common.k8s.resource.SessionSpecResourceList;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.common.k8s.resource.WorkspaceSpecResourceList;
import org.eclipse.theia.cloud.operator.TheiaCloud;
import org.eclipse.theia.cloud.operator.TheiaCloudImpl;
import org.eclipse.theia.cloud.operator.handler.AppDefinitionAddedHandler;
import org.eclipse.theia.cloud.operator.handler.BandwidthLimiter;
import org.eclipse.theia.cloud.operator.handler.DeploymentTemplateReplacements;
import org.eclipse.theia.cloud.operator.handler.IngressPathProvider;
import org.eclipse.theia.cloud.operator.handler.PersistentVolumeHandler;
import org.eclipse.theia.cloud.operator.handler.SessionAddedHandler;
import org.eclipse.theia.cloud.operator.handler.SessionRemovedHandler;
import org.eclipse.theia.cloud.operator.handler.WorkspaceHandler;
import org.eclipse.theia.cloud.operator.handler.impl.BandwidthLimiterImpl;
import org.eclipse.theia.cloud.operator.handler.impl.DefaultDeploymentTemplateReplacements;
import org.eclipse.theia.cloud.operator.handler.impl.IngressPathProviderImpl;
import org.eclipse.theia.cloud.operator.handler.impl.PersistentVolumeHandlerImpl;
import org.eclipse.theia.cloud.operator.resource.AppDefinition;
import org.eclipse.theia.cloud.operator.resource.AppDefinitionSpecResourceList;
import org.eclipse.theia.cloud.operator.timeout.TimeoutStrategy;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;

import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public abstract class AbstractTheiaCloudOperatorModule extends AbstractModule implements TheiaCloudOperatorModule {
    @Override
    protected void configure() {
	bind(TheiaCloud.class).to(bindTheiaCloud()).in(Singleton.class);
	bind(NamespacedKubernetesClient.class).toInstance(createKubernetesClient());

	bind(BandwidthLimiter.class).to(bindBandwidthLimiter()).in(Singleton.class);
	bind(PersistentVolumeHandler.class).to(bindPersistentVolumeHandler()).in(Singleton.class);
	bind(IngressPathProvider.class).to(bindIngressPathProvider()).in(Singleton.class);
	bind(AppDefinitionAddedHandler.class).to(bindAppDefinitionAddedHandler()).in(Singleton.class);
	bind(SessionAddedHandler.class).to(bindSessionAddedHandler()).in(Singleton.class);
	bind(SessionRemovedHandler.class).to(bindSessionRemovedHandler()).in(Singleton.class);
	bind(DeploymentTemplateReplacements.class).to(bindDeploymentTemplateReplacements()).in(Singleton.class);
	bind(WorkspaceHandler.class).to(bindWorkspaceHandler()).in(Singleton.class);

	configure(MultiBinding.create(TimeoutStrategy.class), this::configureTimeoutStrategies);
    }

    protected Class<? extends TheiaCloud> bindTheiaCloud() {
	return TheiaCloudImpl.class;
    }

    protected NamespacedKubernetesClient createKubernetesClient() {
	return TheiaCloudModuleDefaults.createKubernetesClient();
    }

    protected Class<? extends BandwidthLimiter> bindBandwidthLimiter() {
	return BandwidthLimiterImpl.class;
    }

    protected Class<? extends PersistentVolumeHandler> bindPersistentVolumeHandler() {
	return PersistentVolumeHandlerImpl.class;
    }

    protected Class<? extends IngressPathProvider> bindIngressPathProvider() {
	return IngressPathProviderImpl.class;
    }

    protected Class<? extends DeploymentTemplateReplacements> bindDeploymentTemplateReplacements() {
	return DefaultDeploymentTemplateReplacements.class;
    }

    protected abstract Class<? extends WorkspaceHandler> bindWorkspaceHandler();

    protected abstract Class<? extends AppDefinitionAddedHandler> bindAppDefinitionAddedHandler();

    protected abstract Class<? extends SessionAddedHandler> bindSessionAddedHandler();

    protected abstract Class<? extends SessionRemovedHandler> bindSessionRemovedHandler();

    /**
     * Configuration method for multibinded values. The passed configurator is
     * typically a submethod of this module. This means that subclasses can
     * customize the {@link MultiBinding} object before the actual
     * {@link Multibinder} is created.
     *
     * @param <T>          Type of the {@link MultiBinding}
     * @param binding      The multi binding configuration object
     * @param configurator The consumer that should be used to configure the given
     *                     {@link Multibinder}
     */
    protected <T> void configure(final MultiBinding<T> binding, final Consumer<MultiBinding<T>> configurator) {
	configurator.accept(binding);
	binding.applyBinding(binder());
    }

    protected void configureTimeoutStrategies(final MultiBinding<TimeoutStrategy> binding) {
	binding.add(TimeoutStrategy.FixedTime.class);
	binding.add(TimeoutStrategy.Inactivity.class);
    }

    @Provides
    @Singleton
    protected NonNamespaceOperation<AppDefinition, AppDefinitionSpecResourceList, Resource<AppDefinition>> getAppDefinitionResourceClient(
	    final NamespacedKubernetesClient client) {
	return client.resources(AppDefinition.class, AppDefinitionSpecResourceList.class)
		.inNamespace(client.getNamespace());
    }

    @Provides
    @Singleton
    protected NonNamespaceOperation<Workspace, WorkspaceSpecResourceList, Resource<Workspace>> getWorkspaceResourceClient(
	    final NamespacedKubernetesClient client) {
	return client.resources(Workspace.class, WorkspaceSpecResourceList.class).inNamespace(client.getNamespace());
    }

    @Provides
    @Singleton
    protected NonNamespaceOperation<Session, SessionSpecResourceList, Resource<Session>> getSessionResourceClient(
	    final NamespacedKubernetesClient client) {
	return client.resources(Session.class, SessionSpecResourceList.class).inNamespace(client.getNamespace());
    }

    @Provides
    @Singleton
    @Named(NAMESPACE)
    protected String getNamespace(final NamespacedKubernetesClient client) {
	return client.getNamespace();
    }
}
