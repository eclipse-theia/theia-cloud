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
package org.eclipse.theia.cloud.operator.messaging;

import java.io.IOException;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.util.SessionUtil;
import org.eclipse.theia.cloud.operator.TheiaCloudOperatorArguments;
import org.json.JSONObject;

import com.google.inject.Inject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class MonitorMessagingServiceImpl implements MonitorMessagingService {

    private static final String MONITOR_BASE_PATH = "/monitor";
    private static final String POST_MESSAGE = "/message";

    private static final Logger LOGGER = LogManager.getLogger(MonitorMessagingServiceImpl.class);

    @Inject
    private TheiaCloudClient resourceClient;

    @Inject
    private TheiaCloudOperatorArguments arguments;

    @Override
    public void sendMessage(Session session, String level, String message) {
        if (isEnabled()) {
            postMessage(session, level, message, true, "");
        }

    }

    @Override
    public void sendFullscreenMessage(Session session, String level, String message, String detail) {
        if (isEnabled()) {
            postMessage(session, level, message, true, detail);
        }

    }

    @Override
    public void sendTimeoutMessage(Session session, String detail) {
        if (isEnabled()) {
            postMessage(session, "info", "Your session has timed out!", true, detail);
        }
    }

    protected void postMessage(Session session, String level, String message, boolean fullscreen, String detail) {
        OkHttpClient client = new OkHttpClient().newBuilder().build();

        Optional<String> postMessageURL = getURL(session);
        if (postMessageURL.isPresent()) {
            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            String json = new JSONObject().put("message", message).put("level", level).put("fullscreen", fullscreen)
                    .put("detail", detail).toString();

            RequestBody body = RequestBody.create(mediaType, json);
            Request postRequest = new Request.Builder().url(postMessageURL.get())
                    .addHeader("Authorization", "Bearer " + session.getSpec().getSessionSecret()).method("POST", body)
                    .build();
            try {
                client.newCall(postRequest).execute();
            } catch (IOException e) {
                LOGGER.info("[" + session.getSpec().getName() + "] Could not send message to extension:" + e);
            }
        }
    }

    protected Optional<String> getURL(Session session) {
        Optional<String> ip = SessionUtil.getClusterIP(resourceClient, session);
        Optional<Integer> port = getPort(session);
        if (ip.isPresent() && port.isPresent()) {
            return Optional.of("http://" + ip.get() + ":" + port.get() + MONITOR_BASE_PATH + POST_MESSAGE);
        }
        return Optional.empty();
    }

    protected Optional<Integer> getPort(Session session) {
        String appDefinitionId = session.getSpec().getAppDefinition();
        return resourceClient.appDefinitions().get(appDefinitionId)//
                .map(AppDefinition::getSpec)//
                .map(AppDefinitionSpec::getMonitor)//
                .map(AppDefinitionSpec.Monitor::getPort);
    }

    protected boolean isEnabled() {
        return arguments.isEnableMonitor();
    }

}
