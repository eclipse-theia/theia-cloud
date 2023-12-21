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
package org.eclipse.theia.cloud.operator.handler;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;

public interface TimeoutStrategy {

    String getName();

    boolean evaluate(String correlationId, Session session, Instant now, Integer limit);

    static class FixedTime implements TimeoutStrategy {

	private static final String FIXEDTIME = "FIXEDTIME";
	private static final Logger LOGGER = LogManager.getLogger(FixedTime.class);

	@Override
	public String getName() {
	    return FIXEDTIME;
	}

	@Override
	public boolean evaluate(String correlationId, Session session, Instant now, Integer limit) {
	    String creationTimestamp = session.getMetadata().getCreationTimestamp();
	    Instant parse = Instant.parse(creationTimestamp);
	    long minutesSinceCreation = ChronoUnit.MINUTES.between(parse, now);
	    LOGGER.trace(formatLogMessage(FIXEDTIME, correlationId, "Checking " + session.getSpec().getName()
		    + ". minutesSinceLastActivity: " + minutesSinceCreation + ". limit: " + limit));
	    return minutesSinceCreation > limit;
	}

    }
}
