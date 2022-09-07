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
package org.eclipse.theia.cloud.service;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.theia.cloud.common.k8s.resource.Session;
import org.eclipse.theia.cloud.common.k8s.resource.SessionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.common.k8s.resource.WorkspaceSpec;
import org.eclipse.theia.cloud.common.util.TheiaCloudError;

public class TheiaCloudWebException extends WebApplicationException {
    private static final long serialVersionUID = -4151261201767478256L;

    public TheiaCloudWebException(Response response) {
	super(response);
    }

    public TheiaCloudWebException(Response.Status status) {
	super(status);
    }

    public TheiaCloudWebException(TheiaCloudError status) {
	this(Response.status(status.getCode(), status.getReason()).entity(status).type(MediaType.APPLICATION_JSON_TYPE)
		.build());
    }

    public TheiaCloudWebException(String error) {
	this(TheiaCloudError.fromString(error));
    }

    public static Session throwIfErroneous(Session session) {
	throwIfErroneous(session.getSpec());
	return session;
    }

    public static SessionSpec throwIfErroneous(SessionSpec spec) {
	if (spec.hasError()) {
	    throw new TheiaCloudWebException(TheiaCloudError.fromString(spec.getError()));
	}
	return spec;
    }

    public static Workspace throwIfErroneous(Workspace workspace) {
	throwIfErroneous(workspace.getSpec());
	return workspace;
    }

    public static WorkspaceSpec throwIfErroneous(WorkspaceSpec spec) {
	if (spec.hasError()) {
	    throw new TheiaCloudWebException(TheiaCloudError.fromString(spec.getError()));
	}
	return spec;
    }

    public static void throwIfError(String error) {
	if (TheiaCloudError.isErrorString(error)) {
	    throw new TheiaCloudWebException(TheiaCloudError.fromString(error));
	}
    }

    public static void main(String[] args) {
	throwIfError(TheiaCloudError.INVALID_APP_ID.asString());
    }
}
