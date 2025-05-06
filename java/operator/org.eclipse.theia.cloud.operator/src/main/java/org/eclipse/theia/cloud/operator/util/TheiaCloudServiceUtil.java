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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.util.NamingUtil;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Service;

public final class TheiaCloudServiceUtil {

    private static final Logger LOGGER = LogManager.getLogger(TheiaCloudServiceUtil.class);

    public static final String SERVICE_NAME = "service";

    public static final String PLACEHOLDER_SERVICENAME = "placeholder-servicename";

    private TheiaCloudServiceUtil() {
    }

    public static String getServiceName(AppDefinition appDefinition, int instance) {
        return NamingUtil.createName(appDefinition, instance);
    }

    public static String getServiceName(Session session) {
        return NamingUtil.createName(session);
    }

    public static Integer getId(String correlationId, AppDefinition appDefinition, Service service) {
        String instance = TheiaCloudK8sUtil.extractIdFromName(service.getMetadata());
        try {
            return Integer.valueOf(instance);
        } catch (NumberFormatException e) {
            LOGGER.error(formatLogMessage(correlationId, "Error while getting integer value of " + instance), e);
        }
        return null;
    }

    public static Set<Integer> computeIdsOfMissingServices(AppDefinition appDefinition, String correlationId,
            int instances, List<Service> existingItems) {
        return TheiaCloudHandlerUtil.computeIdsOfMissingItems(instances, existingItems,
                service -> getId(correlationId, appDefinition, service));
    }

    public static Map<String, String> getServiceReplacements(String namespace, AppDefinition appDefinition,
            int instance) {
        Map<String, String> replacements = new LinkedHashMap<String, String>();
        replacements.put(PLACEHOLDER_SERVICENAME, getServiceName(appDefinition, instance));
        replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_APP,
                TheiaCloudHandlerUtil.getAppSelector(appDefinition, instance));
        replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_NAMESPACE, namespace);
        replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_PORT, String.valueOf(appDefinition.getSpec().getPort()));
        putMonitorReplacements(appDefinition.getSpec(), replacements);
        return replacements;
    }

    public static Map<String, String> getServiceReplacements(String namespace, Session session,
            AppDefinitionSpec appDefinitionSpec) {
        Map<String, String> replacements = new LinkedHashMap<String, String>();
        replacements.put(PLACEHOLDER_SERVICENAME, getServiceName(session));
        replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_APP, TheiaCloudHandlerUtil.getAppSelector(session));
        replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_NAMESPACE, namespace);
        replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_PORT, String.valueOf(appDefinitionSpec.getPort()));
        putMonitorReplacements(appDefinitionSpec, replacements);
        return replacements;
    }

    private static void putMonitorReplacements(AppDefinitionSpec appDefinitionSpec, Map<String, String> replacements) {
        if (appDefinitionSpec.getMonitor() != null && appDefinitionSpec.getMonitor().getPort() > 0) {
            String port = String.valueOf(appDefinitionSpec.getMonitor().getPort());
            String replacement = "- name: monitor-express\n" + "      port: " + port + "\n" + "      targetPort: "
                    + port + "\n" + "      protocol: TCP";
            if (appDefinitionSpec.getMonitor().getPort() == appDefinitionSpec.getPort()) {
                // Just remove the placeholder, otherwise the port would be duplicate
                replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_MONITOR_PORT, "");
            } else {
                // Replace the placeholder with the port information
                replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_MONITOR_PORT, replacement);
            }
        } else {
            replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_MONITOR_PORT, "");
        }
    }

    public static Optional<Service> getServiceOwnedBySession(String sessionResourceName, String sessionResourceUID,
            List<Service> existingServices) {
        Optional<Service> alreadyReservedService = existingServices.stream()//
                .filter(service -> {
                    if (isUnusedService(service)) {
                        return false;
                    }
                    for (OwnerReference ownerReference : service.getMetadata().getOwnerReferences()) {
                        if (sessionResourceName.equals(ownerReference.getName())
                                && sessionResourceUID.equals(ownerReference.getUid())) {
                            return true;
                        }
                    }
                    return false;
                })//
                .findAny();
        return alreadyReservedService;
    }

    public static boolean isUnusedService(Service service) {
        return service.getMetadata().getOwnerReferences().size() == 1;
    }

    /**
     * Returns an unused service.
     * 
     * @param existingServices
     * @return
     * @deprecated Use {@link #getUnusedService(List, String)} instead: Services should be owned by the corresponding
     *             app definition.
     */
    @Deprecated
    public static Optional<Service> getUnusedService(List<Service> existingServices) {
        Optional<Service> serviceToUse = existingServices.stream()//
                .filter(TheiaCloudServiceUtil::isUnusedService)//
                .findAny();
        return serviceToUse;
    }

    /**
     * Returns an unused service that is owned by the given app definition.
     * 
     * @param existingServices         The list of services to search in.
     * @param appDefinitionResourceUID The UID of the app definition that should own the service.
     * @return The unused service that is owned by the given app definition or nothing if none is available.
     */
    public static Optional<Service> getUnusedService(List<Service> existingServices, String appDefinitionResourceUID) {
        Optional<Service> serviceToUse = existingServices.stream()//
                .filter(TheiaCloudServiceUtil::isUnusedService)//
                .filter(service -> {
                    for (OwnerReference ownerReference : service.getMetadata().getOwnerReferences()) {
                        if (appDefinitionResourceUID.equals(ownerReference.getUid())) {
                            return true;
                        }
                    }
                    return false;
                })//
                .findAny();
        return serviceToUse;
    };
}
