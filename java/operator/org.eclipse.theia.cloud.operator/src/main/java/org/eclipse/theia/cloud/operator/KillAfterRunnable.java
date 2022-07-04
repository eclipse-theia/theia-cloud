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
package org.eclipse.theia.cloud.operator;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;
import static org.eclipse.theia.cloud.common.util.LogMessageUtil.generateCorrelationId;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.resource.Session;
import org.eclipse.theia.cloud.common.k8s.resource.SessionSpecResourceList;
import org.eclipse.theia.cloud.operator.resource.AppDefinitionSpecResource;
import org.eclipse.theia.cloud.operator.resource.AppDefinitionSpecResourceList;

import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public final class KillAfterRunnable implements Runnable {

    private static final String COR_ID_KILLPREFIX = "kill-after-";

    private static final Logger LOGGER = LogManager.getLogger(KillAfterRunnable.class);

    private NonNamespaceOperation<AppDefinitionSpecResource, AppDefinitionSpecResourceList, Resource<AppDefinitionSpecResource>> appDefinitionResourceClient;
    private NonNamespaceOperation<Session, SessionSpecResourceList, Resource<Session>> sessionResourceClient;

    public KillAfterRunnable(
	    NonNamespaceOperation<AppDefinitionSpecResource, AppDefinitionSpecResourceList, Resource<AppDefinitionSpecResource>> appDefinitionResourceClient,
	    NonNamespaceOperation<Session, SessionSpecResourceList, Resource<Session>> sessionResourceClient) {
	this.appDefinitionResourceClient = appDefinitionResourceClient;
	this.sessionResourceClient = sessionResourceClient;
    }

    @Override
    public void run() {
	String correlationId = generateCorrelationId();

	try {
	    Map<String, Integer> killAfterMap = new LinkedHashMap<>();
	    for (AppDefinitionSpecResource appDefinition : appDefinitionResourceClient.list().getItems()) {
		String appDefinitionName = appDefinition.getSpec().getName();
		int killAfter = appDefinition.getSpec().getKillAfter();
		if (killAfter < 1) {
		    LOGGER.trace(formatLogMessage(COR_ID_KILLPREFIX, correlationId,
			    "App Definition " + appDefinitionName + " sessions are not killed."));
		} else {
		    LOGGER.trace(formatLogMessage(COR_ID_KILLPREFIX, correlationId, "App Definition "
			    + appDefinitionName + " sessions will be killed after " + killAfter + " minutes."));
		    killAfterMap.put(appDefinitionName, killAfter);
		}
	    }

	    Set<String> sessionsToKill = new LinkedHashSet<>();

	    Instant now = Instant.now();
	    for (Session session : sessionResourceClient.list().getItems()) {
		String appDefinitionName = session.getSpec().getAppDefinition();
		if (killAfterMap.containsKey(appDefinitionName)) {
		    Integer killAfter = killAfterMap.get(appDefinitionName);

		    String creationTimestamp = session.getMetadata().getCreationTimestamp();
		    Instant parse = Instant.parse(creationTimestamp);
		    long minutesSinceCreation = ChronoUnit.MINUTES.between(parse, now);
		    if (minutesSinceCreation > killAfter) {
			LOGGER.info(formatLogMessage(COR_ID_KILLPREFIX, correlationId,
				"Session " + session.getSpec().getName() + " WILL be killed. KilledAfter: " + killAfter
					+ " ; since creation: " + minutesSinceCreation));
			sessionsToKill.add(session.getMetadata().getName());
		    } else {
			LOGGER.trace(formatLogMessage(COR_ID_KILLPREFIX, correlationId,
				"Session " + session.getSpec().getName() + " will not be killed. KilledAfter: "
					+ killAfter + " ; since creation: " + minutesSinceCreation));
		    }
		} else {
		    LOGGER.trace(formatLogMessage(COR_ID_KILLPREFIX, correlationId,
			    "Session " + session.getSpec().getName() + " will not be killed at all."));
		}
	    }

	    for (String sessionName : sessionsToKill) {
		sessionResourceClient.withName(sessionName).delete();
	    }
	} catch (Exception e) {
	    LOGGER.error(formatLogMessage(COR_ID_KILLPREFIX, correlationId, "Exception in kill after runnable"), e);
	}
    }
}