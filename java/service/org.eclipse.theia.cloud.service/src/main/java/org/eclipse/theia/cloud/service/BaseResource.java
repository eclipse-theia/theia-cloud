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
package org.eclipse.theia.cloud.service;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.generateCorrelationId;

import org.eclipse.theia.cloud.common.util.LogMessageUtil;
import org.eclipse.theia.cloud.common.util.TheiaCloudError;
import org.jboss.logging.Logger;

public class BaseResource {
    private static final String THEIA_CLOUD_APP_ID = "theia.cloud.app.id";

    protected final Logger logger;
    protected final String appId;

    public BaseResource() {
	appId = System.getProperty(THEIA_CLOUD_APP_ID, "asdfghjkl");
	logger = Logger.getLogger(getClass().getSuperclass());
    }

    protected String evaluateRequest(ServiceRequest request) {
	String correlationId = generateCorrelationId();
	if (request == null || request.appId == null || !request.appId.equals(appId)) {
	    String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
	    info(correlationId, "Callling '" + methodName + "' without matching appId: " + request.appId);
	    throw new TheiaCloudWebException(TheiaCloudError.INVALID_APP_ID);
	}
	return correlationId;
    }

    public void info(String correlationId, String message) {
	logger.info(LogMessageUtil.formatLogMessage(correlationId, message));
    }

    public void warn(String correlationId, String message) {
	logger.warn(LogMessageUtil.formatLogMessage(correlationId, message));
    }

    public void error(String correlationId, String message) {
	logger.error(LogMessageUtil.formatLogMessage(correlationId, message));
    }

    public void error(String correlationId, String message, Throwable throwable) {
	logger.error(LogMessageUtil.formatLogMessage(correlationId, message), throwable);
    }

    public void trace(String correlationId, String message) {
	logger.trace(LogMessageUtil.formatLogMessage(correlationId, message));
    }
}
