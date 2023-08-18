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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.CustomResource;

public class GenericConversion<SPECV1, STATUSV1, RESOURCEV1 extends CustomResource<SPECV1, STATUSV1>, //
	SPECV2, STATUSV2, RESOURCEV2 extends CustomResource<SPECV2, STATUSV2>> implements Conversion {

    private Class<SPECV1> spec1Class;
    private Class<STATUSV1> status1Class;
    private Class<RESOURCEV1> resource1Class;

    private Class<SPECV2> spec2Class;
    private Class<STATUSV2> status2Class;
    private Class<RESOURCEV2> resource2Class;

    public GenericConversion(//
	    Class<SPECV1> spec1Class, Class<STATUSV1> status1Class, Class<RESOURCEV1> resource1Class,
	    Class<SPECV2> spec2Class, Class<STATUSV2> status2Class, Class<RESOURCEV2> resource2Class) {
	this.spec1Class = spec1Class;
	this.status1Class = status1Class;
	this.resource1Class = resource1Class;
	this.spec2Class = spec2Class;
	this.status2Class = status2Class;
	this.resource2Class = resource2Class;
    }

    @Override
    public HasMetadata convert(HasMetadata object) throws ConversionException {
	if (!resource1Class.isInstance(object)) {
	    throw new ConversionException(
		    "Unexpected object: " + object.getClass().getName() + ". Expected was " + resource1Class.getName());
	}
	RESOURCEV1 resourcev1 = resource1Class.cast(object);
	SPECV1 specv1 = resourcev1.getSpec();
	STATUSV1 statusv1 = resourcev1.getStatus();

	RESOURCEV2 nextVersion = performMigrationToNextVersion(specv1, statusv1, object.getMetadata());

	return nextVersion;
    }

    protected RESOURCEV2 performMigrationToNextVersion(SPECV1 specv1, STATUSV1 statusv1, ObjectMeta objectMeta)
	    throws ConversionException {
	Constructor<SPECV2> specv2constructor;
	try {
	    specv2constructor = spec2Class.getConstructor(spec1Class);
	} catch (NoSuchMethodException | SecurityException e) {
	    throw new ConversionException(
		    "Missing migration from " + spec1Class.getName() + " to " + spec2Class.getName(), e);
	}
	SPECV2 specv2;
	try {
	    specv2 = specv2constructor.newInstance(specv1);
	} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
		| InvocationTargetException e) {
	    throw new ConversionException(
		    "Failed migration from " + spec1Class.getName() + " to " + spec2Class.getName(), e);
	}

	Constructor<STATUSV2> statusv2constructor;
	try {
	    statusv2constructor = status2Class.getConstructor(status1Class);
	} catch (NoSuchMethodException | SecurityException e) {
	    throw new ConversionException(
		    "Missing migration from " + status1Class.getName() + " to " + status2Class.getName(), e);
	}
	STATUSV2 statusv2;
	try {
	    statusv2 = statusv2constructor.newInstance(statusv1);
	} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
		| InvocationTargetException e) {
	    throw new ConversionException(
		    "Failed migration from " + status1Class.getName() + " to " + status2Class.getName(), e);
	}

	RESOURCEV2 resourcev2;
	try {
	    resourcev2 = resource2Class.getDeclaredConstructor().newInstance();
	} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
		| NoSuchMethodException | SecurityException e) {
	    throw new ConversionException("Failed to create " + resource2Class.getName(), e);
	}

	resourcev2.setSpec(specv2);
	resourcev2.setStatus(statusv2);

	resourcev2.setMetadata(new ObjectMetaBuilder(objectMeta).build());

	return resourcev2;
    }

}
