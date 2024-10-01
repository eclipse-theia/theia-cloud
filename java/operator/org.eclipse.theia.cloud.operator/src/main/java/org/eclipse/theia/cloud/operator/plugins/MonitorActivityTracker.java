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
package org.eclipse.theia.cloud.operator.plugins;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;
import static org.eclipse.theia.cloud.common.util.LogMessageUtil.generateCorrelationId;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.OperatorStatus;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.operator.TheiaCloudOperatorArguments;
import org.eclipse.theia.cloud.operator.messaging.MonitorMessagingService;

import com.google.inject.Inject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MonitorActivityTracker implements OperatorPlugin {

    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    private static final Logger LOGGER = LogManager.getLogger(MonitorActivityTracker.class);

    private static final String MONITOR_BASE_PATH = "/monitor";
    private static final String ACTIVITY_TRACKER_BASE_PATH = "/activity";
    private static final String GET_ACTIVITY = "/lastActivity";
    private static final String POST_POPUP = "/popup";
    private static final String COR_ID_NOACTIVITYPREFIX = "no-activity-";

    @Inject
    private TheiaCloudClient resourceClient;

    @Inject
    private MonitorMessagingService messagingService;

    @Inject
    private TheiaCloudOperatorArguments arguments;

    @Override
    public void start() {
        if (arguments.isEnableMonitor() && arguments.isEnableActivityTracker()) {
            int interval = arguments.getMonitorInterval();
            LOGGER.info("Launching Monitor service with interval of " + interval + " minutes");
            EXECUTOR.scheduleWithFixedDelay(this::pingAllSessions, 0, interval, TimeUnit.MINUTES);
        }
    }

    protected void pingAllSessions() {
        // Only look at handled sessions (handled sessions have a lastActivity)
        List<Session> sessions = resourceClient.sessions().list().stream()
                .filter(session -> OperatorStatus.HANDLED.equals(session.getStatus().getOperatorStatus())).toList();
        String correlationId = generateCorrelationId();

        LOGGER.debug("Pinging sessions: " + sessions);

        for (Session session : sessions) {
            Optional<String> sessionIP = resourceClient.getClusterIPFromSessionName(session.getSpec().getName());
            if (sessionIP.isPresent()) {
                String appDefinitionName = session.getSpec().getAppDefinition();
                Optional<AppDefinition> appDefinitionOptional = resourceClient.appDefinitions().get(appDefinitionName);
                if (appDefinitionOptional.isPresent()) {
                    AppDefinition appDefinition = appDefinitionOptional.get();
                    int timeoutAfter = appDefinition.getSpec().getMonitor().getActivityTracker().getTimeoutAfter();
                    int notifyAfter = appDefinition.getSpec().getMonitor().getActivityTracker().getNotifyAfter();
                    int port = appDefinition.getSpec().getMonitor().getPort();

                    pingSession(correlationId, session, sessionIP.get(), port, timeoutAfter, notifyAfter);
                }
            } else {
                LOGGER.error("No ClusterIP found for session " + session.getSpec().getName());
            }
        }
    }

    protected void pingSession(String correlationId, Session session, String sessionURL, int port, int shutdownAfter,
            int notifyAfter) {
        String sessionName = session.getSpec().getName();
        logInfo(sessionName, "Pinging session at " + sessionURL);
        OkHttpClient client = new OkHttpClient().newBuilder().build();

        String getActivityURL = getURL(sessionURL, port, GET_ACTIVITY);
        logInfo(sessionName, "GET " + getActivityURL);

        Request getActivityRequest = new Request.Builder().url(getActivityURL)
                .addHeader("Authorization", "Bearer " + session.getSpec().getSessionSecret()).get().build();
        Response getActivityResponse;
        try {
            getActivityResponse = client.newCall(getActivityRequest).execute();
            ResponseBody body = getActivityResponse.body();
            if (getActivityResponse.code() == 200 && body != null) {
                long lastReportedMilliseconds = Long.valueOf(body.string());

                session = updateLastActivity(correlationId, session, lastReportedMilliseconds);
            } else {
                logInfo(sessionName,
                        "REQUEST FAILED (Returned " + getActivityResponse.code() + ": " + "GET " + getActivityURL);
            }
        } catch (IOException e) {
            logInfo(sessionName, "REQUEST FAILED: " + "GET " + getActivityURL + ". Error: " + e);

        }
        Date lastActivityDate = new Date(session.getNonNullStatus().getLastActivity());
        Date currentDate = new Date(OffsetDateTime.now(ZoneOffset.UTC).toInstant().toEpochMilli());

        long minutesPassed = getMinutesPassed(lastActivityDate, currentDate);

        String minutes = minutesPassed == 1 ? "minute" : "minutes";
        logInfo(sessionName, "Last reported activity was: " + formatDate(lastActivityDate) + " (" + minutesPassed + " "
                + minutes + " ago)");

        if (minutesPassed < shutdownAfter) {
            if (minutesPassed >= notifyAfter) {
                logInfo(sessionName, "Notifying session as timeout of " + notifyAfter + " minutes was reached!");
                String postPopupURL = getURL(sessionURL, port, POST_POPUP);
                logInfo(sessionName, "POST " + postPopupURL);
                MediaType mediaType = MediaType.parse("text/plain");
                RequestBody body = RequestBody.create(mediaType, "");
                Request postRequest = new Request.Builder().url(postPopupURL)
                        .addHeader("Authorization", "Bearer " + session.getSpec().getSessionSecret()).post(body)
                        .build();
                try {
                    client.newCall(postRequest).execute();
                } catch (IOException e) {
                    logInfo(sessionName, "REQUEST FAILED: " + "POST " + postPopupURL);
                }
            }
        } else {
            // Timeout reached
            stopNonActiveSession(correlationId, session, shutdownAfter);
        }
    }

    protected Session updateLastActivity(String correlationId, Session session, long reportedTimestamp) {
        long currentTimestamp = session.getNonNullStatus().getLastActivity();
        if (currentTimestamp < reportedTimestamp) {
            session = resourceClient.sessions().updateStatus(correlationId, session,
                    status -> status.setLastActivity(reportedTimestamp));
        }
        return session;
    }

    protected long getMinutesPassed(Date lastActivity, Date currentTime) {
        long timePassed = currentTime.getTime() - lastActivity.getTime();
        return TimeUnit.MILLISECONDS.toMinutes(timePassed);
    }

    protected void stopNonActiveSession(String correlationId, Session session, int shutdownAfter) {
        String sessionName = session.getSpec().getName();
        try {
            this.messagingService.sendTimeoutMessage(session,
                    "Timeout of " + shutdownAfter + " minutes of inactivity was reached!");
            logInfo(sessionName, "Deleting session as timeout of " + shutdownAfter + " minutes was reached!");
            resourceClient.sessions().delete(COR_ID_NOACTIVITYPREFIX + correlationId, sessionName);
        } catch (Exception e) {
            LOGGER.error(formatLogMessage(COR_ID_NOACTIVITYPREFIX, correlationId, "Exception trying to delete session"),
                    e);
        }
    }

    protected void logInfo(String sessionName, String message) {
        LOGGER.info("[" + sessionName + "] " + message);
    }

    protected String getURL(String sessionUrl, int port, String endpoint) {
        return "http://" + sessionUrl + ":" + port + MONITOR_BASE_PATH + ACTIVITY_TRACKER_BASE_PATH + endpoint;
    }

    protected String formatDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(date);
    }
}
