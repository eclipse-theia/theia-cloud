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
package org.eclipse.theia.cloud.operator.handler.session;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionSpec;
import org.eclipse.theia.cloud.common.util.LabelsUtil;
import org.eclipse.theia.cloud.operator.TheiaCloudOperatorArguments;
import org.eclipse.theia.cloud.operator.handler.AddedHandlerUtil;
import org.eclipse.theia.cloud.operator.routing.SessionRoutingStrategy;
import org.eclipse.theia.cloud.operator.util.K8sUtil;
import org.eclipse.theia.cloud.operator.util.TheiaCloudConfigMapUtil;
import org.eclipse.theia.cloud.operator.util.TheiaCloudDeploymentUtil;
import org.eclipse.theia.cloud.operator.util.TheiaCloudHandlerUtil;
import org.eclipse.theia.cloud.operator.util.TheiaCloudServiceUtil;

import com.google.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.PodResource;

/**
 * A {@link SessionAddedHandler} that relies on the fact that the app definition handler created spare deployments to
 * use.
 */
public class EagerSessionHandler implements SessionHandler {

    public static final String EAGER_START_REFRESH_ANNOTATION = "theia-cloud.io/eager-start-refresh";

    // Fence a rollback against another replica resuming a partial external claim.
    private static final String RESERVATION_ATTEMPT = "theia-cloud.io/eager-reservation-attempt";

    private static final Logger LOGGER = LogManager.getLogger(EagerSessionHandler.class);

    @Inject
    private TheiaCloudClient client;

    @Inject
    protected TheiaCloudOperatorArguments arguments;

    @Inject
    protected SessionRoutingStrategy routingStrategy;

