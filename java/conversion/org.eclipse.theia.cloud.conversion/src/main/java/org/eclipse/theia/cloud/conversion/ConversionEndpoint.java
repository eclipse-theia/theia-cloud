/********************************************************************************
 * Copyright (C) 2023 EclipseSource and others.
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
package org.eclipse.theia.cloud.conversion;

import org.eclipse.theia.cloud.conversion.mappers.appdefinition.AppDefinitionV1beta10Mapper;
import org.eclipse.theia.cloud.conversion.mappers.appdefinition.AppDefinitionV1beta8Mapper;
import org.eclipse.theia.cloud.conversion.mappers.appdefinition.AppDefinitionV1beta9Mapper;
import org.eclipse.theia.cloud.conversion.mappers.session.SessionV1beta6Mapper;
import org.eclipse.theia.cloud.conversion.mappers.session.SessionV1beta7Mapper;
import org.eclipse.theia.cloud.conversion.mappers.session.SessionV1beta8Mapper;
import org.eclipse.theia.cloud.conversion.mappers.workspace.WorkspaceV1beta3Mapper;
import org.eclipse.theia.cloud.conversion.mappers.workspace.WorkspaceV1beta4Mapper;
import org.eclipse.theia.cloud.conversion.mappers.workspace.WorkspaceV1beta5Mapper;
import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apiextensions.v1.ConversionReview;
import io.javaoperatorsdk.webhook.conversion.ConversionController;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class ConversionEndpoint {
    protected Logger logger = Logger.getLogger(getClass());

    private final ConversionController appDefinitionController;
    private final ConversionController workspaceController;
    private final ConversionController sessionController;

    public ConversionEndpoint() {
	this.appDefinitionController = new ConversionController();
	appDefinitionController.registerMapper(new AppDefinitionV1beta8Mapper());
	appDefinitionController.registerMapper(new AppDefinitionV1beta9Mapper());
	appDefinitionController.registerMapper(new AppDefinitionV1beta10Mapper());

	this.workspaceController = new ConversionController();
	workspaceController.registerMapper(new WorkspaceV1beta3Mapper());
	workspaceController.registerMapper(new WorkspaceV1beta4Mapper());
	workspaceController.registerMapper(new WorkspaceV1beta5Mapper());

	this.sessionController = new ConversionController();
	sessionController.registerMapper(new SessionV1beta6Mapper());
	sessionController.registerMapper(new SessionV1beta7Mapper());
	sessionController.registerMapper(new SessionV1beta8Mapper());
    }

    @POST
    @Path("convert/appdefinition")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ConversionReview convertAppDefinition(ConversionReview conversionReview) {
	conversionReview.getRequest().getObjects().forEach(obj -> {
	    logger.info("[convert/appdefinition] [" + conversionReview.getRequest().getUid() + "] Converting "
		    + ((HasMetadata) obj).getKind() + " (version: '" + ((HasMetadata) obj).getApiVersion()
		    + "') to version '" + conversionReview.getRequest().getDesiredAPIVersion() + "'");
	});
	return this.appDefinitionController.handle(conversionReview);
    }

    @POST
    @Path("convert/workspace")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ConversionReview convertWorkspace(ConversionReview conversionReview) {
	conversionReview.getRequest().getObjects().forEach(obj -> {
	    logger.info("[convert/workspace] [" + conversionReview.getRequest().getUid() + "] Converting "
		    + ((HasMetadata) obj).getKind() + " (version: '" + ((HasMetadata) obj).getApiVersion()
		    + "') to version '" + conversionReview.getRequest().getDesiredAPIVersion() + "'");
	});
	return this.workspaceController.handle(conversionReview);
    }

    @POST
    @Path("convert/session")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ConversionReview convertSession(ConversionReview conversionReview) {
	conversionReview.getRequest().getObjects().forEach(obj -> {
	    logger.info("[convert/session] [" + conversionReview.getRequest().getUid() + "] Converting "
		    + ((HasMetadata) obj).getKind() + " (version: '" + ((HasMetadata) obj).getApiVersion()
		    + "') to version '" + conversionReview.getRequest().getDesiredAPIVersion() + "'");
	});
	return this.sessionController.handle(conversionReview);
    }

}
