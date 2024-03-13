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
package org.eclipse.theia.cloud.operator;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;
import static org.eclipse.theia.cloud.common.util.LogMessageUtil.generateCorrelationId;

import java.math.BigInteger;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.operator.util.TriConsumer;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;

final class SpecWatch<S extends CustomResource<?, ?>> implements Watcher<S> {

    private static final Logger LOGGER = LogManager.getLogger(SpecWatch.class);

    private static int MAX_RECONNECTION_TRIES = 10;
    private int reconnectionTries = 0;

    private final Map<String, S> cache;
    private final TriConsumer<Action, String, String> eventHandler;
    private final String resourceName;
    private final String correlationIdPrefix;

    private long lastActive;

    SpecWatch(Map<String, S> cache, TriConsumer<Action, String, String> eventHandler, String resourceName,
	    String correlationIdPrefix) {
	this.lastActive = System.currentTimeMillis();
	this.cache = cache;
	this.eventHandler = eventHandler;
	this.resourceName = resourceName;
	this.correlationIdPrefix = correlationIdPrefix;
    }

    @Override
    public void eventReceived(Action action, S resource) {
	lastActive = System.currentTimeMillis();
	if (reconnectionTries > 0) {
	    reconnectionTries = 0;
	    LOGGER.info(formatLogMessage(correlationIdPrefix,
		    getResourceName() + " did receive event. Resetting retry counter to 0."));
	}
	String correlationId = generateCorrelationId();
	String uid = resource.getMetadata().getUid();
	try {
	    LOGGER.trace(formatLogMessage(correlationIdPrefix, correlationId,
		    getResourceName() + " " + uid + " : received an event: " + action));
	    if (cache.containsKey(uid)) {
		LOGGER.trace(formatLogMessage(correlationIdPrefix, correlationId,
			getResourceName() + " " + uid + " : already known. Check if outdated event"));
		BigInteger knownResourceVersion = new BigInteger(cache.get(uid).getMetadata().getResourceVersion());
		BigInteger receivedResourceVersion = new BigInteger(resource.getMetadata().getResourceVersion());
		if (knownResourceVersion.compareTo(receivedResourceVersion) >= 1) {
		    LOGGER.info(formatLogMessage(correlationIdPrefix, correlationId,
			    getResourceName() + " " + uid + " : event is outdated"));
		    return;
		} else {
		    LOGGER.trace(formatLogMessage(correlationIdPrefix, correlationId,
			    getResourceName() + " " + uid + " : event is NOT outdated. Handle event"));
		}
	    }

	    if (action == Action.ADDED || action == Action.MODIFIED) {
		cache.put(uid, resource);
	    }
	    eventHandler.accept(action, uid, correlationId);
	    if (action == Action.DELETED) {
		cache.remove(uid);
	    }
	} catch (Exception e) {
	    LOGGER.error(formatLogMessage(correlationIdPrefix,
		    getResourceName() + " " + uid + " : error while handling event"), e);
	    System.exit(-1);
	}
    }

    @Override
    public void onClose(WatcherException cause) {
	lastActive = System.currentTimeMillis();
	LOGGER.error(formatLogMessage(correlationIdPrefix, getResourceName() + " watch closed because of an exception"),
		cause);
	System.exit(-1);
    }

    @Override
    public void onClose() {
	lastActive = System.currentTimeMillis();
	LOGGER.info(formatLogMessage(correlationIdPrefix, getResourceName() + " watch closed"));
	Watcher.super.onClose();
    }

    @Override
    public boolean reconnecting() {
	reconnectionTries++;
	if (reconnectionTries >= MAX_RECONNECTION_TRIES) {
	    LOGGER.info(formatLogMessage(correlationIdPrefix, getResourceName() + " did not reconnect after "
		    + MAX_RECONNECTION_TRIES + " tries. Restarting Operator."));
	    System.exit(-1);
	}
	lastActive = System.currentTimeMillis();
	LOGGER.info(
		formatLogMessage(correlationIdPrefix, getResourceName() + " reconnecting (" + reconnectionTries + ")"));
	return Watcher.super.reconnecting();
    }

    public String getResourceName() {
	return resourceName;
    }

    /**
     * @return the last timestamp when one of the {@link Watcher} actions was
     *         invoked
     */
    public long getLastActive() {
	return lastActive;
    }
}
