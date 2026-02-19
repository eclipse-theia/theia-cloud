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
package org.eclipse.theia.cloud.operator.handler;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;
import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatMetric;
import static org.eclipse.theia.cloud.operator.util.TheiaCloudDeploymentUtil.HOST_PROTOCOL;

import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.SessionResourceClient;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.util.LogMessageUtil;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapEnvSource;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.SecretEnvSource;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.sentry.ISpan;
import io.sentry.SpanStatus;
import org.eclipse.theia.cloud.common.tracing.Tracing;
import org.eclipse.theia.cloud.common.tracing.TraceContext;

public final class AddedHandlerUtil {

    private static final Logger LOGGER = LogManager.getLogger(AddedHandlerUtil.class);

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    public static final String TEMPLATE_SERVICE_YAML = "/templateService.yaml";
    public static final String TEMPLATE_SERVICE_WITHOUT_AOUTH2_PROXY_YAML = "/templateServiceWithoutOAuthProxy.yaml";
    public static final String TEMPLATE_INTERNAL_SERVICE_YAML = "/templateInternalService.yaml";
    public static final String TEMPLATE_CONFIGMAP_EMAILS_YAML = "/templateConfigmapEmails.yaml";
    public static final String TEMPLATE_CONFIGMAP_YAML = "/templateConfigmap.yaml";
    public static final String TEMPLATE_DEPLOYMENT_YAML = "/templateDeployment.yaml";
    public static final String TEMPLATE_DEPLOYMENT_WITHOUT_AOUTH2_PROXY_YAML = "/templateDeploymentWithoutOAuthProxy.yaml";

    public static final String OAUTH2_PROXY_CFG = "oauth2-proxy.cfg";

    public static final String OAUTH2_PROXY_CONFIGMAP_NAME = "oauth2-proxy-config";

    public static final String CONFIGMAP_DATA_PLACEHOLDER_HOST = "https://placeholder";
    public static final String CONFIGMAP_DATA_PLACEHOLDER_PORT = "placeholder-port";

    public static final String FILENAME_AUTHENTICATED_EMAILS_LIST = "authenticated-emails-list";

    public static final String INGRESS_REWRITE_PATH = "(/|$)(.*)";
    public static final String INGRESS_PATH_TYPE = "ImplementationSpecific";

    private static final HostnameVerifier ALL_GOOD_HOSTNAME_VERIFIER = new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    private static final X509TrustManager TRUST_ALL_MANAGER = new X509TrustManager() {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            /* no op */
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            /* no op */
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };

    private AddedHandlerUtil() {

    }

    public static void updateProxyConfigMap(NamespacedKubernetesClient client, String namespace, ConfigMap configMap,
            String host, int port) {
        ConfigMap templateConfigMap = client.configMaps().inNamespace(namespace).withName(OAUTH2_PROXY_CONFIGMAP_NAME)
                .get();
        Map<String, String> data = new LinkedHashMap<>(templateConfigMap.getData());
        data.put(OAUTH2_PROXY_CFG, data.get(OAUTH2_PROXY_CFG)//
                .replace(CONFIGMAP_DATA_PLACEHOLDER_HOST, HOST_PROTOCOL + host)//
                .replace(CONFIGMAP_DATA_PLACEHOLDER_PORT, String.valueOf(port)));
        configMap.setData(data);
    }

