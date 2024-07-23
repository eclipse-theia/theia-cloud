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

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionSpec;
import org.eclipse.theia.cloud.common.util.JavaUtil;
import org.eclipse.theia.cloud.operator.TheiaCloudOperatorArguments;
import org.eclipse.theia.cloud.operator.handler.AddedHandlerUtil;
import org.eclipse.theia.cloud.operator.ingress.IngressPathProvider;
import org.eclipse.theia.cloud.operator.util.K8sUtil;
import org.eclipse.theia.cloud.operator.util.TheiaCloudConfigMapUtil;
import org.eclipse.theia.cloud.operator.util.TheiaCloudDeploymentUtil;
import org.eclipse.theia.cloud.operator.util.TheiaCloudHandlerUtil;
import org.eclipse.theia.cloud.operator.util.TheiaCloudServiceUtil;

import com.google.inject.Inject;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressRuleValue;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBackend;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRule;
import io.fabric8.kubernetes.api.model.networking.v1.IngressServiceBackend;
import io.fabric8.kubernetes.api.model.networking.v1.ServiceBackendPort;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;

/**
 * A {@link SessionAddedHandler} that relies on the fact that the app definition handler created spare deployments to
 * use.
 */
public class EagerStartSessionHandler implements SessionHandler {

    private static final Logger LOGGER = LogManager.getLogger(EagerStartSessionHandler.class);

    @Inject
    private TheiaCloudClient client;

    @Inject
    protected IngressPathProvider ingressPathProvider;

    @Inject
    protected TheiaCloudOperatorArguments arguments;

    @Override
    public boolean sessionAdded(Session session, String correlationId) {
        SessionSpec spec = session.getSpec();
        LOGGER.info(formatLogMessage(correlationId, "Handling " + spec));

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

        String appDefinitionResourceName = appDefinition.get().getMetadata().getName();
        String appDefinitionResourceUID = appDefinition.get().getMetadata().getUid();
        int port = appDefinition.get().getSpec().getPort();

        /* find ingress */
        Optional<Ingress> ingress = K8sUtil.getExistingIngress(client.kubernetes(), client.namespace(),
                appDefinitionResourceName, appDefinitionResourceUID);
        if (ingress.isEmpty()) {
            LOGGER.error(
                    formatLogMessage(correlationId, "No Ingress for app definition " + appDefinitionID + " found."));
            return false;
        }

        /* get a service to use */
        Entry<Optional<Service>, Boolean> reserveServiceResult = reserveService(client.kubernetes(), client.namespace(),
                appDefinitionResourceName, appDefinitionResourceUID, appDefinitionID, sessionResourceName,
                sessionResourceUID, correlationId);
        if (reserveServiceResult.getValue()) {
            LOGGER.info(formatLogMessage(correlationId, "Found an already reserved service"));
            return true;
        }
        Optional<Service> serviceToUse = reserveServiceResult.getKey();
        if (serviceToUse.isEmpty()) {
            LOGGER.error(
                    formatLogMessage(correlationId, "No Service for app definition " + appDefinitionID + " found."));
            return false;
        }

        /* get the deployment for the service and add as owner */
        Integer instance = TheiaCloudServiceUtil.getId(correlationId, appDefinition.get(), serviceToUse.get());
        if (instance == null) {
            LOGGER.error(formatLogMessage(correlationId, "Error while getting instance from Service"));
            return false;
        }

        try {
            client.kubernetes().apps().deployments()
                    .withName(TheiaCloudDeploymentUtil.getDeploymentName(appDefinition.get(), instance))
                    .edit(deployment -> TheiaCloudHandlerUtil.addOwnerReferenceToItem(correlationId,
                            sessionResourceName, sessionResourceUID, deployment));
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
        }

        /* adjust the ingress */
        String host;
        try {
            host = updateIngress(ingress, serviceToUse, appDefinitionID, instance, port, appDefinition.get(),
                    correlationId);
        } catch (KubernetesClientException e) {
            LOGGER.error(formatLogMessage(correlationId,
                    "Error while editing ingress " + ingress.get().getMetadata().getName()), e);
            return false;
        }

        /* Update session resource */
        try {
            AddedHandlerUtil.updateSessionURLAsync(client.sessions(), session, client.namespace(), host, correlationId);
        } catch (KubernetesClientException e) {
            LOGGER.error(
                    formatLogMessage(correlationId, "Error while editing session " + session.getMetadata().getName()),
                    e);
            return false;
        }

        return true;
    }

