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
package org.eclipse.theia.cloud.operator.replacements;

import static org.eclipse.theia.cloud.operator.util.TheiaCloudHandlerUtil.PLACEHOLDER_APP;
import static org.eclipse.theia.cloud.operator.util.TheiaCloudHandlerUtil.PLACEHOLDER_CONFIGNAME;
import static org.eclipse.theia.cloud.operator.util.TheiaCloudHandlerUtil.PLACEHOLDER_EMAILSCONFIGNAME;
import static org.eclipse.theia.cloud.operator.util.TheiaCloudHandlerUtil.PLACEHOLDER_NAMESPACE;
import static org.eclipse.theia.cloud.operator.util.TheiaCloudHandlerUtil.PLACEHOLDER_PORT;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.operator.TheiaCloudOperatorArguments;
import org.eclipse.theia.cloud.operator.ingress.IngressPathProvider;
import org.eclipse.theia.cloud.operator.util.TheiaCloudConfigMapUtil;
import org.eclipse.theia.cloud.operator.util.TheiaCloudDeploymentUtil;
import org.eclipse.theia.cloud.operator.util.TheiaCloudHandlerUtil;

import com.google.inject.Inject;

public class DefaultDeploymentTemplateReplacements implements DeploymentTemplateReplacements {
    public static final String PLACEHOLDER_DEPLOYMENTNAME = "placeholder-depname";
    public static final String PLACEHOLDER_APPDEFINITIONNAME = "placeholder-definitionname";
    public static final String PLACEHOLDER_IMAGE = "placeholder-image";
    public static final String PLACEHOLDER_IMAGE_PULL_POLICY = "placeholder-pull-policy";
    // TODO Is "Always" the correct default? Should it be setable as an operator
    // argument?
    public static final String DEFAULT_IMAGE_PULL_POLICY = "Always";
    public static final String PLACEHOLDER_CPU_LIMITS = "placeholder-cpu-limits";
    public static final String PLACEHOLDER_MEMORY_LIMITS = "placeholder-memory-limits";
    public static final String PLACEHOLDER_CPU_REQUESTS = "placeholder-cpu-requests";
    public static final String PLACEHOLDER_MEMORY_REQUESTS = "placeholder-memory-requests";
    public static final String PLACEHOLDER_UID = "placeholder-uid";

    public static final String PLACEHOLDER_ENV_SERVICE_AUTH_TOKEN = "placeholder-env-service-auth-token";
    /** @deprecated Use PLACEHOLDER_ENV_SERVICE_AUTH_TOKEN instead */
    public static final String PLACEHOLDER_ENV_APP_ID = "placeholder-env-app-id";
    public static final String PLACEHOLDER_ENV_SERVICE_URL = "placeholder-env-service-url";
    public static final String PLACEHOLDER_ENV_SESSION_UID = "placeholder-env-session-uid";
    public static final String PLACEHOLDER_ENV_SESSION_NAME = "placeholder-env-session-name";
    public static final String PLACEHOLDER_ENV_SESSION_USER = "placeholder-env-session-user";
    public static final String PLACEHOLDER_ENV_SESSION_URL = "placeholder-env-session-url";
    public static final String PLACEHOLDER_ENV_SESSION_SECRET = "placeholder-env-session-secret";
    public static final String PLACEHOLDER_ENV_SESSION_KEYCLOAK_URL = "placeholder-keycloak-env-url";
    public static final String PLACEHOLDER_ENV_SESSION_KEYCLOAK_REALM = "placeholder-keycloak-env-realm";
    public static final String PLACEHOLDER_ENV_SESSION_KEYCLOAK_CLIENT_ID = "placeholder-keycloak-env-clientid";

    public static final String PLACEHOLDER_MONITOR_PORT = "placeholder-monitor-port";
    public static final String PLACEHOLDER_MONITOR_PORT_ENV = "placeholder-monitor-env-port";
    public static final String PLACEHOLDER_ENABLE_ACTIVITY_TRACKER = "placeholder-enable-activity-tracker";
    public static final String PLACEHOLDER_OAUTH2_PROXY_VERSION = "placeholder-oauth2-proxy-version";

    protected static final String DEFAULT_UID = "1000";

    @Inject
    protected TheiaCloudOperatorArguments arguments;

    @Inject
    protected IngressPathProvider ingressPathProvider;

