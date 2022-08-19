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

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public class ServiceRequest {

    @Schema(description = "The App Id of this Theia.cloud instance. Request without a matching Id will be denied.", required = true)
    public String appId;

    @Schema(hidden = true)
    public String kind;

    public ServiceRequest(String kind) {
	this.kind = kind;
    }

    public ServiceRequest(String kind, String appId) {
	this.appId = appId;
	this.kind = kind;
    }

    @Override
    public String toString() {
	return "ServiceRequest [appId=" + appId + ", kind=" + kind + "]";
    }

}