    protected synchronized Entry<Optional<Service>, Boolean> reserveService(NamespacedKubernetesClient client,
            String namespace, String appDefinitionResourceName, String appDefinitionResourceUID, String appDefinitionID,
            String sessionResourceName, String sessionResourceUID, String correlationId) {
        List<Service> existingServices = K8sUtil.getExistingServices(client, namespace, appDefinitionResourceName,
                appDefinitionResourceUID);

        Optional<Service> alreadyReservedService = TheiaCloudServiceUtil.getServiceOwnedBySession(sessionResourceName,
                sessionResourceUID, existingServices);
        if (alreadyReservedService.isPresent()) {
            return JavaUtil.tuple(alreadyReservedService, true);
        }

        Optional<Service> serviceToUse = TheiaCloudServiceUtil.getUnusedService(existingServices);
        if (serviceToUse.isEmpty()) {
            return JavaUtil.tuple(serviceToUse, false);
        }

        /* add our session as owner to the service */
        try {
            client.services().inNamespace(namespace).withName(serviceToUse.get().getMetadata().getName())
                    .edit(service -> TheiaCloudHandlerUtil.addOwnerReferenceToItem(correlationId, sessionResourceName,
                            sessionResourceUID, service));
        } catch (KubernetesClientException e) {
            LOGGER.error(formatLogMessage(correlationId,
                    "Error while editing service " + (serviceToUse.get().getMetadata().getName())), e);
            return JavaUtil.tuple(Optional.empty(), false);
        }
        return JavaUtil.tuple(serviceToUse, false);
    }

    protected synchronized String updateIngress(Optional<Ingress> ingress, Optional<Service> serviceToUse,
            String appDefinitionID, int instance, int port, AppDefinition appDefinition, String correlationId) {
        final String host = arguments.getInstancesHost();
        String path = ingressPathProvider.getPath(appDefinition, instance);
        client.ingresses().edit(correlationId, ingress.get().getMetadata().getName(),
                ingressToUpdate -> addIngressRule(ingressToUpdate, serviceToUse.get(), host, port, path));
        return host + path + "/";
    }

    protected Ingress addIngressRule(Ingress ingress, Service serviceToUse, String host, int port, String path) {
        IngressRule ingressRule = new IngressRule();
        ingress.getSpec().getRules().add(ingressRule);

        ingressRule.setHost(host);

        HTTPIngressRuleValue http = new HTTPIngressRuleValue();
        ingressRule.setHttp(http);

        HTTPIngressPath httpIngressPath = new HTTPIngressPath();
        http.getPaths().add(httpIngressPath);
        httpIngressPath.setPath(path + AddedHandlerUtil.INGRESS_REWRITE_PATH);
        httpIngressPath.setPathType("Prefix");

        IngressBackend ingressBackend = new IngressBackend();
        httpIngressPath.setBackend(ingressBackend);

        IngressServiceBackend ingressServiceBackend = new IngressServiceBackend();
        ingressBackend.setService(ingressServiceBackend);
        ingressServiceBackend.setName(serviceToUse.getMetadata().getName());

        ServiceBackendPort serviceBackendPort = new ServiceBackendPort();
        ingressServiceBackend.setPort(serviceBackendPort);
        serviceBackendPort.setNumber(port);

        return ingress;
    }

}