    @Override
    public boolean sessionAdded(Session session, String correlationId) {
        SessionSpec spec = session.getSpec();
        LOGGER.info(formatLogMessage(correlationId, "Handling sessionAdded " + spec));

        String sessionResourceName = session.getMetadata().getName();
        String sessionResourceUID = session.getMetadata().getUid();

        String appDefinitionID = spec.getAppDefinition();
        String userEmail = spec.getUser();

        /* find app definition for session */
        Optional<AppDefinition> appDefinition = client.appDefinitions().get(appDefinitionID);
        if (appDefinition.isEmpty()) {
            LOGGER.error(formatLogMessage(correlationId, "No App Definition with name " + appDefinitionID + " found."));
            return false;
        }

        /* check routing resource exists */
        if (!routingStrategy.ensureRoutingResourceExists(appDefinition.get(), correlationId)) {
            LOGGER.error(formatLogMessage(correlationId,
                    "No routing resource for app definition " + appDefinitionID + " found."));
            return false;
        }

        ServicePair pair;
        try {
            pair = reserveServicePair(appDefinition.get(), session, correlationId);
        } catch (KubernetesClientException e) {
            LOGGER.error(formatLogMessage(correlationId, "Error reserving Services for " + sessionResourceName), e);
            return false;
        }
        if (pair == null) {
            return false;
        }
        if (session.getNonNullStatus().hasUrl()) {
            return true;
        }

        int instance = pair.instance();
        final String deploymentName = TheiaCloudDeploymentUtil.getDeploymentName(appDefinition.get(), instance);
        try {
            client.kubernetes().apps().deployments().withName(deploymentName).edit(deployment -> {
                if (!ownedBy(deployment, session)) {
                    TheiaCloudHandlerUtil.addOwnerReferenceToItem(correlationId, sessionResourceName, sessionResourceUID,
                            deployment);
                }
                return deployment;
            });
        } catch (KubernetesClientException e) {
            LOGGER.error(formatLogMessage(correlationId, "Error while editing deployment "
                    + (appDefinitionID + TheiaCloudDeploymentUtil.DEPLOYMENT_NAME + instance)), e);
            return false;
        }

        if (arguments.isUseKeycloak()) {
            /* add user to allowed emails */
            try {
                client.kubernetes().configMaps()
                        .withName(TheiaCloudConfigMapUtil.getEmailConfigName(appDefinition.get(), instance))
                        .edit(configmap -> {
                            configmap.setData(Collections
                                    .singletonMap(AddedHandlerUtil.FILENAME_AUTHENTICATED_EMAILS_LIST, userEmail));
                            return configmap;
                        });

            } catch (KubernetesClientException e) {
                LOGGER.error(
                        formatLogMessage(correlationId,
                                "Error while editing email configmap "
                                        + (appDefinitionID + TheiaCloudConfigMapUtil.CONFIGMAP_EMAIL_NAME + instance)),
                        e);
                return false;
            }

            // Add/update annotation to the session pod to trigger a sync with the Kubelet.
            // Otherwise, the pod might not be updated with the new email list for the OAuth proxy in time.
            // This is the case because ConfigMap changes are not propagated to the pod immediately but during a
            // periodic sync. See
            // https://kubernetes.io/docs/concepts/configuration/configmap/#mounted-configmaps-are-updated-automatically
            // NOTE that this is still not a one hundred percent guarantee that the pod is updated in time.
            try {
                LOGGER.info(formatLogMessage(correlationId, "Adding update annotation to pods..."));
                client.kubernetes().pods().list().getItems().forEach(pod -> {
                    // Use startsWith because the actual owner is the deployment's ReplicaSet
                    // whose name starts with the deployment name
                    if (pod.getMetadata().getOwnerReferences().stream()
                            .anyMatch(or -> or.getName().startsWith(deploymentName))) {

                        LOGGER.debug(formatLogMessage(correlationId,
                                "Adding update annotation to pod " + pod.getMetadata().getName()));
                        pod.getMetadata().getAnnotations().put(EAGER_START_REFRESH_ANNOTATION,
                                Instant.now().toString());
                        // Apply the changes
                        PodResource podResource = client.pods().withName(pod.getMetadata().getName());
                        podResource.edit(p -> pod);
                        LOGGER.debug(formatLogMessage(correlationId,
                                "Added update annotation to pod " + pod.getMetadata().getName()));
                    }
                });
            } catch (KubernetesClientException e) {
                LOGGER.error(formatLogMessage(correlationId, "Error while editing pod annotations"), e);
                return false;
            }
        }

        /* adjust the routing */
        String sessionUrl;
        try {
            sessionUrl = routingStrategy.addSessionRouting(session, appDefinition.get(), pair.external(), instance,
                    correlationId);
        } catch (KubernetesClientException e) {
            LOGGER.error(formatLogMessage(correlationId,
                    "Error while updating routing for session " + session.getMetadata().getName()), e);
            return false;
        }
        if (sessionUrl == null) {
            LOGGER.error(formatLogMessage(correlationId,
                    "Failed to add routing for session " + session.getMetadata().getName()));
            return false;
        }

        /* Update session resource */
        try {
            AddedHandlerUtil.updateSessionURLAsync(client.sessions(), session, client.namespace(), sessionUrl,
                    correlationId);
        } catch (KubernetesClientException e) {
            LOGGER.error(
                    formatLogMessage(correlationId, "Error while editing session " + session.getMetadata().getName()),
                    e);
            return false;
        }

        return true;
    }

    private record ServicePair(int instance, Service external, Service internal) {
    }

