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
package org.eclipse.theia.cloud.common.k8s.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.util.LogMessageUtil;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public class BaseResourceClient<T extends HasMetadata, L extends KubernetesResourceList<T>>
	implements ResourceClient<T, L> {

    protected static Logger LOGGER = LogManager.getLogger(CustomResourceClient.class);

    protected NamespacedKubernetesClient client;
    protected NonNamespaceOperation<T, L, Resource<T>> operation;
    private Class<T> typeClass;

    public BaseResourceClient(NamespacedKubernetesClient client, Class<T> typeClass, Class<L> listClass) {
	this(client, client.resources(typeClass, listClass), typeClass);
    }

    public BaseResourceClient(NamespacedKubernetesClient client, NonNamespaceOperation<T, L, Resource<T>> operation,
	    Class<T> typeClass) {
	this.client = client;
	this.operation = operation;
	this.typeClass = typeClass;
    }

    @Override
    public String getTypeName() {
	return typeClass.getSimpleName();
    }

    @Override
    public NonNamespaceOperation<T, L, Resource<T>> operation() {
	return this.operation;
    }

    @Override
    public void info(String correlationId, String message) {
	LOGGER.info(LogMessageUtil.formatLogMessage(correlationId, message));
    }

    @Override
    public void warn(String correlationId, String message) {
	LOGGER.warn(LogMessageUtil.formatLogMessage(correlationId, message));
    }

    @Override
    public void error(String correlationId, String message) {
	LOGGER.error(LogMessageUtil.formatLogMessage(correlationId, message));
    }

    @Override
    public void error(String correlationId, String message, Throwable throwable) {
	LOGGER.error(LogMessageUtil.formatLogMessage(correlationId, message), throwable);
    }

    @Override
    public void trace(String correlationId, String message) {
	LOGGER.trace(LogMessageUtil.formatLogMessage(correlationId, message));
    }

    @Override
    public Optional<T> loadAndCreate(String correlationId, String yaml, Consumer<T> customization) {
	try (ByteArrayInputStream inputStream = new ByteArrayInputStream(yaml.getBytes())) {
	    trace(correlationId, "Loading new " + getTypeName() + ":\n" + yaml);
	    T newItem = operation().load(inputStream).item();

	    trace(correlationId, "Customizing new " + getTypeName());
	    customization.accept(newItem);

	    trace(correlationId, "Creating new " + getTypeName());
	    operation().resource(newItem).create();
	    info(correlationId, "Created a new " + getTypeName());

	    return Optional.of(newItem);
	} catch (IOException exception) {
	    error(correlationId, "Error while reading yaml byte stream", exception);
	}
	return Optional.empty();
    }
}
