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

import org.eclipse.theia.cloud.conversion.Conversion.ConversionException;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.apiextensions.v1.ConversionResponse;
import io.fabric8.kubernetes.api.model.apiextensions.v1.ConversionReview;

public final class ConversionController {

    private static final Map<String, Conversion> CONVERSIONS = new LinkedHashMap<>();

    private ConversionController() {
    }

    public static ConversionReview handle(ConversionReview conversionReview) {
	List<HasMetadata> objects = conversionReview.getRequest().getObjects();
	String desiredAPIVersion = conversionReview.getRequest().getDesiredAPIVersion();

	List<HasMetadata> convertedObjects = new ArrayList<>(objects.size());
	for (HasMetadata object : objects) {
	    String currentAPIVersion = object.getApiVersion();
	    Conversion conversion = CONVERSIONS.get(currentAPIVersion);
	    if (conversion == null) {
		return createConversionReview(conversionReview,
			new NoSuchElementException("No Conversion for " + currentAPIVersion));
	    }
	    try {
		convertedObjects.add(conversion.convert(object, desiredAPIVersion));
	    } catch (ConversionException e) {
		return createConversionReview(conversionReview, e);
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
