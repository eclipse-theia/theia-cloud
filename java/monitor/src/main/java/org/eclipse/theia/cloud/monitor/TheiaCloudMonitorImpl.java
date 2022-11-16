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
package org.eclipse.theia.cloud.monitor;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;
import static org.eclipse.theia.cloud.common.util.LogMessageUtil.generateCorrelationId;

import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TheiaCloudMonitorImpl implements TheiaCloudMonitor {

    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    private static final Logger LOGGER = LogManager.getLogger(TheiaCloudMonitorImpl.class);

    private static final String GET_ACTIVITY = "/activity";
    private static final String POST_POPUP = "/popup";
    private static final String COR_ID_NOACTIVITYPREFIX = "no-activity-";

    private int port;
    private int shutdownAfter;
    private int notifyAfter;

    @Override
    public void start(int port, int interval, int shutdownAfter, int notifyAfter) {
	this.port = port;
	this.shutdownAfter = shutdownAfter;
	this.notifyAfter = notifyAfter;
	EXECUTOR.scheduleWithFixedDelay(this::pingAllSessions, 0, interval, TimeUnit.MINUTES);
    }

    protected void pingAllSessions() {
//	List<Session> sessions = resourceClient.sessions().list();
//
//	LOGGER.debug("Pinging sessions: " + sessions);
//
//	for (Session session : sessions) {
//	    String urlString = session.getSpec().getUrl();
//	    String name = session.getSpec().getName();
//	    pingSession(name, urlString);
//	}

	pingSession("Test Session", "localhost");
    }

    protected void pingSession(String sessionName, String sessionURL) {
	logInfo(sessionName, "Pinging session at " + sessionURL);
	OkHttpClient client = new OkHttpClient().newBuilder().build();

	try {
	    String getActivityURL = getURL(sessionURL, GET_ACTIVITY);
	    logInfo(sessionName, "GET " + getActivityURL);

	    Request getActivityRequest = new Request.Builder().url(getActivityURL).method("GET", null).build();
	    Response getActivityResponse = client.newCall(getActivityRequest).execute();

	    if (getActivityResponse.code() == 200) {

		long lastReportedMilliseconds = Long.valueOf(getActivityResponse.body().string());

		Date lastActivityDate = new Date(lastReportedMilliseconds);
		Date currentDate = new Date(OffsetDateTime.now(ZoneOffset.UTC).toInstant().toEpochMilli());

		long minutesPassed = getMinutesPassed(lastActivityDate, currentDate);

		String minutes = minutesPassed == 1 ? "minute" : "minutes";
		logInfo(sessionName, "Last reported activity was: " + formatDate(lastActivityDate) + " ("
			+ minutesPassed + " " + minutes + " ago)");

		if (!(minutesPassed >= this.shutdownAfter)) {
		    if (minutesPassed >= this.notifyAfter) {
			logInfo(sessionName,
				"Notifying session as timeout of " + notifyAfter + " minutes was reached!");
			String postPopupURL = getURL(sessionURL, POST_POPUP);
			logInfo(sessionName, "POST " + postPopupURL);
			MediaType mediaType = MediaType.parse("text/plain");
			RequestBody body = RequestBody.create(mediaType, "");
			Request postRequest = new Request.Builder().url(postPopupURL).method("POST", body).build();
			client.newCall(postRequest).execute();
		    }
		} else {
		    // Timeout reached
		    stopNonActiveSession(sessionName);
		}
	    } else {
		// Response of the GET request was unsuccessful
		stopNonActiveSession(sessionName);
	    }
	} catch (Exception e) {
	    // Some issue with connecting to the pod, shut it down
	    LOGGER.error("Could not connect to pod");
	    stopNonActiveSession(sessionName);
	}
    }

    protected long getMinutesPassed(Date lastActivity, Date currentTime) {
	long timePassed = currentTime.getTime() - lastActivity.getTime();
	return TimeUnit.MILLISECONDS.toMinutes(timePassed);
    }

    protected void stopNonActiveSession(String sessionName) {
	String correlationId = generateCorrelationId();

	try {
	    logInfo(sessionName, "Deleting session as timeout of " + shutdownAfter + " minutes was reached!");
	    // resourceClient.sessions().delete(COR_ID_NOACTIVITYPREFIX + correlationId,
	    // sessionName);
	} catch (Exception e) {
	    LOGGER.error(formatLogMessage(COR_ID_NOACTIVITYPREFIX, correlationId, "Exception trying to delete session"),
		    e);
	}
    }

    protected void logInfo(String sessionName, String message) {
	System.out.println("[" + sessionName + "] " + message);
    }

    protected String getURL(String sessionUrl, String endpoint) {
	return "http://" + sessionUrl + ":" + this.port + endpoint;
    }

    protected String formatDate(Date date) {
	return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(date);
    }
}
