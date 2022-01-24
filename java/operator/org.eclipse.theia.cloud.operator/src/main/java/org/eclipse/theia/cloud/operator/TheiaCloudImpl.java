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

import static org.eclipse.theia.cloud.operator.util.LogMessageUtil.formatLogMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.operator.resource.TemplateSpecResource;
import org.eclipse.theia.cloud.operator.resource.TemplateSpecResourceList;
import org.eclipse.theia.cloud.operator.resource.WorkspaceSpecResource;
import org.eclipse.theia.cloud.operator.resource.WorkspaceSpecResourceList;

import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public class TheiaCloudImpl implements TheiaCloud {

    private static final Logger LOGGER = LogManager.getLogger(TheiaCloudImpl.class);

    private static final String COR_ID_TEMPLATEPREFIX = "template-watch-";
    private static final String COR_ID_WORKSPACEPREFIX = "workspace-watch-";

    private final Map<String, TemplateSpecResource> templateCache = new ConcurrentHashMap<>();
    private final Map<String, WorkspaceSpecResource> workspaceCache = new ConcurrentHashMap<>();

    @SuppressWarnings("unused")
    private final String namespace;
    private final NonNamespaceOperation<TemplateSpecResource, TemplateSpecResourceList, Resource<TemplateSpecResource>> templateResourceClient;
    private final NonNamespaceOperation<WorkspaceSpecResource, WorkspaceSpecResourceList, Resource<WorkspaceSpecResource>> workspaceResourceClient;

    public TheiaCloudImpl(String namespace,
	    NonNamespaceOperation<TemplateSpecResource, TemplateSpecResourceList, Resource<TemplateSpecResource>> templateResourceClient,
	    NonNamespaceOperation<WorkspaceSpecResource, WorkspaceSpecResourceList, Resource<WorkspaceSpecResource>> workspaceResourceClient) {
	this.namespace = namespace;
	this.templateResourceClient = templateResourceClient;
	this.workspaceResourceClient = workspaceResourceClient;
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
	// TODO
	LOGGER.warn(formatLogMessage(COR_ID_TEMPLATEPREFIX, correlationId, "templateAdded not implemented"));
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
	WorkspaceSpecResource workspace = workspaceCache.get(uid);
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

    private void workspaceAdded(WorkspaceSpecResource workspace, String correlationId) {
	// TODO
	LOGGER.warn(formatLogMessage(COR_ID_WORKSPACEPREFIX, correlationId, "workspaceAdded not implemented"));
    }

    private void workspaceDeleted(WorkspaceSpecResource workspace, String correlationId) {
	// TODO
	LOGGER.warn(formatLogMessage(COR_ID_WORKSPACEPREFIX, correlationId, "workspaceDeleted not implemented"));
    }

    private void workspaceModified(WorkspaceSpecResource workspace, String correlationId) {
	// TODO
	LOGGER.warn(formatLogMessage(COR_ID_WORKSPACEPREFIX, correlationId, "workspaceModified not implemented"));
    }

    private void workspaceErrored(WorkspaceSpecResource workspace, String correlationId) {
	// TODO
	LOGGER.warn(formatLogMessage(COR_ID_WORKSPACEPREFIX, correlationId, "workspaceErrored not implemented"));
    }

    private void workspaceBookmarked(WorkspaceSpecResource workspace, String correlationId) {
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
	    workspaceResourceClient.watch(new SpecWatch<WorkspaceSpecResource>(workspaceCache,
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

    private void initWorkspace(WorkspaceSpecResource resource) {
	workspaceCache.put(resource.getMetadata().getUid(), resource);
	String uid = resource.getMetadata().getUid();
	handleWorkspaceEvent(Watcher.Action.ADDED, uid, Main.COR_ID_INIT);
    }

}
