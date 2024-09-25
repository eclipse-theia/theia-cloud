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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.eclipse.theia.cloud.common.util.JavaUtil;
import org.eclipse.theia.cloud.common.util.WatcherAdapter;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.StatusDetails;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public interface ResourceClient<T extends HasMetadata, L extends KubernetesResourceList<T>> {

    NonNamespaceOperation<T, L, Resource<T>> operation();

    default Resource<T> resource(String name) {
        return operation().withName(name);
    }

    default Optional<T> get(String name) {
        return Optional.ofNullable(resource(name).get());
    }

    default boolean has(String name) {
        return get(name).isPresent();
    }

    default List<StatusDetails> delete(String correlationId, String name) {
        info(correlationId, "Delete " + name);
        return resource(name).delete();
    }

    default T edit(String correlationId, String name, Consumer<T> consumer) {
        info(correlationId, "Edit " + name);
        Resource<T> resource = resource(name);
        if (resource.get() == null) {
            warn(correlationId, "Resource not found! Could not edit " + name
                    + ". Was this called before the resource has been created?");
            return null;
        }
        return resource.edit(JavaUtil.toUnary(consumer));
    }

    default T editStatus(String correlationId, String name, Consumer<T> consumer) {
        trace(correlationId, "Edit status of " + name);
        Resource<T> resource = resource(name);
        if (resource.get() == null) {
            warn(correlationId, "Resource " + name
                    + " not found. Could not update the status. Note that the status of a resource cannot be changed before it is created on the cluster.");
            return null;
        }
        return resource.editStatus(JavaUtil.toUnary(consumer));
    }

    Optional<T> loadAndCreate(String correlationId, String yaml, Consumer<T> customization);

    default Optional<T> loadAndCreate(String correlationId, String yaml) {
        return loadAndCreate(correlationId, yaml, item -> {
        });
    }

    default List<T> list() {
        return operation().list().getItems();
    }

    default void watchUntil(BiFunction<Action, T, Boolean> eventHandler, long timeout, TimeUnit unit)
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Watch watch = operation().watch(WatcherAdapter.eventReceived((action, item) -> {
            if (eventHandler.apply(action, item)) {
                latch.countDown();
            }
        }));

        try {
            if (!latch.await(timeout, unit)) {
                throw new InterruptedException("Timeout reached. Interrupt Watch.");
            }
        } finally {
            watch.close();
        }
    }

    // Logging API

    String getTypeName();

    void info(String correlationId, String message);

    void warn(String correlationId, String message);

    void error(String correlationId, String message);

    void error(String correlationId, String message, Throwable throwable);

    void trace(String correlationId, String message);
}
