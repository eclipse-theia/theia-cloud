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

public class ServiceResponse {

    @Schema(description = "Specifies which kind of response this is. This is optional.", required = false)
    public String kind;

    @Schema(description = "Whether the request was successful", required = true)
    public boolean success;

    @Schema(description = "The error message. Empty if request was successful.", required = false)
    public String error;

    public ServiceResponse(String kind) {
	this.kind = kind;
    }

    public ServiceResponse(String kind, boolean success, String error) {
	this.kind = kind;
	this.success = success;
	this.error = error;
    }

    public static ServiceResponse error(String kind, String error) {
	return new ServiceResponse(kind, false, error);
    }

    public static ServiceResponse ok(String kind) {
	return new ServiceResponse(kind, true, "");
    }

    @Override
    public String toString() {
	return "ServiceResponse [kind=" + kind + ", success=" + success + ", error=" + error + "]";
    }

}
