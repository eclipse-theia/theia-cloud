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

@Schema(name = "SessionPerformance", description = "Description of the performance of a session")
public class SessionPerformance {

    @Schema(description = "Used CPU amount of the workspace", required = true)
    public String cpuAmount;

    @Schema(description = "Used CPU format of the workspace", required = true)
    public String cpuFormat;

    @Schema(description = "Used memory amount of the workspace", required = true)
    public String memoryAmount;

    @Schema(description = "Used memory format of the workspace", required = true)
    public String memoryFormat;

    public SessionPerformance() {
    }

    public SessionPerformance(String cpuAmount, String cpuFormat, String memoryAmount, String memoryFormat) {
	this.cpuAmount = cpuAmount;
	this.cpuFormat = cpuFormat;
	this.memoryAmount = memoryAmount;
	this.memoryFormat = memoryFormat;
    }

    @Override
    public String toString() {
	return "UserWorkspace [cpu=" + cpuAmount + cpuFormat + ", memory=" + memoryFormat + memoryFormat + "]";
    }

}
