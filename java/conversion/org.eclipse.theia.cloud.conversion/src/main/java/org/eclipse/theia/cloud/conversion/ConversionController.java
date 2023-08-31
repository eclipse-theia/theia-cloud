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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionStatus;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionStatus;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.Workspace;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.WorkspaceSpec;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.WorkspaceStatus;
import org.eclipse.theia.cloud.conversion.Conversion.ConversionException;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.apiextensions.v1.ConversionResponse;
import io.fabric8.kubernetes.api.model.apiextensions.v1.ConversionReview;

public final class ConversionController {

    private static final Map<String, Conversion> CONVERSIONS = new LinkedHashMap<>();

    @SuppressWarnings("deprecation")
    private ConversionController() {

	CONVERSIONS.put(org.eclipse.theia.cloud.common.k8s.resource.appdefinition.v7.AppDefinitionV7beta.API,
		new GenericConversion<>(
			org.eclipse.theia.cloud.common.k8s.resource.appdefinition.v7.AppDefinitionV7betaSpec.class,
			org.eclipse.theia.cloud.common.k8s.resource.appdefinition.v7.AppDefinitionV7betaStatus.class,
			org.eclipse.theia.cloud.common.k8s.resource.appdefinition.v7.AppDefinitionV7beta.class,
			AppDefinitionSpec.class, AppDefinitionStatus.class, AppDefinition.class));

	CONVERSIONS.put(org.eclipse.theia.cloud.common.k8s.resource.session.v5.SessionV5beta.API,
		new GenericConversion<>(org.eclipse.theia.cloud.common.k8s.resource.session.v5.SessionV5betaSpec.class,
			org.eclipse.theia.cloud.common.k8s.resource.session.v5.SessionV5betaStatus.class,
			org.eclipse.theia.cloud.common.k8s.resource.session.v5.SessionV5beta.class,
			SessionSpec.class, SessionStatus.class, Session.class));

	CONVERSIONS.put(org.eclipse.theia.cloud.common.k8s.resource.workspace.v2.WorkspaceV2beta.API,
		new GenericConversion<>(
			org.eclipse.theia.cloud.common.k8s.resource.workspace.v2.WorkspaceV2betaSpec.class,
			org.eclipse.theia.cloud.common.k8s.resource.workspace.v2.WorkspaceV2betaStatus.class,
			org.eclipse.theia.cloud.common.k8s.resource.workspace.v2.WorkspaceV2beta.class,
			WorkspaceSpec.class, WorkspaceStatus.class, Workspace.class));
    }

    public static ConversionReview handle(ConversionReview conversionReview) {
	List<HasMetadata> objects = conversionReview.getRequest().getObjects();
	String desiredAPIVersion = conversionReview.getRequest().getDesiredAPIVersion();

	List<HasMetadata> convertedObjects = new ArrayList<>(objects.size());
	for (HasMetadata object : objects) {

	    HasMetadata objectUnderMigration = object;
	    String currentAPIVersion = objectUnderMigration.getApiVersion();

	    while (!desiredAPIVersion.equals(currentAPIVersion)) {
		Conversion conversion = CONVERSIONS.get(currentAPIVersion);
		if (conversion == null) {
		    return createConversionReview(conversionReview,
			    new NoSuchElementException("No Conversion for " + currentAPIVersion));
		}
		try {
		    objectUnderMigration = conversion.convert(objectUnderMigration);
		} catch (ConversionException e) {
		    return createConversionReview(conversionReview, e);
		}
		currentAPIVersion = objectUnderMigration.getApiVersion();
	    }
	}

	return createConversionReview(conversionReview, convertedObjects);
    }

    private static ConversionReview createConversionReview(ConversionReview conversionReview,
	    List<HasMetadata> convertedObjects) {
	ConversionReview result = new ConversionReview();
	ConversionResponse response = new ConversionResponse();
	response.setUid(conversionReview.getRequest().getUid());
	response.setResult(new Status());
	response.getResult().setStatus("Success");
	response.setConvertedObjects(convertedObjects);
	result.setResponse(response);
	return result;
    }

    private static ConversionReview createConversionReview(ConversionReview conversionReview, Exception e) {
	ConversionReview result = new ConversionReview();
	ConversionResponse response = new ConversionResponse();
	response.setUid(conversionReview.getRequest().getUid());
	response.setResult(new Status());
	response.getResult().setStatus("Failed");
	response.getResult().setMessage(e.getMessage());
	result.setResponse(response);
	return result;
    }

}
