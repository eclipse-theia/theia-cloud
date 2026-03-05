/********************************************************************************
 * Copyright (C) 2022 EclipseSource, Lockular, Ericsson, STMicroelectronics and 
 * others.
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
package org.eclipse.theia.cloud.operator.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRoute;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRouteList;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;

public final class TheiaCloudIngressUtil {

    private TheiaCloudIngressUtil() {
    }

    public static boolean checkForExistingIngressAndAddOwnerReferencesIfMissing(NamespacedKubernetesClient client,
            String namespace, AppDefinition appDefinition, String correlationId) {
        Optional<HTTPRoute> existingRouteWithParentAppDefinition = K8sUtil.getExistingHttpRoute(
                client, namespace, appDefinition.getMetadata().getName(), appDefinition.getMetadata().getUid());
        if (existingRouteWithParentAppDefinition.isPresent()) {
            return true;
        }
        Optional<HTTPRoute> route = K8sUtil.getExistingHttpRoute(client, namespace,
                appDefinition.getSpec().getIngressname());
        if (route.isPresent()) {
            OwnerReference ownerReference = new OwnerReference();
            ownerReference.setApiVersion(HasMetadata.getApiVersion(AppDefinition.class));
            ownerReference.setKind(AppDefinition.KIND);
            ownerReference.setName(appDefinition.getMetadata().getName());
            ownerReference.setUid(appDefinition.getMetadata().getUid());
            addOwnerReferenceToHttpRoute(client, namespace, route.get(), ownerReference);
        }
        return route.isPresent();
    }

    public static String getIngressName(AppDefinition appDefinition) {
        return appDefinition.getSpec().getIngressname();
    }

    public static void addOwnerReferenceToHttpRoute(NamespacedKubernetesClient client, String namespace,
            HTTPRoute route, OwnerReference ownerReference) {
        client.resources(HTTPRoute.class, HTTPRouteList.class).inNamespace(namespace)
                .withName(route.getMetadata().getName())
                .edit(resource -> {
                    List<OwnerReference> ownerReferences = resource.getMetadata().getOwnerReferences();
                    if (ownerReferences == null) {
                        ownerReferences = new ArrayList<>();
                        resource.getMetadata().setOwnerReferences(ownerReferences);
                    }
                    ownerReferences.add(ownerReference);
                    return resource;
                });
    }
}
