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

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionSpec;
import org.eclipse.theia.cloud.service.ApplicationProperties;
import org.eclipse.theia.cloud.service.BaseResource;
import org.eclipse.theia.cloud.service.K8sUtil;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/service/appdefinition")
@Authenticated
public class AppDefinitionResource extends BaseResource {

    @Inject
    private K8sUtil k8sUtil;

    public AppDefinitionResource(ApplicationProperties applicationProperties) {
        super(applicationProperties);
    }

    @Operation(summary = "List app definitions", description = "List available app definitions.")
    @GET
    @Path("/{appId}")
    public List<AppDefinitionSpec> list(@PathParam("appId") String appId) {
        evaluateRequest(new AppDefinitionListRequest(appId));
        List<AppDefinitionSpec> appDefinitions = k8sUtil.listAppDefinitions();
        return appDefinitions;
    }
}
