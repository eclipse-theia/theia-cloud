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
package org.eclipse.theia.cloud.service;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.theia.cloud.common.k8s.resource.Session;
import org.eclipse.theia.cloud.common.k8s.resource.SessionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.SessionSpecResourceList;
import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

public final class K8sUtil {

    private static final Logger LOGGER = Logger.getLogger(K8sUtil.class);

    private static final String COR_ID_INIT = "init";

    private static String NAMESPACE = "";
    private static DefaultKubernetesClient CLIENT = createClient();

    private K8sUtil() {
    }

    private static DefaultKubernetesClient createClient() {
	Config config = new ConfigBuilder().build();

	/* don't close resource */
	DefaultKubernetesClient client = new DefaultKubernetesClient(config);

	String namespace = client.getNamespace();
	K8sUtil.NAMESPACE = namespace;
	LOGGER.info(formatLogMessage(COR_ID_INIT, "Namespace: " + namespace));

	String sessionAPIVersion = HasMetadata.getApiVersion(Session.class);
	LOGGER.info(formatLogMessage(COR_ID_INIT, "Registering Session in version " + sessionAPIVersion));
	KubernetesDeserializer.registerCustomKind(sessionAPIVersion, SessionSpec.KIND, Session.class);

	return client;
    }

    public static Reply launchSession(String correlationId, String name, String appDefinition, String user) {

	NonNamespaceOperation<Session, SessionSpecResourceList, Resource<Session>> sessions = CLIENT
		.customResources(Session.class, SessionSpecResourceList.class).inNamespace(K8sUtil.NAMESPACE);

	String createSpecName;
	Resource<Session> existingSession = sessions.withName(name);
	SessionSpec existingSessionSpec = null;
	if (existingSession.get() == null) {
	    Session sessionSpecResource = new Session();

	    ObjectMeta metadata = new ObjectMeta();
	    sessionSpecResource.setMetadata(metadata);
	    metadata.setName(name);

	    SessionSpec sessionSpec = new SessionSpec(name, appDefinition, user);
	    sessionSpecResource.setSpec(sessionSpec);

	    Session created = sessions.create(sessionSpecResource);
	    createSpecName = created.getSpec().getName();
	} else {
	    createSpecName = existingSession.get().getSpec().getName();
	    existingSessionSpec = existingSession.get().getSpec();
	}

	AtomicReference<String> atomicReferenceURL = new AtomicReference<String>(null);
	AtomicReference<String> atomicReferenceError = new AtomicReference<String>(null);
	CountDownLatch latch = new CountDownLatch(1);

	Watch watch = null;
	if (existingSessionSpec != null
		&& ((existingSessionSpec.getUrl() != null && !existingSessionSpec.getUrl().isBlank())
			|| (existingSessionSpec.getError() != null && !existingSessionSpec.getError().isBlank()))) {
	    LOGGER.info(formatLogMessage(correlationId, "Session existing with result"));
	    if (existingSessionSpec.getUrl() != null && !existingSessionSpec.getUrl().isBlank()) {
		atomicReferenceURL.set(existingSessionSpec.getUrl());
		latch.countDown();
	    }
	    if (existingSessionSpec.getError() != null && !existingSessionSpec.getError().isBlank()) {
		atomicReferenceError.set(existingSessionSpec.getError());
		sessions.withName(name).delete();
		latch.countDown();
	    }
	} else {
	    watch = sessions.watch(new Watcher<Session>() {

		@Override
		public void eventReceived(Action action, Session resource) {
		    LOGGER.trace(
			    formatLogMessage(correlationId, "Received session event " + action + " for " + resource));
		    if (createSpecName.equals(resource.getSpec().getName())) {
			if (resource.getSpec().getUrl() != null && !resource.getSpec().getUrl().isBlank()) {
			    LOGGER.info(formatLogMessage(correlationId, "Received URL for " + resource));
			    atomicReferenceURL.set(resource.getSpec().getUrl());
			    latch.countDown();
			} else if (resource.getSpec().getError() != null && !resource.getSpec().getError().isBlank()) {
			    LOGGER.info(formatLogMessage(correlationId,
				    "Received Error for " + resource + ". Deleting session again."));
			    atomicReferenceError.set(resource.getSpec().getError());
			    sessions.withName(name).delete();
			    latch.countDown();
			}
		    }
		}

		@Override
		public void onClose(WatcherException cause) {
		}

	    });
	}

	try {
	    latch.await(1, TimeUnit.MINUTES);
	} catch (InterruptedException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Timeout while waiting for URL for " + name), e);
	    return new Reply(false, "", "Unable to start session");
	} finally {
	    if (watch != null) {
		watch.close();
	    }
	}

	return new Reply(atomicReferenceURL.get() != null, atomicReferenceURL.get(), atomicReferenceError.get());

    }

    public static void reportActivity(String correlationId, String name, String appDefinition, String user) {
	NonNamespaceOperation<Session, SessionSpecResourceList, Resource<Session>> sessions = CLIENT
		.customResources(Session.class, SessionSpecResourceList.class).inNamespace(K8sUtil.NAMESPACE);
	Resource<Session> existingSession = sessions.withName(name);
	if (existingSession.get() == null) {
	    LOGGER.info(formatLogMessage(correlationId, "Activity reported for session {" + name + " - " + appDefinition
		    + " - " + user + "} but session not found"));
	    return;
	}
	existingSession.edit(session -> {
	    session.getSpec().setLastActivity((int) System.currentTimeMillis());
	    LOGGER.trace(formatLogMessage(correlationId,
		    "updating activity for session {" + name + " - " + appDefinition + " - " + user + "}"));
	    return session;
	});
    }

}
