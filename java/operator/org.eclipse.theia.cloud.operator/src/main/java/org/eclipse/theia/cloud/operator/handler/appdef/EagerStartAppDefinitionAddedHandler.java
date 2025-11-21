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
package org.eclipse.theia.cloud.operator.handler.appdef;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionSpec;
import org.eclipse.theia.cloud.operator.TheiaCloudOperatorArguments;
import org.eclipse.theia.cloud.operator.bandwidth.BandwidthLimiter;
import org.eclipse.theia.cloud.operator.handler.AddedHandlerUtil;
import org.eclipse.theia.cloud.operator.ingress.IngressPathProvider;
import org.eclipse.theia.cloud.operator.replacements.DeploymentTemplateReplacements;
import org.eclipse.theia.cloud.operator.util.JavaResourceUtil;
import org.eclipse.theia.cloud.operator.util.K8sUtil;
import org.eclipse.theia.cloud.operator.util.TheiaCloudConfigMapUtil;
import org.eclipse.theia.cloud.operator.util.TheiaCloudDeploymentUtil;
import org.eclipse.theia.cloud.operator.util.TheiaCloudIngressUtil;
import org.eclipse.theia.cloud.operator.util.TheiaCloudServiceUtil;

import com.google.inject.Inject;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;

/**
 * A {@link AppDefinitionHandler} that will eagerly start up deployments ahead of usage time which will later be used as
 * sessions.
 */
public class EagerStartAppDefinitionAddedHandler implements AppDefinitionHandler {

    private static final Logger LOGGER = LogManager.getLogger(EagerStartAppDefinitionAddedHandler.class);

    public static final String LABEL_KEY = "theia-cloud.io/template-purpose";
    public static final String LABEL_VALUE_PROXY = "proxy";
    public static final String LABEL_VALUE_EMAILS = "emails";

    @Inject
    protected TheiaCloudClient client;

    @Inject
    protected TheiaCloudOperatorArguments arguments;

    @Inject
    protected IngressPathProvider ingressPathProvider;

    @Inject
    protected BandwidthLimiter bandwidthLimiter;

    @Inject
    protected DeploymentTemplateReplacements deploymentReplacements;

