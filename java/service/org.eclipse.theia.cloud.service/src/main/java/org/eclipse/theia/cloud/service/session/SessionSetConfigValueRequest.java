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
package org.eclipse.theia.cloud.service.session;

import org.eclipse.theia.cloud.service.ServiceRequest;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public class SessionSetConfigValueRequest extends ServiceRequest {

    public static final String KIND = "sessionSetConfigValueRequest";

    @Schema(description = "The configuration key", required = true)
    public String key;

    @Schema(description = "The configuration value", required = true, nullable = true)
    public String value;

    public SessionSetConfigValueRequest() {
        super(KIND);
    }

    public SessionSetConfigValueRequest(String appId, String key, String value) {
        super(KIND, appId);
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return "SessionSetConfigValueRequest [key=" + key + ", value=" + value + ", appId=" + appId + ", kind=" + kind
                + "]";
    }
}