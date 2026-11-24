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

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.util.LabelsUtil;
import org.eclipse.theia.cloud.common.util.NamingUtil;
import org.eclipse.theia.cloud.common.util.SessionUtil;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;

public final class TheiaCloudServiceUtil {

    private static final Logger LOGGER = LogManager.getLogger(TheiaCloudServiceUtil.class);

    public static final String SERVICE_NAME = "service";

    public static final String PLACEHOLDER_SERVICENAME = "placeholder-servicename";
    public static final String PLACEHOLDER_SERVICENAME_INTERNAL = "placeholder-servicename-internal";

    private TheiaCloudServiceUtil() {
    }

    public static String getServiceName(AppDefinition appDefinition, int instance) {
        return NamingUtil.createName(appDefinition, instance);
    }

    public static String getServiceName(Session session) {
        return NamingUtil.createName(session);
    }

    public static String getInternalServiceName(Session session) {
        return NamingUtil.createNameWithSuffix(session, "int");
    }

    public static String getInternalServiceName(AppDefinition appDefinition, int instance) {
        return NamingUtil.createNameWithSuffix(appDefinition, instance, "int");
    }

    public static URI getInternalServiceURL(String serviceName, String namespace) {
        int port = SessionUtil.getInternalPort().orElseThrow();
        return URI.create("http://" + serviceName + "." + namespace + ".svc:" + port + "/");
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

    public static Map<String, String> getInternalServiceReplacements(String namespace, Session session,
            AppDefinitionSpec appDefinitionSpec) {
        Map<String, String> replacements = new LinkedHashMap<String, String>();
        replacements.put(PLACEHOLDER_SERVICENAME_INTERNAL, getInternalServiceName(session));
        replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_APP, TheiaCloudHandlerUtil.getAppSelector(session));
        replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_NAMESPACE, namespace);
        replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_PORT, String.valueOf(appDefinitionSpec.getPort()));
        return replacements;
    }

    public static Map<String, String> getInternalServiceReplacements(String namespace, AppDefinition appDefinition,
            int instance) {
        Map<String, String> replacements = new LinkedHashMap<String, String>();
        replacements.put(PLACEHOLDER_SERVICENAME_INTERNAL, getInternalServiceName(appDefinition, instance));
        replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_APP,
                TheiaCloudHandlerUtil.getAppSelector(appDefinition, instance));
        replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_NAMESPACE, namespace);
        replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_PORT, String.valueOf(appDefinition.getSpec().getPort()));
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

    public static boolean isUnusedService(Service service) {
        return service.getMetadata().getOwnerReferences().size() == 1;
    }

    public static boolean ownedBy(HasMetadata resource, Session session) {
        return resource.getMetadata().getOwnerReferences().stream().anyMatch(owner -> Session.KIND.equals(owner.getKind())
                && session.getMetadata().getUid().equals(owner.getUid())
                && session.getMetadata().getName().equals(owner.getName()));
    }

    public static boolean labeledFor(Service service, Session session) {
        return session.getMetadata().getUid().equals(service.getMetadata().getLabels()
                .get(LabelsUtil.LABEL_KEY_SESSION_UUID));
    }

    public static boolean canReserve(Service service, Session session) {
        // Discovery has already checked the AppDefinition owner. A second owner must be this Session.
        var metadata = service.getMetadata();
        String labeledSession = metadata.getLabels().get(LabelsUtil.LABEL_KEY_SESSION_UUID);
        return (labeledSession == null || session.getMetadata().getUid().equals(labeledSession))
                && (isUnusedService(service)
                        || (metadata.getOwnerReferences().size() == 2 && ownedBy(service, session)));
    }
}
