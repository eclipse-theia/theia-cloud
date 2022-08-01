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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.theia.cloud.common.k8s.resource.UserScopedSpec;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.CustomResource;

public interface CustomResourceClient<S, T extends CustomResource<S, Void>, L extends KubernetesResourceList<T>>
	extends ResourceClient<T, L> {

    T create(String correlationId, S spec);

    default Optional<S> spec(String name) {
	return get(name).map(T::getSpec);
    }

    default List<T> list(String user) {
	return list().stream().filter(item -> Objects.equals(UserScopedSpec.getUser(item.getSpec()), user))
		.collect(Collectors.toList());
    }

    default List<S> specs() {
	return list().stream().map(item -> item.getSpec()).collect(Collectors.toList());
    }

    default List<S> specs(String user) {
	return list(user).stream().map(item -> item.getSpec()).collect(Collectors.toList());
    }
}
