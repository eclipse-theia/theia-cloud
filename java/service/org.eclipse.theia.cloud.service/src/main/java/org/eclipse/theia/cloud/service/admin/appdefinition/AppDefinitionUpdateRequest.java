/********************************************************************************
 * Copyright (C) 2025 EclipseSource and others.
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
package org.eclipse.theia.cloud.service.admin.appdefinition;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.theia.cloud.service.ServiceRequest;

public class AppDefinitionUpdateRequest extends ServiceRequest {
    public static final String KIND = "appDefinitionUpdateRequest";

    @Schema(description = "The minimum number of instances to run.", required = false)
    public Integer minInstances;

    @Schema(description = "The maximum number of instances to run.", required = false)
    public Integer maxInstances;

    public AppDefinitionUpdateRequest() {
        super(KIND);
    }

    public AppDefinitionUpdateRequest(String appId) {
        super(KIND, appId);
    }

    @Override
    public String toString() {
        return "AppDefinitionUpdateRequest [minInstances=" + minInstances + ", maxInstances=" + maxInstances
                + ", appId=" + appId + ", kind=" + kind + "]";
    }
}