    private ServicePair reserveServicePair(AppDefinition app, Session session, String correlationId) {
        List<Service> services = K8sUtil.getExistingServices(client.kubernetes(), client.namespace(),
                app.getMetadata().getName(), app.getMetadata().getUid());
        Map<String, Service> byName = new HashMap<>();
        Set<String> reserved = new HashSet<>();
        for (Service service : services) {
            String name = service.getMetadata().getName();
            byName.put(name, service);
            if (ownedBy(service, session) || labeledFor(service, session)) {
                reserved.add(name);
            }
        }

        // Prefer the lowest eligible instance on every replica. A partial reservation pins the choice.
        ServicePair pair = null;
        for (Service external : services) {
            String name = external.getMetadata().getName();
            if (!canReserve(external, session)) {
                continue;
            }
            Integer instance = TheiaCloudServiceUtil.getId(correlationId, app, external);
            if (instance == null || !name.equals(TheiaCloudServiceUtil.getServiceName(app, instance))) {
                continue;
            }
            String internalName = TheiaCloudServiceUtil.getInternalServiceName(app, instance);
            Service internal = byName.get(internalName);
            String selector = TheiaCloudHandlerUtil.getAppSelector(app, instance);
            if (internal != null && selectsInstance(external, selector) && selectsInstance(internal, selector)
                    && Set.of(name, internalName).containsAll(reserved)
                    && canReserve(internal, session)
                    && (pair == null || instance < pair.instance())) {
                pair = new ServicePair(instance, external, internal);
            }
        }
        if (pair == null || (session.getNonNullStatus().hasUrl() && reserved.isEmpty())) {
            LOGGER.error(formatLogMessage(correlationId, "No matching Service pair available for Session "
                    + session.getMetadata().getName() + " of " + app.getMetadata().getName()
                    + ". Services already reserved by this Session: " + reserved));
            return null;
        }

        Map<String, String> labels = LabelsUtil.createSessionLabels(session, app);
        boolean completingInternal = TheiaCloudServiceUtil.isUnusedService(pair.internal());
        Service external = claim(pair.external(), session, labels, correlationId, completingInternal);
        try {
            if (reserved.isEmpty() && hasOtherReservation(app, session, pair)) {
                // Two replicas may have seen different free pairs for the same Session. After acquiring
                // the external Service, re-list before completing either pair.
                LOGGER.error(formatLogMessage(correlationId, "Another instance was reserved for Session "
                        + session.getMetadata().getName()));
                rollbackExternal(pair.external(), external, correlationId);
                return null;
            }
            Service internal = claim(pair.internal(), session, labels, correlationId, false);
            return new ServicePair(pair.instance(), external, internal);
        } catch (KubernetesClientException e) {
            // A transport failure may have committed the internal write. Leave the reservation pinned in that case.
            if (!ownedBy(pair.external(), session) && e.getCode() >= 400 && e.getCode() < 500) {
                rollbackExternal(pair.external(), external, correlationId);
            }
            throw e;
        }
    }

    private boolean hasOtherReservation(AppDefinition app, Session session, ServicePair pair) {
        Set<String> names = Set.of(pair.external().getMetadata().getName(), pair.internal().getMetadata().getName());
        return K8sUtil.getExistingServices(client.kubernetes(), client.namespace(), app.getMetadata().getName(),
                app.getMetadata().getUid()).stream().anyMatch(service -> !names.contains(service.getMetadata().getName())
                        && (ownedBy(service, session) || labeledFor(service, session)));
    }

    private void rollbackExternal(Service original, Service claimed, String correlationId) {
        // Restore the original only if the claim is still the last write, including any concurrent replay.
        Service restored = new ServiceBuilder(original).editMetadata()
                .withResourceVersion(claimed.getMetadata().getResourceVersion()).endMetadata().build();
        try {
            client.services().resource(restored).update();
        } catch (KubernetesClientException e) {
            LOGGER.warn(formatLogMessage(correlationId, "Could not release " + original.getMetadata().getName()
                    + " after failing to reserve its internal Service"), e);
        }
    }

    private static boolean selectsInstance(Service service, String selector) {
        return service.getSpec() != null && selector.equals(service.getSpec().getSelector().get("app"));
    }

