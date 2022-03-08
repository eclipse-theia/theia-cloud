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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.common.k8s.resource.WorkspaceSpecResourceList;
import org.eclipse.theia.cloud.operator.di.AbstractTheiaCloudOperatorModule;
import org.eclipse.theia.cloud.operator.handler.TemplateAddedHandler;
import org.eclipse.theia.cloud.operator.handler.WorkspaceAddedHandler;
import org.eclipse.theia.cloud.operator.resource.TemplateSpecResource;
import org.eclipse.theia.cloud.operator.resource.TemplateSpecResourceList;

import com.google.inject.Guice;
import com.google.inject.Injector;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public class TheiaCloudImpl implements TheiaCloud {

    private static final Logger LOGGER = LogManager.getLogger(TheiaCloudImpl.class);

    private static final String COR_ID_TEMPLATEPREFIX = "template-watch-";
    private static final String COR_ID_WORKSPACEPREFIX = "workspace-watch-";

    private final Map<String, TemplateSpecResource> templateCache = new ConcurrentHashMap<>();
    private final Map<String, Workspace> workspaceCache = new ConcurrentHashMap<>();

    private final String namespace;
    private final Injector injector;
    private final DefaultKubernetesClient client;
    private final NonNamespaceOperation<TemplateSpecResource, TemplateSpecResourceList, Resource<TemplateSpecResource>> templateResourceClient;
    private final NonNamespaceOperation<Workspace, WorkspaceSpecResourceList, Resource<Workspace>> workspaceResourceClient;

    private TemplateAddedHandler templateAddedHandler;
    private WorkspaceAddedHandler workspaceAddedHandler;

    public TheiaCloudImpl(String namespace, AbstractTheiaCloudOperatorModule module, DefaultKubernetesClient client,
	    NonNamespaceOperation<TemplateSpecResource, TemplateSpecResourceList, Resource<TemplateSpecResource>> templateResourceClient,
	    NonNamespaceOperation<Workspace, WorkspaceSpecResourceList, Resource<Workspace>> workspaceResourceClient) {
	this.namespace = namespace;
	this.injector = Guice.createInjector(module);
	this.client = client;
	this.templateResourceClient = templateResourceClient;
	this.workspaceResourceClient = workspaceResourceClient;

	this.templateAddedHandler = injector.getInstance(TemplateAddedHandler.class);
	this.workspaceAddedHandler = injector.getInstance(WorkspaceAddedHandler.class);
    }

    @Override
    public void start() {
	initTemplatesAndWatchForChanges();
	initWorkspacesAndWatchForChanges();
    }

    private void handleTemplateEvent(Watcher.Action action, String uid, String correlationId) {
	TemplateSpecResource template = templateCache.get(uid);
	switch (action) {
	case ADDED:
	    templateAdded(template, correlationId);
	    break;
	case DELETED:
	    templateDeleted(template, correlationId);
	    break;
	case MODIFIED:
	    templateModified(template, correlationId);
	    break;
	case ERROR:
	    templateErrored(template, correlationId);
	    break;
	case BOOKMARK:
	    templateBookmarked(template, correlationId);
	    break;
	}
    }

    private void templateAdded(TemplateSpecResource template, String correlationId) {
	LOGGER.trace(formatLogMessage(COR_ID_TEMPLATEPREFIX, correlationId,
		"Delegating templateAdded to " + templateAddedHandler.getClass().getName()));
	templateAddedHandler.handle(client, template, namespace, correlationId);
    }

    private void templateDeleted(TemplateSpecResource template, String correlationId) {
	// TODO
	LOGGER.warn(formatLogMessage(COR_ID_TEMPLATEPREFIX, correlationId, "templateDeleted not implemented"));
    }

    private void templateModified(TemplateSpecResource template, String correlationId) {
	// TODO
	LOGGER.warn(formatLogMessage(COR_ID_TEMPLATEPREFIX, correlationId, "templateModified not implemented"));
    }

    private void templateErrored(TemplateSpecResource template, String correlationId) {
	// TODO
	LOGGER.warn(formatLogMessage(COR_ID_TEMPLATEPREFIX, correlationId, "templateErrored not implemented"));
    }

    private void templateBookmarked(TemplateSpecResource template, String correlationId) {
	// TODO
	LOGGER.warn(formatLogMessage(COR_ID_TEMPLATEPREFIX, correlationId, "templateBookmarked not implemented"));
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
	// TODO
	LOGGER.warn(formatLogMessage(COR_ID_WORKSPACEPREFIX, correlationId, "workspaceDeleted not implemented"));
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

    private void initTemplatesAndWatchForChanges() {
	try {
	    /* init existing templates */
	    templateResourceClient.list().getItems().forEach(this::initTemplate);

	    /* watch for changes */
	    templateResourceClient.watch(new SpecWatch<TemplateSpecResource>(templateCache, this::handleTemplateEvent,
		    "Template", COR_ID_TEMPLATEPREFIX));
	} catch (Exception e) {
	    LOGGER.error(formatLogMessage(Main.COR_ID_INIT, "Error while initializing templates watch"), e);
	    System.exit(-1);
	}
    }

    private void initWorkspacesAndWatchForChanges() {
	try {
	    /* init existing workspaces */
	    workspaceResourceClient.list().getItems().forEach(this::initWorkspace);

	    /* watch for changes */
	    workspaceResourceClient.watch(new SpecWatch<Workspace>(workspaceCache,
		    this::handleWorkspaceEvent, "Workspace", COR_ID_WORKSPACEPREFIX));
	} catch (Exception e) {
	    LOGGER.error(formatLogMessage(Main.COR_ID_INIT, "Error while initializing workspace watch"), e);
	    System.exit(-1);
	}
    }

    private void initTemplate(TemplateSpecResource resource) {
	templateCache.put(resource.getMetadata().getUid(), resource);
	String uid = resource.getMetadata().getUid();
	handleTemplateEvent(Watcher.Action.ADDED, uid, Main.COR_ID_INIT);
    }

    private void initWorkspace(Workspace resource) {
	workspaceCache.put(resource.getMetadata().getUid(), resource);
	String uid = resource.getMetadata().getUid();
	handleWorkspaceEvent(Watcher.Action.ADDED, uid, Main.COR_ID_INIT);
    }

}