    @Override
    public Map<String, String> getReplacements(String namespace, AppDefinition appDefinition, int instance) {
        Map<String, String> replacements = new LinkedHashMap<String, String>();
        replacements.put(PLACEHOLDER_NAMESPACE, namespace);
        replacements.putAll(getAppDefinitionData(appDefinition));
        replacements.putAll(getEnvironmentVariables(appDefinition, instance));
        replacements.putAll(getInstanceData(appDefinition, instance));
        replacements.put(PLACEHOLDER_OAUTH2_PROXY_VERSION, arguments.getOAuth2ProxyVersion());
        return replacements;
    }

    @Override
    public Map<String, String> getReplacements(String namespace, AppDefinition appDefinition, Session session) {
        Map<String, String> replacements = new LinkedHashMap<String, String>();
        replacements.put(PLACEHOLDER_NAMESPACE, namespace);
        replacements.putAll(getAppDefinitionData(appDefinition));
        replacements.putAll(getEnvironmentVariables(appDefinition, session));
        replacements.putAll(getSessionData(session));
        replacements.put(PLACEHOLDER_OAUTH2_PROXY_VERSION, arguments.getOAuth2ProxyVersion());
        return replacements;
    }

    protected Map<String, String> getAppDefinitionData(AppDefinition appDefinition) {
        Map<String, String> appDefinitionData = new LinkedHashMap<String, String>();
        appDefinitionData.put(PLACEHOLDER_APPDEFINITIONNAME, appDefinition.getSpec().getName());
        appDefinitionData.put(PLACEHOLDER_IMAGE, appDefinition.getSpec().getImage());
        appDefinitionData.put(PLACEHOLDER_IMAGE_PULL_POLICY,
                orDefault(appDefinition.getSpec().getImagePullPolicy(), DEFAULT_IMAGE_PULL_POLICY));
        appDefinitionData.put(PLACEHOLDER_PORT, String.valueOf(appDefinition.getSpec().getPort()));
        appDefinitionData.put(PLACEHOLDER_CPU_LIMITS, orEmpty(appDefinition.getSpec().getLimitsCpu()));
        appDefinitionData.put(PLACEHOLDER_MEMORY_LIMITS, orEmpty(appDefinition.getSpec().getLimitsMemory()));
        appDefinitionData.put(PLACEHOLDER_CPU_REQUESTS, orEmpty(appDefinition.getSpec().getRequestsCpu()));
        appDefinitionData.put(PLACEHOLDER_MEMORY_REQUESTS, orEmpty(appDefinition.getSpec().getRequestsMemory()));
        appDefinitionData.put(PLACEHOLDER_UID, getUID(appDefinition.getSpec().getUid()));
        return appDefinitionData;
    }

    protected Map<String, String> getEnvironmentVariables(AppDefinition appDefinition, Session session) {
        Map<String, String> environmentVariables = getEnvironmentVariables(appDefinition, Optional.of(session));
        environmentVariables.put(PLACEHOLDER_ENV_SESSION_URL, TheiaCloudDeploymentUtil
                .getSessionURL(arguments.getInstancesHost(), ingressPathProvider, appDefinition, session));
        return environmentVariables;
    }

    protected Map<String, String> getEnvironmentVariables(AppDefinition appDefinition, int instance) {
        Map<String, String> environmentVariables = getEnvironmentVariables(appDefinition, Optional.empty());
        environmentVariables.put(PLACEHOLDER_ENV_SESSION_URL, TheiaCloudDeploymentUtil
                .getSessionURL(arguments.getInstancesHost(), ingressPathProvider, appDefinition, instance));
        return environmentVariables;
    }