    private static boolean ownedBy(HasMetadata resource, Session session) {
        return resource.getMetadata().getOwnerReferences().stream().anyMatch(owner -> Session.KIND.equals(owner.getKind())
                && session.getMetadata().getUid().equals(owner.getUid())
                && session.getMetadata().getName().equals(owner.getName()));
    }

    private static boolean labeledFor(Service service, Session session) {
        return session.getMetadata().getUid().equals(service.getMetadata().getLabels()
                .get(LabelsUtil.LABEL_KEY_SESSION_UUID));
    }

    private static boolean canReserve(Service service, Session session) {
        // Discovery has already checked the AppDefinition owner. A second owner must be this Session.
        var metadata = service.getMetadata();
        String labeledSession = metadata.getLabels().get(LabelsUtil.LABEL_KEY_SESSION_UUID);
        return (labeledSession == null || session.getMetadata().getUid().equals(labeledSession))
                && (TheiaCloudServiceUtil.isUnusedService(service)
                        || (metadata.getOwnerReferences().size() == 2 && ownedBy(service, session)));
    }

    private Service claim(Service service, Session session, Map<String, String> labels, String correlationId,
            boolean completingInternal) {
        boolean alreadyOwned = ownedBy(service, session);
        if (!completingInternal && alreadyOwned
                && service.getMetadata().getLabels().entrySet().containsAll(labels.entrySet())) {
            return service;
        }
        Service updated = new ServiceBuilder(service).editMetadata().addToLabels(labels).endMetadata().build();
        if (completingInternal) {
            // Even a replay must change the external resourceVersion before writing the internal Service.
            // Otherwise a pending rollback from the first replica could undo this replica's reservation.
            updated.getMetadata().getAnnotations().put(RESERVATION_ATTEMPT, UUID.randomUUID().toString());
        }
        if (!alreadyOwned) {
            TheiaCloudHandlerUtil.addOwnerReferenceToItem(correlationId, session.getMetadata().getName(),
                    session.getMetadata().getUid(), updated);
        }
        // update() sends the resourceVersion from the listed Service and fails on conflict.
        return client.services().resource(updated).update();
    }

