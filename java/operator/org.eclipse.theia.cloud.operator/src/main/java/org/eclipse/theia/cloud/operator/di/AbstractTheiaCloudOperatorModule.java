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

import java.util.function.Consumer;

import org.eclipse.theia.cloud.common.k8s.client.DefaultTheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.Workspace;
import org.eclipse.theia.cloud.common.util.CustomResourceUtil;
import org.eclipse.theia.cloud.operator.TheiaCloudOperator;
import org.eclipse.theia.cloud.operator.TheiaCloudOperatorArguments;
import org.eclipse.theia.cloud.operator.bandwidth.BandwidthLimiter;
import org.eclipse.theia.cloud.operator.bandwidth.BandwidthLimiterImpl;
import org.eclipse.theia.cloud.operator.handler.appdef.AppDefinitionHandler;
import org.eclipse.theia.cloud.operator.handler.appdef.EagerStartAppDefinitionAddedHandler;
import org.eclipse.theia.cloud.operator.handler.appdef.LazyStartAppDefinitionHandler;
import org.eclipse.theia.cloud.operator.handler.session.EagerSessionHandler;
import org.eclipse.theia.cloud.operator.handler.session.LazySessionHandler;
import org.eclipse.theia.cloud.operator.handler.session.SessionHandler;
import org.eclipse.theia.cloud.operator.handler.ws.LazyWorkspaceHandler;
import org.eclipse.theia.cloud.operator.handler.ws.WorkspaceHandler;
import org.eclipse.theia.cloud.operator.ingress.IngressPathProvider;
import org.eclipse.theia.cloud.operator.ingress.IngressPathProviderImpl;
import org.eclipse.theia.cloud.operator.messaging.MonitorMessagingService;
import org.eclipse.theia.cloud.operator.messaging.MonitorMessagingServiceImpl;
import org.eclipse.theia.cloud.operator.plugins.MonitorActivityTracker;
import org.eclipse.theia.cloud.operator.plugins.OperatorPlugin;
import org.eclipse.theia.cloud.operator.pv.DefaultPersistentVolumeCreator;
import org.eclipse.theia.cloud.operator.pv.MinikubePersistentVolumeCreator;
import org.eclipse.theia.cloud.operator.pv.PersistentVolumeCreator;
import org.eclipse.theia.cloud.operator.replacements.DefaultDeploymentTemplateReplacements;
import org.eclipse.theia.cloud.operator.replacements.DefaultPersistentVolumeTemplateReplacements;
import org.eclipse.theia.cloud.operator.replacements.DeploymentTemplateReplacements;
import org.eclipse.theia.cloud.operator.replacements.PersistentVolumeTemplateReplacements;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.fabric8.kubernetes.client.NamespacedKubernetesClient;

public abstract class AbstractTheiaCloudOperatorModule extends AbstractModule {

    protected TheiaCloudOperatorArguments arguments;

    public AbstractTheiaCloudOperatorModule(TheiaCloudOperatorArguments arguments) {
        this.arguments = arguments;
    }

    @Override
    protected void configure() {
        bind(TheiaCloudOperator.class).to(bindTheiaCloudOperator()).in(Singleton.class);

        bind(BandwidthLimiter.class).to(bindBandwidthLimiter()).in(Singleton.class);
        bind(PersistentVolumeCreator.class).to(bindPersistentVolumeHandler()).in(Singleton.class);
        bind(IngressPathProvider.class).to(bindIngressPathProvider()).in(Singleton.class);
        bind(DeploymentTemplateReplacements.class).to(bindDeploymentTemplateReplacements()).in(Singleton.class);
        bind(PersistentVolumeTemplateReplacements.class).to(bindPersistentVolumeTemplateReplacements())
                .in(Singleton.class);

        bind(AppDefinitionHandler.class).to(bindAppDefinitionHandler()).in(Singleton.class);
        bind(SessionHandler.class).to(bindSessionHandler()).in(Singleton.class);
        bind(WorkspaceHandler.class).to(bindWorkspaceHandler()).in(Singleton.class);

        configure(MultiBinding.create(OperatorPlugin.class), this::bindOperatorPlugins);
        bind(MonitorMessagingService.class).to(bindMonitorMessagingService()).in(Singleton.class);
        bind(TheiaCloudOperatorArguments.class).toInstance(arguments);
    }

    protected <T> void configure(final MultiBinding<T> binding, final Consumer<MultiBinding<T>> configurator) {
        configurator.accept(binding);
        binding.applyBinding(binder());
    }

    protected abstract Class<? extends TheiaCloudOperator> bindTheiaCloudOperator();

    protected Class<? extends BandwidthLimiter> bindBandwidthLimiter() {
        return BandwidthLimiterImpl.class;
    }

    protected Class<? extends PersistentVolumeCreator> bindPersistentVolumeHandler() {
        switch (arguments.getCloudProvider()) {
        case MINIKUBE:
            return MinikubePersistentVolumeCreator.class;
        case K8S:
        default:
            return DefaultPersistentVolumeCreator.class;
        }
    }

    protected Class<? extends IngressPathProvider> bindIngressPathProvider() {
        return IngressPathProviderImpl.class;
    }

    protected Class<? extends DeploymentTemplateReplacements> bindDeploymentTemplateReplacements() {
        return DefaultDeploymentTemplateReplacements.class;
    }

    protected void bindOperatorPlugins(final MultiBinding<OperatorPlugin> binding) {
        bindMonitorActivityTracker(binding);
    }

    protected void bindMonitorActivityTracker(final MultiBinding<OperatorPlugin> binding) {
        binding.add(MonitorActivityTracker.class);
    }

    protected Class<? extends MonitorMessagingService> bindMonitorMessagingService() {
        return MonitorMessagingServiceImpl.class;
    }

    protected Class<? extends PersistentVolumeTemplateReplacements> bindPersistentVolumeTemplateReplacements() {
        return DefaultPersistentVolumeTemplateReplacements.class;
    }

    protected Class<? extends AppDefinitionHandler> bindAppDefinitionHandler() {
        if (arguments.isEagerStart()) {
            return EagerStartAppDefinitionAddedHandler.class;
        } else {
            return LazyStartAppDefinitionHandler.class;
        }
    }

    protected Class<? extends WorkspaceHandler> bindWorkspaceHandler() {
        return LazyWorkspaceHandler.class;
    }

    protected Class<? extends SessionHandler> bindSessionHandler() {
        if (arguments.isEagerStart()) {
            return EagerSessionHandler.class;
        } else {
            return LazySessionHandler.class;
        }
    }

    @Provides
    @Singleton
    protected NamespacedKubernetesClient provideKubernetesClient() {
        NamespacedKubernetesClient client = CustomResourceUtil.createClient();
        CustomResourceUtil.validateCustomResource(client, Session.CRD_NAME);
        CustomResourceUtil.validateCustomResource(client, Workspace.CRD_NAME);
        CustomResourceUtil.validateCustomResource(client, AppDefinition.CRD_NAME);
        return client;
    }

    @Provides
    @Singleton
    protected TheiaCloudClient provideTheiaCloudClient(final NamespacedKubernetesClient client) {
        return new DefaultTheiaCloudClient(client);
    }
}
