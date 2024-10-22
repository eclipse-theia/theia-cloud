/********************************************************************************
 * Copyright (C) 2024 EclipseSource and others.
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
package org.eclipse.theia.cloud.service.appdefinition;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.theia.cloud.service.ServiceRequest;

@Schema(name = "AppDefinitionListRequest", description = "A request to list available app definitions.")
public class AppDefinitionListRequest extends ServiceRequest {
    public static final String KIND = "appDefinitionListRequest";

    public AppDefinitionListRequest() {
        super(KIND);
    }

    public AppDefinitionListRequest(String appId) {
        super(KIND, appId);
    }

    @Override
    public String toString() {
        return "AppDefinitionListRequest [appId=" + appId + ", kind=" + kind + "]";
    }

}
