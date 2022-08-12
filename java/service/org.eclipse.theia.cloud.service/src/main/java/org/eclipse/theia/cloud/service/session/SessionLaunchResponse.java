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
package org.eclipse.theia.cloud.service.session;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.theia.cloud.common.k8s.resource.SessionSpec;
import org.eclipse.theia.cloud.service.ServiceResponse;

@Schema(name = "Session Launch Response", description = "Response for a session launch. Allows to access the URL of the session or the error that occured during launch.")
public class SessionLaunchResponse extends ServiceResponse {
    public static final String KIND = "sessionLaunchResponse";

    @Schema(title = "The URL of the running workspace or empty if there was an error.", required = false)
    public String url;

    public SessionLaunchResponse() {
	super(KIND);
    }

    public SessionLaunchResponse(boolean success, String error, String url) {
	super(KIND, success, error);
	this.url = url;
    }

    public static SessionLaunchResponse from(SessionSpec session) {
	return new SessionLaunchResponse(session.getUrl() != null, session.getError(), session.getUrl());
    }

    public static SessionLaunchResponse error(String error) {
	return new SessionLaunchResponse(false, error, "");
    }

    public static SessionLaunchResponse ok(String url) {
	return new SessionLaunchResponse(true, "", "");
    }

    @Override
    public String toString() {
	return "SessionLaunchResponse [url=" + url + ", kind=" + kind + ", success=" + success + ", error=" + error
		+ "]";
    }

}