    @Override
    public boolean appDefinitionAdded(AppDefinition appDefinition, String correlationId) {
        AppDefinitionSpec spec = appDefinition.getSpec();
        LOGGER.info(formatLogMessage(correlationId, "Handling " + spec));

        String appDefinitionResourceName = appDefinition.getMetadata().getName();
        String appDefinitionResourceUID = appDefinition.getMetadata().getUid();
        int instances = spec.getMinInstances();

        /* Create ingress if not existing */
        if (!TheiaCloudIngressUtil.checkForExistingIngressAndAddOwnerReferencesIfMissing(client.kubernetes(),
                client.namespace(), appDefinition, correlationId)) {
            LOGGER.error(formatLogMessage(correlationId,
                    "Expected ingress '" + spec.getIngressname() + "' for app definition '" + appDefinitionResourceName
                            + "' does not exist. Abort handling app definition."));
            return false;
        } else {
            LOGGER.trace(formatLogMessage(correlationId, "Ingress available already"));
        }

        /* Get existing services for this app definition */
        List<Service> existingServices = K8sUtil.getExistingServices(client.kubernetes(), client.namespace(),
                appDefinitionResourceName, appDefinitionResourceUID);

        /* Compute missing services */
        Set<Integer> missingServiceIds = TheiaCloudServiceUtil.computeIdsOfMissingServices(appDefinition, correlationId,
                instances, existingServices);

        Map<String, String> labelsToAdd = new HashMap<String, String>();

        /* Create missing services for this app definition */
        for (int instance : missingServiceIds) {
            createAndApplyService(client.kubernetes(), client.namespace(), correlationId, appDefinitionResourceName,
                    appDefinitionResourceUID, instance, appDefinition, arguments.isUseKeycloak(), labelsToAdd);
        }

        /* Create missing internal services for this app definition */
        for (int instance : missingServiceIds) {
            createAndApplyInternalService(client.kubernetes(), client.namespace(), correlationId,
                    appDefinitionResourceName, appDefinitionResourceUID, instance, appDefinition, labelsToAdd);
        }

        if (arguments.isUseKeycloak()) {
            /* Get existing configmaps for this app definition */
            List<ConfigMap> existingConfigMaps = K8sUtil.getExistingConfigMaps(client.kubernetes(), client.namespace(),
                    appDefinitionResourceName, appDefinitionResourceUID);
            List<ConfigMap> existingProxyConfigMaps = existingConfigMaps.stream()//
                    .filter(configmap -> LABEL_VALUE_PROXY.equals(configmap.getMetadata().getLabels().get(LABEL_KEY)))//
                    .collect(Collectors.toList());
            List<ConfigMap> existingEmailsConfigMaps = existingConfigMaps.stream()//
                    .filter(configmap -> LABEL_VALUE_EMAILS.equals(configmap.getMetadata().getLabels().get(LABEL_KEY)))//
                    .collect(Collectors.toList());

            /* Compute missing configmaps */
            Set<Integer> missingProxyIds = TheiaCloudConfigMapUtil.computeIdsOfMissingProxyConfigMaps(appDefinition,
                    correlationId, instances, existingProxyConfigMaps);
            Set<Integer> missingEmailIds = TheiaCloudConfigMapUtil.computeIdsOfMissingEmailConfigMaps(appDefinition,
                    correlationId, instances, existingEmailsConfigMaps);

            /* Create missing configmaps for this app definition */
            for (int instance : missingProxyIds) {
                createAndApplyProxyConfigMap(client.kubernetes(), client.namespace(), correlationId,
                        appDefinitionResourceName, appDefinitionResourceUID, instance, appDefinition, labelsToAdd);
            }
            for (int instance : missingEmailIds) {
                createAndApplyEmailConfigMap(client.kubernetes(), client.namespace(), correlationId,
                        appDefinitionResourceName, appDefinitionResourceUID, instance, appDefinition, labelsToAdd);
            }
        }

        /* Get existing deployments for this app definition */
        List<Deployment> existingDeployments = K8sUtil.getExistingDeployments(client.kubernetes(), client.namespace(),
                appDefinitionResourceName, appDefinitionResourceUID);

        /* Compute missing deployments */
        Set<Integer> missingDeploymentIds = TheiaCloudDeploymentUtil.computeIdsOfMissingDeployments(appDefinition,
                correlationId, instances, existingDeployments);

        /* Create missing deployments for this app definition */
        for (int instance : missingDeploymentIds) {
            createAndApplyDeployment(client.kubernetes(), client.namespace(), correlationId, appDefinitionResourceName,
                    appDefinitionResourceUID, instance, appDefinition, arguments.isUseKeycloak(), labelsToAdd);
        }
        return true;
    }

    protected void createAndApplyService(NamespacedKubernetesClient client, String namespace, String correlationId,
            String appDefinitionResourceName, String appDefinitionResourceUID, int instance,
            AppDefinition appDefinition, boolean useOAuth2Proxy, Map<String, String> labelsToAdd) {
        Map<String, String> replacements = TheiaCloudServiceUtil.getServiceReplacements(namespace, appDefinition,
                instance);
        String templateYaml = useOAuth2Proxy ? AddedHandlerUtil.TEMPLATE_SERVICE_YAML
                : AddedHandlerUtil.TEMPLATE_SERVICE_WITHOUT_AOUTH2_PROXY_YAML;
        String serviceYaml;
        try {
            serviceYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(templateYaml, replacements,
                    correlationId);
        } catch (IOException | URISyntaxException e) {
            LOGGER.error(
                    formatLogMessage(correlationId, "Error while adjusting template for instance number " + instance),
                    e);
            return;
        }
        K8sUtil.loadAndCreateServiceWithOwnerReference(client, namespace, correlationId, serviceYaml, AppDefinition.API,
                AppDefinition.KIND, appDefinitionResourceName, appDefinitionResourceUID, 0, labelsToAdd);
    }

    protected void createAndApplyInternalService(NamespacedKubernetesClient client, String namespace,
            String correlationId, String appDefinitionResourceName, String appDefinitionResourceUID, int instance,
            AppDefinition appDefinition, Map<String, String> labelsToAdd) {
        Map<String, String> replacements = TheiaCloudServiceUtil.getInternalServiceReplacements(namespace,
                appDefinition, instance);
        String serviceYaml;
        try {
            serviceYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(
                    AddedHandlerUtil.TEMPLATE_INTERNAL_SERVICE_YAML, replacements, correlationId);
        } catch (IOException | URISyntaxException e) {
            LOGGER.error(formatLogMessage(correlationId,
                    "Error while adjusting internal service template for instance number " + instance), e);
            return;
        }
        K8sUtil.loadAndCreateServiceWithOwnerReference(client, namespace, correlationId, serviceYaml, AppDefinition.API,
                AppDefinition.KIND, appDefinitionResourceName, appDefinitionResourceUID, 0, labelsToAdd);
    }

