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
package org.eclipse.theia.cloud.service.admin;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.theia.cloud.service.AdminOnly;
import org.eclipse.theia.cloud.service.ApplicationProperties;
import org.eclipse.theia.cloud.service.BaseResource;
import org.eclipse.theia.cloud.service.PingRequest;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/service/admin")
@AdminOnly
public class RootAdminResource extends BaseResource {

    @Inject
    public RootAdminResource(ApplicationProperties applicationProperties) {
        super(applicationProperties);
    }

    @Operation(summary = "Admin Ping", description = "Replies with success if the service is available and the user an admin.")
    @GET
    @Path("/{appId}")
    public boolean ping(@PathParam("appId") String appId) {
        evaluateRequest(new PingRequest(appId));
        return true;
    }
}