    public static void updateSessionURLAsync(SessionResourceClient sessions, Session session, String namespace,
            String url, String correlationId, ISpan parentSpan) {
        String sessionName = session.getSpec().getName();
        String appDef = session.getSpec().getAppDefinition();

        // Extract trace context BEFORE scheduling - the parent span will be finished by the time executor runs
        java.util.Optional<TraceContext> traceContext = 
            TraceContext.fromSpan(parentSpan);

        EXECUTOR.execute(() -> {
            // Create a new transaction linked to the same trace (parent span is already finished)
            ISpan span = Tracing.continueTraceAsync(traceContext, "session.url_availability", "URL availability check");
            span.setTag("session.name", sessionName);
            span.setTag("app_definition", appDef);
            span.setData("correlation_id", correlationId);
            span.setData("url", url);

            boolean updateURL = false;
            int lastResponseCode = -1;
            String failureReason = null;

            try {
                for (int i = 1; i <= 309; i++) {
                    /*
                     * On the first 60 loops we will check every 500ms whether URL is available. This will take at
                     * least 30s. On the next 60 loops we will check every 1000ms. This will take at least 60s. On
                     * the next 24 loops we will check every 2500ms. This will take at least further 60s. On the
                     * next 60 loops we will check every 5000ms. This will take at least further 5 minutes. If the
                     * pod has not started within the first ~7 minutes, we will continue to check every 30 seconds.
                     * We give up after an hour.
                     */
                    long sleepDuration;
                    if (i <= 60) {
                        sleepDuration = 500;
                    } else if (i <= 120) {
                        sleepDuration = 1000;
                    } else if (i <= 144) {
                        sleepDuration = 2500;
                    } else if (i <= 204) {
                        sleepDuration = 5000;
                    } else {
                        sleepDuration = 30000;
                    }
                    
                    try {
                        Thread.sleep(sleepDuration);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        span.setTag("outcome", "cancelled");
                        Tracing.finishError(span, e);
                        return;
                    }

                    HttpsURLConnection connection;
                    try {
                        connection = (HttpsURLConnection) new URL(HOST_PROTOCOL + url).openConnection();
                    } catch (IOException e) {
                        LOGGER.error(formatLogMessage(correlationId, "Error while checking session availability."), e);
                        failureReason = "connection_error";
                        continue;
                    }
                    int code;

                    try {
                        connection.setHostnameVerifier(ALL_GOOD_HOSTNAME_VERIFIER);
                        SSLContext sc = SSLContext.getInstance("SSL");
                        sc.init(null, new TrustManager[] { TRUST_ALL_MANAGER }, new java.security.SecureRandom());
                        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                        connection.setSSLSocketFactory(sc.getSocketFactory());
                        connection.connect();
                        code = connection.getResponseCode();
                        lastResponseCode = code;
                    } catch (IOException e) {
                        LOGGER.error(formatLogMessage(correlationId, url + " is NOT available yet."), e);
                        failureReason = "io_error";
                        continue;
                    } catch (NoSuchAlgorithmException | KeyManagementException e) {
                        LOGGER.error(formatLogMessage(correlationId,
                                "Error while checking session availability with SSL ignore."), e);
                        failureReason = "ssl_error";
                        continue;
                    }

                    LOGGER.trace(formatLogMessage(correlationId, url + " has response code " + code));

                    if (code == 200) {
                        updateURL = true;
                    } else if (code != 404 && code != 503 && !updateURL) {
                        /*
                         * we don't get a 404 or 503, so something is available. Try accessing the URL once more then
                         * update URL anyway
                         */
                        updateURL = true;
                        continue;
                    }

                    if (updateURL) {
                        LOGGER.info(formatLogMessage(correlationId, url + " is available."));

                        ISpan updateSpan = Tracing.childSpan(span, "session.update_status", "Update session URL status");
                        try {
                            sessions.updateStatus(correlationId, session, status -> status.setUrl(url));
                            Tracing.finishSuccess(updateSpan);
                        } catch (Exception e) {
                            Tracing.finishError(updateSpan, e);
                            // Propagate failure to outer span
                            span.setTag("outcome", "error");
                            span.setData("attempts", i);
                            span.setData("final_response_code", code);
                            Tracing.finishError(span, e);
                            return;
                        }

                        LOGGER.info(formatMetric(correlationId, "Running session for " + appDef));

                        span.setTag("outcome", "success");
                        span.setData("attempts", i);
                        span.setData("final_response_code", code);
                        Tracing.finishSuccess(span);
                        return;
                    } else {
                        LOGGER.trace(formatLogMessage(correlationId, url + " is NOT available yet."));
                    }
                }

                // Exhausted all attempts
                span.setTag("outcome", "timeout");
                span.setData("attempts", 309);
                span.setData("last_response_code", lastResponseCode);
                if (failureReason != null) {
                    span.setTag("failure.reason", failureReason);
                }
                Tracing.finish(span, SpanStatus.DEADLINE_EXCEEDED);

            } catch (Exception e) {
                span.setTag("outcome", "error");
                Tracing.finishError(span, e);
            }
        });
    }

