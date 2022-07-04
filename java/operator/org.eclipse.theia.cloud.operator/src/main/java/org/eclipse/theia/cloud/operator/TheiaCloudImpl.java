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
import org.eclipse.theia.cloud.common.k8s.resource.Session;
import org.eclipse.theia.cloud.common.k8s.resource.SessionSpecResourceList;
import org.eclipse.theia.cloud.operator.di.AbstractTheiaCloudOperatorModule;
import org.eclipse.theia.cloud.operator.handler.AppDefinitionAddedHandler;
import org.eclipse.theia.cloud.operator.handler.SessionAddedHandler;
import org.eclipse.theia.cloud.operator.handler.SessionRemovedHandler;
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
    private static final String COR_ID_SESSIONPREFIX = "session-watch-";

    private final Map<String, AppDefinitionSpecResource> appDefinitionCache = new ConcurrentHashMap<>();
    private final Map<String, Session> sessionCache = new ConcurrentHashMap<>();

    private final String namespace;
    private final Injector injector;
    private final DefaultKubernetesClient client;
    private final NonNamespaceOperation<AppDefinitionSpecResource, AppDefinitionSpecResourceList, Resource<AppDefinitionSpecResource>> appDefinitionResourceClient;
    private final NonNamespaceOperation<Session, SessionSpecResourceList, Resource<Session>> sessionResourceClient;

    private AppDefinitionAddedHandler appDefinitionAddedHandler;
    private SessionAddedHandler sessionAddedHandler;
    private SessionRemovedHandler sessionRemovedHandler;

    public TheiaCloudImpl(String namespace, AbstractTheiaCloudOperatorModule module, DefaultKubernetesClient client,
	    NonNamespaceOperation<AppDefinitionSpecResource, AppDefinitionSpecResourceList, Resource<AppDefinitionSpecResource>> appDefinitionResourceClient,
	    NonNamespaceOperation<Session, SessionSpecResourceList, Resource<Session>> sessionResourceClient) {
	this.namespace = namespace;
	this.injector = Guice.createInjector(module);
	this.client = client;
	this.appDefinitionResourceClient = appDefinitionResourceClient;
	this.sessionResourceClient = sessionResourceClient;

	this.appDefinitionAddedHandler = injector.getInstance(AppDefinitionAddedHandler.class);
	this.sessionAddedHandler = injector.getInstance(SessionAddedHandler.class);
	this.sessionRemovedHandler = injector.getInstance(SessionRemovedHandler.class);
    }

    @Override
    public void start() {
	initAppDefinitionsAndWatchForChanges();
	initSessionsAndWatchForChanges();

	EXECUTOR.scheduleWithFixedDelay(new KillAfterRunnable(appDefinitionResourceClient, sessionResourceClient), 1, 1,
		TimeUnit.MINUTES);
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

    private void handleSessionEvent(Watcher.Action action, String uid, String correlationId) {
	Session session = sessionCache.get(uid);
	switch (action) {
	case ADDED:
	    sessionAdded(session, correlationId);
	    break;
	case DELETED:
	    sessionDeleted(session, correlationId);
	    break;
	case MODIFIED:
	    sessionModified(session, correlationId);
	    break;
	case ERROR:
	    sessionErrored(session, correlationId);
	    break;
	case BOOKMARK:
	    sessionBookmarked(session, correlationId);
	    break;
	}
    }

    private void sessionAdded(Session session, String correlationId) {
	LOGGER.trace(formatLogMessage(COR_ID_SESSIONPREFIX, correlationId,
		"Delegating sessionAdded to " + sessionAddedHandler.getClass().getName()));
	sessionAddedHandler.handle(client, session, namespace, correlationId);
    }

    private void sessionDeleted(Session session, String correlationId) {
	LOGGER.trace(formatLogMessage(COR_ID_SESSIONPREFIX, correlationId,
		"Delegating sessionDeleted to " + sessionRemovedHandler.getClass().getName()));
	sessionRemovedHandler.handle(client, session, namespace, correlationId);
    }

    private void sessionModified(Session session, String correlationId) {
	// TODO
	LOGGER.warn(formatLogMessage(COR_ID_SESSIONPREFIX, correlationId, "sessionModified not implemented"));
    }

    private void sessionErrored(Session session, String correlationId) {
	// TODO
	LOGGER.warn(formatLogMessage(COR_ID_SESSIONPREFIX, correlationId, "sessionErrored not implemented"));
    }

    private void sessionBookmarked(Session session, String correlationId) {
	// TODO
	LOGGER.warn(formatLogMessage(COR_ID_SESSIONPREFIX, correlationId, "sessionBookmarked not implemented"));
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

    private void initSessionsAndWatchForChanges() {
	try {
	    /* init existing sessions */
	    sessionResourceClient.list().getItems().forEach(this::initSession);

	    /* watch for changes */
	    sessionResourceClient.watch(
		    new SpecWatch<Session>(sessionCache, this::handleSessionEvent, "Session", COR_ID_SESSIONPREFIX));
	} catch (Exception e) {
	    LOGGER.error(formatLogMessage(Main.COR_ID_INIT, "Error while initializing session watch"), e);
	    System.exit(-1);
	}
    }

    private void initAppDefinition(AppDefinitionSpecResource resource) {
	appDefinitionCache.put(resource.getMetadata().getUid(), resource);
	String uid = resource.getMetadata().getUid();
	handleAppDefnitionEvent(Watcher.Action.ADDED, uid, Main.COR_ID_INIT);
    }

    private void initSession(Session resource) {
	sessionCache.put(resource.getMetadata().getUid(), resource);
	String uid = resource.getMetadata().getUid();
	handleSessionEvent(Watcher.Action.ADDED, uid, Main.COR_ID_INIT);
    }

}