    protected Map<String, String> getEnvironmentVariables(AppDefinition appDefinition, Optional<Session> session) {
        Map<String, String> environmentVariables = new LinkedHashMap<String, String>();
        // Set both new and old placeholders for backwards compatibility
        environmentVariables.put(PLACEHOLDER_ENV_SERVICE_AUTH_TOKEN, arguments.getServiceAuthToken());
        environmentVariables.put(PLACEHOLDER_ENV_APP_ID, arguments.getServiceAuthToken());
        environmentVariables.put(PLACEHOLDER_ENV_SERVICE_URL, arguments.getServiceUrl());
        environmentVariables.put(PLACEHOLDER_ENV_SESSION_UID, session.map(s -> s.getMetadata().getUid()).orElse(""));
        environmentVariables.put(PLACEHOLDER_ENV_SESSION_NAME, session.map(s -> s.getSpec().getName()).orElse(""));
        environmentVariables.put(PLACEHOLDER_ENV_SESSION_USER, session.map(s -> s.getSpec().getUser()).orElse(""));
        environmentVariables.put(PLACEHOLDER_ENV_SESSION_SECRET,
                session.map(s -> s.getSpec().getSessionSecret()).orElse(""));

        if (arguments.isUseKeycloak()) {
            environmentVariables.put(PLACEHOLDER_ENV_SESSION_KEYCLOAK_URL, orEmpty(arguments.getKeycloakURL()));
            environmentVariables.put(PLACEHOLDER_ENV_SESSION_KEYCLOAK_REALM, orEmpty(arguments.getKeycloakRealm()));
            environmentVariables.put(PLACEHOLDER_ENV_SESSION_KEYCLOAK_CLIENT_ID,
                    orEmpty(arguments.getKeycloakClientId()));
        } else {
            environmentVariables.put(PLACEHOLDER_ENV_SESSION_KEYCLOAK_URL, "");
            environmentVariables.put(PLACEHOLDER_ENV_SESSION_KEYCLOAK_REALM, "");
            environmentVariables.put(PLACEHOLDER_ENV_SESSION_KEYCLOAK_CLIENT_ID, "");
        }

        if (arguments.isEnableMonitor()) {
            if (appDefinition.getSpec().getMonitor() != null && appDefinition.getSpec().getMonitor().getPort() > 0) {
                String port = String.valueOf(appDefinition.getSpec().getMonitor().getPort());
                environmentVariables.put(PLACEHOLDER_MONITOR_PORT_ENV, port);
                if (appDefinition.getSpec().getMonitor().getPort() == appDefinition.getSpec().getPort()) {
                    // Just remove the placeholder, otherwise the port would be duplicate
                    environmentVariables.put(PLACEHOLDER_MONITOR_PORT, "");
                } else {
                    // Replace the placeholder with the port information
                    environmentVariables.put(PLACEHOLDER_MONITOR_PORT,
                            "- containerPort: " + port + "\n" + "              name: monitor");
                }
            } else {
                environmentVariables.put(TheiaCloudHandlerUtil.PLACEHOLDER_MONITOR_PORT, "");
            }
            environmentVariables.put(PLACEHOLDER_ENABLE_ACTIVITY_TRACKER,
                    arguments.isEnableActivityTracker() ? "true" : "false");
        } else {
            environmentVariables.put(TheiaCloudHandlerUtil.PLACEHOLDER_MONITOR_PORT, "");
        }
        return environmentVariables;
    }

    protected Map<String, String> getSessionData(Session session) {
        Map<String, String> sessionData = new LinkedHashMap<String, String>();
        sessionData.put(PLACEHOLDER_DEPLOYMENTNAME, TheiaCloudDeploymentUtil.getDeploymentName(session));
        sessionData.put(PLACEHOLDER_APP, TheiaCloudHandlerUtil.getAppSelector(session));
        sessionData.put(PLACEHOLDER_CONFIGNAME, TheiaCloudConfigMapUtil.getProxyConfigName(session));
        sessionData.put(PLACEHOLDER_EMAILSCONFIGNAME, TheiaCloudConfigMapUtil.getEmailConfigName(session));
        return sessionData;
    }

    protected Map<String, String> getInstanceData(AppDefinition appDefinition, int instance) {
        Map<String, String> sessionData = new LinkedHashMap<String, String>();
        sessionData.put(PLACEHOLDER_DEPLOYMENTNAME,
                TheiaCloudDeploymentUtil.getDeploymentName(appDefinition, instance));
        sessionData.put(PLACEHOLDER_APP, TheiaCloudHandlerUtil.getAppSelector(appDefinition, instance));
        sessionData.put(PLACEHOLDER_CONFIGNAME, TheiaCloudConfigMapUtil.getProxyConfigName(appDefinition, instance));
        sessionData.put(PLACEHOLDER_EMAILSCONFIGNAME,
                TheiaCloudConfigMapUtil.getEmailConfigName(appDefinition, instance));
        return sessionData;
    }

    protected static String getUID(int uid) {
        return uid < 0 ? DEFAULT_UID : String.valueOf(uid);
    }
}
