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

import org.eclipse.theia.cloud.common.util.LogMessageUtil;
import org.jboss.logging.Logger;

import io.quarkus.security.identity.SecurityIdentity;

public class BaseResource {
    private static final String THEIA_CLOUD_APP_ID = "theia.cloud.app.id";
    private static final String THEIA_CLOUD_USE_KEYCLOAK = "theia.cloud.use.keycloak";

    protected final Logger logger;
    protected final String appId;
    protected boolean useKeycloak;

    public BaseResource() {
	appId = System.getProperty(THEIA_CLOUD_APP_ID, "asdfghjkl");
	useKeycloak = Boolean.valueOf(System.getProperty(THEIA_CLOUD_USE_KEYCLOAK, "false"));
	logger = Logger.getLogger(getClass().getSuperclass());
    }

    protected boolean isValidRequest(ServiceRequest request) {
	return request != null && request.appId != null && request.appId.equals(appId);
    }

    protected boolean isAuthenticated(String correlationId, ServiceRequest request, SecurityIdentity identity) {
	if (!useKeycloak) {
	    trace(correlationId, "Keycloak disabled. All calls are accepted.");
	    return true;
	}
	if (identity == null) {
	    error(correlationId, "SecurityIdentity is null. Request can't be checked for authentication.");
	    return false;
	}
	boolean isAuthenticatedCall = !identity.isAnonymous();
	info(correlationId, "Authenticated: " + isAuthenticatedCall + " for request " + request);
	return isAuthenticatedCall;
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
