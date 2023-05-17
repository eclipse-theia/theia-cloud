/********************************************************************************
 * Copyright (C) 2022-2023 EclipseSource, Lockular, Ericsson, STMicroelectronics and 
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
import org.eclipse.theia.cloud.service.UserScopedServiceRequest;

@Schema(name = "SessionListRequest", description = "A request to list the sessions of a user.")
public class SessionListRequest extends UserScopedServiceRequest {
    public static final String KIND = "sessionListRequest";

    public SessionListRequest() {
	super(KIND);
    }

    public SessionListRequest(String appId, String user) {
	super(KIND, appId);
	this.user = user;
    }

    @Override
    public String toString() {
	return "SessionListRequest [user=" + user + ", appId=" + appId + ", kind=" + kind + "]";
    }

}
