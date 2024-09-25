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
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionSpecResourceList;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionStatus;
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

        spec.setSessionSecret(UUID.randomUUID().toString());

        ObjectMeta metadata = new ObjectMeta();
        metadata.setName(spec.getName());
        session.setMetadata(metadata);

        info(correlationId, "Create Session " + session.getSpec());
        return operation().resource(session).create();
    }

    @Override
    public Session launch(String correlationId, SessionSpec spec, long timeout, TimeUnit unit) {
        // get or create session
        Session session = get(spec.getName()).orElseGet(() -> create(correlationId, spec));
        SessionStatus sessionStatus = session.getNonNullStatus();

        // if session is available and has already an url or error, return that session
        if (sessionStatus.hasUrl()) {
            return session;
        }
        if (sessionStatus.hasError()) {
            delete(correlationId, session.getSpec().getName());
            return session;
        }
        // wait for session url or error to be available
        try {
            String sessionName = session.getSpec().getName();
            watchUntil((action, changedSession) -> isSessionComplete(correlationId, sessionName, changedSession),
                    timeout, unit);
            // Workaround to get the last changedSession from the watchUntil call above
            session = get(spec.getName()).orElseGet(() -> create(correlationId, spec));
        } catch (InterruptedException exception) {
            error(correlationId, "Timeout while waiting for URL for " + session.getSpec().getName(), exception);
            session = updateStatus(correlationId, session,
                    status -> status.setError(TheiaCloudError.SESSION_LAUNCH_TIMEOUT));
            return session;
        }
        return session;
    }

    protected boolean isSessionComplete(String correlationId, String sessionName, Session changedSession) {
        if (sessionName.equals(changedSession.getSpec().getName())) {
            if (changedSession.getNonNullStatus().hasUrl()) {
                info(correlationId, "Received URL for " + changedSession);
                return true;
            }
            if (changedSession.getNonNullStatus().hasError()) {
                info(correlationId, "Received Error for " + changedSession + ". Deleting session again.");
                delete(correlationId, sessionName);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean reportActivity(String correlationId, String name) {
        return edit(correlationId, name, session -> {
            trace(correlationId, "Updating activity for session {" + name + "}");
            updateStatus(correlationId, session, status -> status.setLastActivity(Instant.now().toEpochMilli()));
        }) != null;
    }

    @Override
    public SessionStatus createDefaultStatus() {
        return new SessionStatus();
    }

}