    public static void removeEmptyResources(Deployment deployment) {
        for (Container container : deployment.getSpec().getTemplate().getSpec().getContainers()) {
            ResourceRequirements resources = container.getResources();
            if (resources == null) {
                continue;
            }
            Map<String, Quantity> limits = resources.getLimits();
            if (limits != null) {
                Set<String> toRemove = new LinkedHashSet<>();
                for (String key : limits.keySet()) {
                    Quantity quantity = limits.get(key);
                    if (quantity == null) {
                        toRemove.add(key);
                    }
                }
                toRemove.forEach(limits::remove);
            }
            Map<String, Quantity> requests = resources.getRequests();
            if (requests != null) {
                Set<String> toRemove = new LinkedHashSet<>();
                for (String key : requests.keySet()) {
                    Quantity quantity = requests.get(key);
                    if (quantity == null) {
                        toRemove.add(key);
                    }
                }
                toRemove.forEach(requests::remove);
            }
        }
    }

    public static void addImagePullSecret(Deployment deployment, String secret) {
        List<LocalObjectReference> imagePullSecrets = deployment.getSpec().getTemplate().getSpec()
                .getImagePullSecrets();
        if (imagePullSecrets == null) {
            imagePullSecrets = new ArrayList<LocalObjectReference>();
            deployment.getSpec().getTemplate().getSpec().setImagePullSecrets(imagePullSecrets);
        }
        imagePullSecrets.add(new LocalObjectReference(secret));
    }

    /* ------------------- Addition of env vars to Deployments ------------------ */
    public static void addCustomEnvVarsToDeploymentFromSession(String correlationId, Deployment deployment,
            Session session, AppDefinition appDefinition) {
        String containerName = appDefinition.getSpec().getName();
        Optional<Integer> maybeContainerIdx = findContainerIdxInDeployment(deployment, containerName);

        if (maybeContainerIdx.isEmpty()) {
            LOGGER.error(LogMessageUtil.formatLogMessage(correlationId,
                    "Trying to add custom env vars to Deployment from Session. Could not find the container "
                            + containerName + " in Deployment named" + deployment.getMetadata().getName()));
            return;
        }
        int containerIdx = maybeContainerIdx.get();
        Container container = deployment.getSpec().getTemplate().getSpec().getContainers().get(containerIdx);

        container = withDirectEnvVarsToContainer(container, session.getSpec().getEnvVars());
        container = withEnvVarsFromRefsToContainer(container, session.getSpec().getEnvVarsFromConfigMaps(), false);
        container = withEnvVarsFromRefsToContainer(container, session.getSpec().getEnvVarsFromSecrets(), true);

        deployment.getSpec().getTemplate().getSpec().getContainers().set(containerIdx, container);
    }

    private static Container withEnvVarsFromRefsToContainer(Container container, List<String> refs,
            boolean isFromSecret) {
        if (refs == null || refs.size() == 0)
            return container;

        List<EnvFromSource> newEnvFromSources = refs.stream()
                .map((ref) -> isFromSecret ? new EnvFromSource(null, null, new SecretEnvSource(ref, null))
                        : new EnvFromSource(new ConfigMapEnvSource(ref, null), null, null))
                .collect(Collectors.toList());

        List<EnvFromSource> combinedEnvVarFromSources = new LinkedList<>();
        combinedEnvVarFromSources.addAll(container.getEnvFrom());
        combinedEnvVarFromSources.addAll(newEnvFromSources);

        container.setEnvFrom(combinedEnvVarFromSources);

        return container;
    }

    private static Container withDirectEnvVarsToContainer(Container container, Map<String, String> mapEnvVars) {
        if (mapEnvVars == null || mapEnvVars.size() == 0)
            return container;

        List<EnvVar> newEnvVars = mapEnvVars.entrySet().stream()
                .map((kv) -> new EnvVar(kv.getKey(), kv.getValue(), null)).collect(Collectors.toList());

        List<EnvVar> combinedEnvVars = new LinkedList<>();
        combinedEnvVars.addAll(container.getEnv());
        combinedEnvVars.addAll(newEnvVars);

        container.setEnv(combinedEnvVars);

        return container;
    }

    private static Optional<Integer> findContainerIdxInDeployment(Deployment deployment, String containerName) {
        List<Container> containers = deployment.getSpec().getTemplate().getSpec().getContainers();
        for (int i = 0; i < containers.size(); i++) {
            if (containers.get(i).getName().equals(containerName)) {
                return Optional.of(i);
            }
        }

        return Optional.empty();
    }

}