    @Override
    public boolean sessionDeleted(Session session, String correlationId) {
        SessionSpec spec = session.getSpec();
        LOGGER.info(formatLogMessage(correlationId, "Handling sessionDeleted " + spec));

        // Find app definition for session. If it's not there anymore, we don't need to clean up because the resources
        // are deleted by Kubernetes garbage collection.
        String appDefinitionID = spec.getAppDefinition();
        Optional<AppDefinition> appDefinition = client.appDefinitions().get(appDefinitionID);
        if (appDefinition.isEmpty()) {
            LOGGER.info(formatLogMessage(correlationId, "No App Definition with name " + appDefinitionID
                    + " found. Thus, no cleanup is needed because associated resources are deleted by Kubernets garbage collecion."));
            return true;
        }

        String sessionResourceName = session.getMetadata().getName();
        String sessionResourceUID = session.getMetadata().getUid();
        // Kubernetes may already have removed the Session owner reference when deletion is observed.
        List<Service> services = K8sUtil.getExistingServices(client.kubernetes(), client.namespace(),
                appDefinition.get().getMetadata().getName(), appDefinition.get().getMetadata().getUid()).stream()
                .filter(service -> ownedBy(service, session) || labeledFor(service, session)).toList();
        if (services.isEmpty()) {
            return true;
        }
        if (services.size() == 1) {
            // Interrupted between claims, or between the two releases at the end of cleanup.
            return releaseService(services.getFirst(), session, correlationId);
        }
        List<Service> externalServices = services.stream()
                .filter(service -> !service.getMetadata().getName().endsWith("-int")).toList();
        if (services.size() != 2 || externalServices.size() != 1) {
            LOGGER.error(formatLogMessage(correlationId, "Ambiguous Service reservation for " + sessionResourceName));
            return false;
        }
        Integer instance = TheiaCloudServiceUtil.getId(correlationId, appDefinition.get(), externalServices.getFirst());
        if (instance == null) {
            return false;
        }

        // Cleanup routing rule to prevent further traffic to the session pod
        try {
            boolean routingCleanupSuccess = routingStrategy.removeSessionRouting(session, appDefinition.get(), instance,
                    correlationId);
            if (!routingCleanupSuccess) {
                LOGGER.error(formatLogMessage(correlationId, "Failed to remove routing for session " + spec.getName()));
                return false;
            }
        } catch (KubernetesClientException e) {
            LOGGER.error(formatLogMessage(correlationId, "Error while removing routing for session " + spec.getName()),
                    e);
            return false;
        }

        // Remove owner reference from deployment
        final String deploymentName = TheiaCloudDeploymentUtil.getDeploymentName(appDefinition.get(), instance);
        try {
            client.kubernetes().apps().deployments().withName(deploymentName).edit(deployment -> TheiaCloudHandlerUtil
                    .removeOwnerReferenceFromItem(correlationId, sessionResourceName, sessionResourceUID, deployment));
        } catch (KubernetesClientException e) {
            LOGGER.error(formatLogMessage(correlationId, "Error while editing deployment "
                    + (appDefinitionID + TheiaCloudDeploymentUtil.DEPLOYMENT_NAME + instance)), e);
            return false;
        }

        if (arguments.isUseKeycloak()) {
            // Remove user from allowed emails in config map
            try {
                client.kubernetes().configMaps()
                        .withName(TheiaCloudConfigMapUtil.getEmailConfigName(appDefinition.get(), instance))
                        .edit(configmap -> {
                            configmap.setData(
                                    Collections.singletonMap(AddedHandlerUtil.FILENAME_AUTHENTICATED_EMAILS_LIST, null));
                            return configmap;
                        });
            } catch (KubernetesClientException e) {
                LOGGER.error(formatLogMessage(correlationId, "Error while editing email configmap "
                        + (appDefinitionID + TheiaCloudConfigMapUtil.CONFIGMAP_EMAIL_NAME + instance)), e);
                return false;
            }
        }

        // Delete the pod to clean temporary workspace files. The deployment recreates a fresh pod automatically.
        try {
            Optional<Pod> pod = client.kubernetes().pods().list().getItems().stream()
                    .filter(p -> p.getMetadata().getName().startsWith(deploymentName)).findAny();
            if (pod.isPresent()) {
                LOGGER.info(formatLogMessage(correlationId, "Deleting pod " + pod.get().getMetadata().getName()));
                client.pods().withName(pod.get().getMetadata().getName()).delete();
            }
        } catch (KubernetesClientException e) {
            LOGGER.error(formatLogMessage(correlationId, "Error while deleting pod"), e);
            return false;
        }

        // Keep the instance reserved until its route, user credentials and pod have been cleaned up.
        for (Service service : services) {
            if (!releaseService(service, session, correlationId)) {
                return false;
            }
        }
        return true;
    }

    private boolean releaseService(Service service, Session session, String correlationId) {
        String name = service.getMetadata().getName();
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                Service current = client.services().withName(name).get();
                if (current == null || (!ownedBy(current, session) && !labeledFor(current, session))) {
                    return true;
                }
                TheiaCloudHandlerUtil.removeOwnerReferenceFromItem(correlationId, session.getMetadata().getName(),
                        session.getMetadata().getUid(), current);
                if (labeledFor(current, session)) {
                    current.getMetadata().getAnnotations().remove(RESERVATION_ATTEMPT);
                    current.getMetadata().getLabels().keySet().removeAll(LabelsUtil.getSessionSpecificLabelKeys());
                }
                client.services().resource(current).update();
                return true;
            } catch (KubernetesClientException e) {
                LOGGER.warn(formatLogMessage(correlationId, "Attempt " + attempt + " failed to release " + name), e);
            }
        }
        return false;
    }

}
