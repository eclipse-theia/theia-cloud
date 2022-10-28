/********************************************************************************
 * Copyright (C) 2022 EclipseSource and others.
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
package org.eclipse.theia.cloud.common.k8s.client;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.eclipse.theia.cloud.common.k8s.resource.Session;
import org.eclipse.theia.cloud.common.k8s.resource.SessionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.SessionSpecResourceList;
import org.eclipse.theia.cloud.common.util.TheiaCloudError;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;

public class DefaultSessionResourceClient extends BaseResourceClient<Session, SessionSpecResourceList>
	implements SessionResourceClient {

    protected NamespacedKubernetesClient client;

    public DefaultSessionResourceClient(NamespacedKubernetesClient client) {
	super(client, Session.class, SessionSpecResourceList.class);
    }

    @Override
    public Session create(String correlationId, SessionSpec spec) {
	Session session = new Session();
	session.setSpec(spec);
	spec.setLastActivity(Instant.now().toEpochMilli());

	ObjectMeta metadata = new ObjectMeta();
	metadata.setName(spec.getName());
	session.setMetadata(metadata);

	info(correlationId, "Create Session " + session.getSpec());
	// TODO ES validate before creating
	return operation().create(session);
    }

    @Override
    public Session launch(String correlationId, SessionSpec spec, long timeout, TimeUnit unit) {
	// get or create session
	Session session = get(spec.getName()).orElseGet(() -> create(correlationId, spec));
	SessionSpec sessionSpec = session.getSpec();

	// if session is available and has already an url or error, return that session
	if (sessionSpec.hasUrl()) {
	    return session;
	}
	if (sessionSpec.hasError()) {
	    delete(correlationId, spec.getName());
	    return session;
	}

	// wait for session url or error to be available
	try {
	    watchUntil((action, changedSession) -> isSessionComplete(correlationId, sessionSpec, changedSession),
		    timeout, unit);
	} catch (InterruptedException exception) {
	    error(correlationId, "Timeout while waiting for URL for " + spec.getName(), exception);
	    sessionSpec.setError(TheiaCloudError.SESSION_LAUNCH_TIMEOUT);
	}
	return session;
    }

    protected boolean isSessionComplete(String correlationId, SessionSpec sessionSpec, Session changedSession) {
	if (sessionSpec.getName().equals(changedSession.getSpec().getName())) {
	    if (changedSession.getSpec().hasUrl()) {
		info(correlationId, "Received URL for " + changedSession);
		sessionSpec.setUrl(changedSession.getSpec().getUrl());
		return true;
	    }
	    if (changedSession.getSpec().hasError()) {
		info(correlationId, "Received Error for " + changedSession + ". Deleting session again.");
		delete(correlationId, sessionSpec.getName());
		sessionSpec.setError(changedSession.getSpec().getError());
		return true;
	    }
	}
	return false;
    }

    @Override
    public boolean reportActivity(String correlationId, String name) {
	return edit(correlationId, name, session -> {
	    trace(correlationId, "Updating activity for session {" + name + "}");
	    session.getSpec().setLastActivity(Instant.now().toEpochMilli());
	}) != null;
    }

}
