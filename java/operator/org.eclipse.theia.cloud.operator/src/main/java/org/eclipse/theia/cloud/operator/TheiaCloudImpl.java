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
package org.eclipse.theia.cloud.operator;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.common.k8s.resource.WorkspaceSpecResourceList;
import org.eclipse.theia.cloud.operator.di.AbstractTheiaCloudOperatorModule;
import org.eclipse.theia.cloud.operator.handler.AppDefinitionAddedHandler;
import org.eclipse.theia.cloud.operator.handler.WorkspaceAddedHandler;
import org.eclipse.theia.cloud.operator.handler.WorkspaceRemovedHandler;
import org.eclipse.theia.cloud.operator.resource.AppDefinitionSpecResource;
import org.eclipse.theia.cloud.operator.resource.AppDefinitionSpecResourceList;

import com.google.inject.Guice;
import com.google.inject.Injector;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public class TheiaCloudImpl implements TheiaCloud {

    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    private static final Logger LOGGER = LogManager.getLogger(TheiaCloudImpl.class);

    private static final String COR_ID_APPDEFINITIONPREFIX = "appdefinition-watch-";
    private static final String COR_ID_WORKSPACEPREFIX = "workspace-watch-";

    private final Map<String, AppDefinitionSpecResource> appDefinitionCache = new ConcurrentHashMap<>();
    private final Map<String, Workspace> workspaceCache = new ConcurrentHashMap<>();

    private final String namespace;
    private final Injector injector;
    private final DefaultKubernetesClient client;
    private final NonNamespaceOperation<AppDefinitionSpecResource, AppDefinitionSpecResourceList, Resource<AppDefinitionSpecResource>> appDefinitionResourceClient;
    private final NonNamespaceOperation<Workspace, WorkspaceSpecResourceList, Resource<Workspace>> workspaceResourceClient;

    private AppDefinitionAddedHandler appDefinitionAddedHandler;
    private WorkspaceAddedHandler workspaceAddedHandler;
    private WorkspaceRemovedHandler workspaceRemovedHandler;

    public TheiaCloudImpl(String namespace, AbstractTheiaCloudOperatorModule module, DefaultKubernetesClient client,
	    NonNamespaceOperation<AppDefinitionSpecResource, AppDefinitionSpecResourceList, Resource<AppDefinitionSpecResource>> appDefinitionResourceClient,
	    NonNamespaceOperation<Workspace, WorkspaceSpecResourceList, Resource<Workspace>> workspaceResourceClient) {
	this.namespace = namespace;
	this.injector = Guice.createInjector(module);
	this.client = client;
	this.appDefinitionResourceClient = appDefinitionResourceClient;
	this.workspaceResourceClient = workspaceResourceClient;

	this.appDefinitionAddedHandler = injector.getInstance(AppDefinitionAddedHandler.class);
	this.workspaceAddedHandler = injector.getInstance(WorkspaceAddedHandler.class);
	this.workspaceRemovedHandler = injector.getInstance(WorkspaceRemovedHandler.class);
    }

    @Override
    public void start() {
	initAppDefinitionsAndWatchForChanges();
	initWorkspacesAndWatchForChanges();

	EXECUTOR.scheduleWithFixedDelay(new KillAfterRunnable(appDefinitionResourceClient, workspaceResourceClient), 1,
		1, TimeUnit.MINUTES);
    }

    private void handleAppDefnitionEvent(Watcher.Action action, String uid, String correlationId) {
	AppDefinitionSpecResource appDefinition = appDefinitionCache.get(uid);
	switch (action) {
	case ADDED:
	    appDefinitionAdded(appDefinition, correlationId);
	    break;
	case DELETED:
	    appDefinitionDeleted(appDefinition, correlationId);
	    break;
	case MODIFIED:
	    appDefinitionModified(appDefinition, correlationId);
	    break;
	case ERROR:
	    appDefinitionErrored(appDefinition, correlationId);
	    break;
	case BOOKMARK:
	    appDefinitionBookmarked(appDefinition, correlationId);
	    break;
	}
    }

    private void appDefinitionAdded(AppDefinitionSpecResource appDefinition, String correlationId) {
	LOGGER.trace(formatLogMessage(COR_ID_APPDEFINITIONPREFIX, correlationId,
		"Delegating appDefinitionAdded to " + appDefinitionAddedHandler.getClass().getName()));
	appDefinitionAddedHandler.handle(client, appDefinition, namespace, correlationId);
    }

    private void appDefinitionDeleted(AppDefinitionSpecResource appDefinition, String correlationId) {
	// TODO
	LOGGER.warn(
		formatLogMessage(COR_ID_APPDEFINITIONPREFIX, correlationId, "appDefinitionDeleted not implemented"));
    }

    private void appDefinitionModified(AppDefinitionSpecResource appDefinition, String correlationId) {
	// TODO
	LOGGER.warn(
		formatLogMessage(COR_ID_APPDEFINITIONPREFIX, correlationId, "appDefinitionModified not implemented"));
    }

    private void appDefinitionErrored(AppDefinitionSpecResource appDefinition, String correlationId) {
	// TODO
	LOGGER.warn(
		formatLogMessage(COR_ID_APPDEFINITIONPREFIX, correlationId, "appDefinitionErrored not implemented"));
    }

    private void appDefinitionBookmarked(AppDefinitionSpecResource appDefinition, String correlationId) {
	// TODO
	LOGGER.warn(
		formatLogMessage(COR_ID_APPDEFINITIONPREFIX, correlationId, "appDefinitionBookmarked not implemented"));
    }

    private void handleWorkspaceEvent(Watcher.Action action, String uid, String correlationId) {
	Workspace workspace = workspaceCache.get(uid);
	switch (action) {
	case ADDED:
	    workspaceAdded(workspace, correlationId);
	    break;
	case DELETED:
	    workspaceDeleted(workspace, correlationId);
	    break;
	case MODIFIED:
	    workspaceModified(workspace, correlationId);
	    break;
	case ERROR:
	    workspaceErrored(workspace, correlationId);
	    break;
	case BOOKMARK:
	    workspaceBookmarked(workspace, correlationId);
	    break;
	}
    }

    private void workspaceAdded(Workspace workspace, String correlationId) {
	LOGGER.trace(formatLogMessage(COR_ID_WORKSPACEPREFIX, correlationId,
		"Delegating workspaceAdded to " + workspaceAddedHandler.getClass().getName()));
	workspaceAddedHandler.handle(client, workspace, namespace, correlationId);
    }

    private void workspaceDeleted(Workspace workspace, String correlationId) {
	LOGGER.trace(formatLogMessage(COR_ID_WORKSPACEPREFIX, correlationId,
		"Delegating workspaceDeleted to " + workspaceRemovedHandler.getClass().getName()));
	workspaceRemovedHandler.handle(client, workspace, namespace, correlationId);
    }

    private void workspaceModified(Workspace workspace, String correlationId) {
	// TODO
	LOGGER.warn(formatLogMessage(COR_ID_WORKSPACEPREFIX, correlationId, "workspaceModified not implemented"));
    }

    private void workspaceErrored(Workspace workspace, String correlationId) {
	// TODO
	LOGGER.warn(formatLogMessage(COR_ID_WORKSPACEPREFIX, correlationId, "workspaceErrored not implemented"));
    }

    private void workspaceBookmarked(Workspace workspace, String correlationId) {
	// TODO
	LOGGER.warn(formatLogMessage(COR_ID_WORKSPACEPREFIX, correlationId, "workspaceBookmarked not implemented"));
    }

    private void initAppDefinitionsAndWatchForChanges() {
	try {
	    /* init existing app definitions */
	    appDefinitionResourceClient.list().getItems().forEach(this::initAppDefinition);

	    /* watch for changes */
	    appDefinitionResourceClient.watch(new SpecWatch<AppDefinitionSpecResource>(appDefinitionCache,
		    this::handleAppDefnitionEvent, "App Definition", COR_ID_APPDEFINITIONPREFIX));
	} catch (Exception e) {
	    LOGGER.error(formatLogMessage(Main.COR_ID_INIT, "Error while initializing app definitions watch"), e);
	    System.exit(-1);
	}
    }

    private void initWorkspacesAndWatchForChanges() {
	try {
	    /* init existing workspaces */
	    workspaceResourceClient.list().getItems().forEach(this::initWorkspace);

	    /* watch for changes */
	    workspaceResourceClient.watch(new SpecWatch<Workspace>(workspaceCache, this::handleWorkspaceEvent,
		    "Workspace", COR_ID_WORKSPACEPREFIX));
	} catch (Exception e) {
	    LOGGER.error(formatLogMessage(Main.COR_ID_INIT, "Error while initializing workspace watch"), e);
	    System.exit(-1);
	}
    }

    private void initAppDefinition(AppDefinitionSpecResource resource) {
	appDefinitionCache.put(resource.getMetadata().getUid(), resource);
	String uid = resource.getMetadata().getUid();
	handleAppDefnitionEvent(Watcher.Action.ADDED, uid, Main.COR_ID_INIT);
    }

    private void initWorkspace(Workspace resource) {
	workspaceCache.put(resource.getMetadata().getUid(), resource);
	String uid = resource.getMetadata().getUid();
	handleWorkspaceEvent(Watcher.Action.ADDED, uid, Main.COR_ID_INIT);
    }

}
