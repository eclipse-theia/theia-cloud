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

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;
import static org.eclipse.theia.cloud.common.util.NamingUtil.asValidName;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;

public final class TheiaCloudHandlerUtil {

    private static final Logger LOGGER = LogManager.getLogger(TheiaCloudHandlerUtil.class);

    public static final String PLACEHOLDER_NAMESPACE = "placeholder-namespace";
    public static final String PLACEHOLDER_APP = "placeholder-app";
    public static final String PLACEHOLDER_PORT = "placeholder-port";
    public static final String PLACEHOLDER_EMAILSCONFIGNAME = "placeholder-emailsconfigname";
    public static final String PLACEHOLDER_CONFIGNAME = "placeholder-configname";

    public static final String PLACEHOLDER_MONITOR_PORT = "placeholder-monitor-port";

    private TheiaCloudHandlerUtil() {
    }

    public static <T extends HasMetadata> Set<Integer> computeIdsOfMissingItems(int instances, List<T> existingItems,
            Function<T, Integer> getId) {
        Set<Integer> missing = IntStream.rangeClosed(1, instances).boxed().collect(Collectors.toSet());
        existingItems.stream()//
                .map(getId)//
                .forEach(missing::remove);
        return missing;
    }

    public static String getAppSelector(AppDefinition appDefinition, int instance) {
        return asValidName(appDefinition.getSpec().getName() + "-" + instance);
    }

    public static String getAppSelector(Session session) {
        return asValidName(session.getSpec().getName() + "-" + session.getMetadata().getUid());
    }

    public static <T extends HasMetadata> T addOwnerReferenceToItem(String correlationId, String sessionResourceName,
            String sessionResourceUID, T item) {
        OwnerReference serviceOwnerReference = createOwnerReference(sessionResourceName, sessionResourceUID);
        LOGGER.info(formatLogMessage(correlationId, "Adding a new owner reference to " + item.getMetadata().getName()));
        item.getMetadata().getOwnerReferences().add(serviceOwnerReference);
        return item;
    }

    /**
     * Removes the owner reference from the item if it is present. Does nothing otherwise.
     */
    public static <T extends HasMetadata> T removeOwnerReferenceFromItem(String correlationId,
            String sessionResourceName, String sessionResourceUID, T item) {
        LOGGER.info(
                formatLogMessage(correlationId, "Removing the owner reference from " + item.getMetadata().getName()));
        item.getMetadata().getOwnerReferences().removeIf(ownerReference -> {
            return ownerReference.getName().equals(sessionResourceName)
                    && ownerReference.getUid().equals(sessionResourceUID);
        });
        return item;
    }

    public static OwnerReference createOwnerReference(String sessionResourceName, String sessionResourceUID) {
        OwnerReference ownerReference = new OwnerReference();
        ownerReference.setApiVersion(HasMetadata.getApiVersion(Session.class));
        ownerReference.setKind(Session.KIND);
        ownerReference.setName(sessionResourceName);
        ownerReference.setUid(sessionResourceUID);
        return ownerReference;
    }

}
