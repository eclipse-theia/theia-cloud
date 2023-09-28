/********************************************************************************
 * Copyright (C) 2023 EclipseSource and others.
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
package org.eclipse.theia.cloud.common.k8s.resource.session.hub;

import org.eclipse.theia.cloud.common.k8s.resource.session.Session;

import io.fabric8.kubernetes.api.model.ObjectMeta;

public class SessionHub {

    private ObjectMeta metadata = new ObjectMeta();
    private SessionHubSpec spec;
    private SessionHubStatus status;

    public ObjectMeta getMetadata() {
	return metadata;
    }

    public void setMetadata(ObjectMeta metadata) {
	this.metadata = metadata;
    }

    public SessionHubSpec getSpec() {
	return spec;
    }

    public void setSpec(SessionHubSpec spec) {
	this.spec = spec;
    }

    public SessionHubStatus getStatus() {
	return status;
    }

    public void setStatus(SessionHubStatus status) {
	this.status = status;
    }

    public SessionHub(Session toHub) {
	this.setMetadata(toHub.getMetadata());
	this.spec = new SessionHubSpec(toHub.getSpec());
	if (toHub.getStatus() != null) {
	    this.status = new SessionHubStatus(toHub.getStatus());
	}
    }

    @SuppressWarnings("deprecation")
    public SessionHub(org.eclipse.theia.cloud.common.k8s.resource.session.v1beta5.SessionV1beta5 toHub) {
	this.setMetadata(toHub.getMetadata());
	this.spec = new SessionHubSpec(toHub.getSpec());
	if (toHub.getStatus() != null) {
	    this.status = new SessionHubStatus(toHub.getStatus());
	}
    }

}
