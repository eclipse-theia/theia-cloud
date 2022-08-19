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
package org.eclipse.theia.cloud.service.session;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.theia.cloud.service.ServiceRequest;

@Schema(name = "SessionStopRequest", description = "A request to stop a session")
public class SessionStopRequest extends ServiceRequest {
    public static final String KIND = "sessionStopRequest";

    @Schema(description = "The user identification, usually the email address.", required = true)
    public String user;

    @Schema(description = "The name of the session to stop.", required = true)
    public String sessionName;

    public SessionStopRequest() {
	super(KIND);
    }

    public SessionStopRequest(String appId, String user, String sessionName) {
	super(KIND, appId);
	this.sessionName = sessionName;
	this.user = user;
    }

    @Override
    public String toString() {
	return "SessionStopRequest [user=" + user + ", sessionName=" + sessionName + ", appId=" + appId + ", kind="
		+ kind + "]";
    }

}