    protected void createAndApplyDeployment(NamespacedKubernetesClient client, String namespace, String correlationId,
            String appDefinitionResourceName, String appDefinitionResourceUID, int instance,
            AppDefinition appDefinition, boolean useOAuth2Proxy, Map<String, String> labelsToAdd) {
        Map<String, String> replacements = deploymentReplacements.getReplacements(namespace, appDefinition, instance);
        String templateYaml = useOAuth2Proxy ? AddedHandlerUtil.TEMPLATE_DEPLOYMENT_YAML
                : AddedHandlerUtil.TEMPLATE_DEPLOYMENT_WITHOUT_AOUTH2_PROXY_YAML;
        String deploymentYaml;
        try {
            deploymentYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(templateYaml, replacements,
                    correlationId);
        } catch (IOException | URISyntaxException e) {
            LOGGER.error(
                    formatLogMessage(correlationId, "Error while adjusting template for instance number " + instance),
                    e);
            return;
        }
        K8sUtil.loadAndCreateDeploymentWithOwnerReference(client, namespace, correlationId, deploymentYaml,
                AppDefinition.API, AppDefinition.KIND, appDefinitionResourceName, appDefinitionResourceUID, 0,
                labelsToAdd, deployment -> {
                    bandwidthLimiter.limit(deployment, appDefinition.getSpec().getDownlinkLimit(),
                            appDefinition.getSpec().getUplinkLimit(), correlationId);
                    AddedHandlerUtil.removeEmptyResources(deployment);
                    if (appDefinition.getSpec().getPullSecret() != null
                            && !appDefinition.getSpec().getPullSecret().isEmpty()) {
                        AddedHandlerUtil.addImagePullSecret(deployment, appDefinition.getSpec().getPullSecret());
                    }
                });
    }

    protected void createAndApplyProxyConfigMap(NamespacedKubernetesClient client, String namespace,
            String correlationId, String appDefinitionResourceName, String appDefinitionResourceUID, int instance,
            AppDefinition appDefinition, Map<String, String> labelsToAdd) {
        Map<String, String> replacements = TheiaCloudConfigMapUtil.getProxyConfigMapReplacements(namespace,
                appDefinition, instance);
        String configMapYaml;
        try {
            configMapYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(
                    AddedHandlerUtil.TEMPLATE_CONFIGMAP_YAML, replacements, correlationId);
        } catch (IOException | URISyntaxException e) {
            LOGGER.error(
                    formatLogMessage(correlationId, "Error while adjusting template for instance number " + instance),
                    e);
            return;
        }
        K8sUtil.loadAndCreateConfigMapWithOwnerReference(client, namespace, correlationId, configMapYaml,
                AppDefinition.API, AppDefinition.KIND, appDefinitionResourceName, appDefinitionResourceUID, 0,
                labelsToAdd, configMap -> {
                    String host = arguments.getInstancesHost() + ingressPathProvider.getPath(appDefinition, instance);
                    int port = appDefinition.getSpec().getPort();
                    AddedHandlerUtil.updateProxyConfigMap(client, namespace, configMap, host, port);
                });
    }

    protected void createAndApplyEmailConfigMap(NamespacedKubernetesClient client, String namespace,
            String correlationId, String appDefinitionResourceName, String appDefinitionResourceUID, int instance,
            AppDefinition appDefinition, Map<String, String> labelsToAdd) {
        Map<String, String> replacements = TheiaCloudConfigMapUtil.getEmailConfigMapReplacements(namespace,
                appDefinition, instance);
        String configMapYaml;
        try {
            configMapYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(
                    AddedHandlerUtil.TEMPLATE_CONFIGMAP_EMAILS_YAML, replacements, correlationId);
        } catch (IOException | URISyntaxException e) {
            LOGGER.error(
                    formatLogMessage(correlationId, "Error while adjusting template for instance number " + instance),
                    e);
            return;
        }
        K8sUtil.loadAndCreateConfigMapWithOwnerReference(client, namespace, correlationId, configMapYaml,
                AppDefinition.API, AppDefinition.KIND, appDefinitionResourceName, appDefinitionResourceUID, 0,
                labelsToAdd);
    }

}
