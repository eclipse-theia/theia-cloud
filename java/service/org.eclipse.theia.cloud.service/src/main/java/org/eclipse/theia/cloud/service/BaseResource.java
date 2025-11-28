/********************************************************************************
 * Copyright (C) 2022-2023 EclipseSource and others.
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

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;

public class BaseResource {

    protected final Logger logger;
    protected final String appId;
    private ApplicationProperties applicationProperties;

    @Inject
    protected TheiaCloudUser theiaCloudUser;

    public BaseResource(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
        appId = applicationProperties.getServiceAuthToken();
        logger = Logger.getLogger(getClass().getSuperclass());
    }

    /** @return The correlation id for this request. */
    protected String evaluateRequest(ServiceRequest request) {
        return basicEvaluateRequest(request);
    }

    private String basicEvaluateRequest(ServiceRequest request) {
        String correlationId = generateCorrelationId();
        if (request == null || request.appId == null || !request.appId.equals(appId)) {
            info(correlationId, "Request '" + request.kind + "' without matching appId: " + request.appId);
            trace(correlationId, request.toString());
            throw new TheiaCloudWebException(TheiaCloudError.INVALID_APP_ID);
        }
        return correlationId;
    }

    /**
     * Evaluates user scoped requests. In addition to the basic evaluation performed for every
     * {@linkplain ServiceRequest}, this ensures that the requested user is the same as the authenticated user. In
     * anonymous mode, this ensures a user is set in the request.
     * 
     * @param request The {@link UserScopedServiceRequest} to evaluate
     * @throws TheiaCloudWebException
     * @return The {@link EvaluatedRequest} including the user identifier to use
     */
    protected EvaluatedRequest evaluateRequest(UserScopedServiceRequest request) {
        String correlationId = basicEvaluateRequest(request);

        // Keycloak is not used => user must be specified in request.
        if (!applicationProperties.isUseKeycloak()) {
            if (request.user == null || request.user.isBlank()) {
                info(correlationId,
                        "User was not specified for user scoped request. This is mandatory in anonymous mode.");
                throw new TheiaCloudWebException(Status.BAD_REQUEST,
                        "Property \"user\" was not specified for user scoped request.");
            }
            return new EvaluatedRequest(correlationId, request.user);
        }

        // Keycloak is used. User must not be considered anonymous (i.e. have a
        // non-empty identifier) and the request user must match the authenticated user
        // (this might change if the concept of admin users is introduced later)
        if (theiaCloudUser.isAnonymous()) {
            info(correlationId,
                    "User is unexpectedly considered anonymous and, thus, must not access user scoped resources.");
            throw new TheiaCloudWebException(Status.UNAUTHORIZED);
        } else if (request.user == null || request.user.equals(theiaCloudUser.getIdentifier())) {
            return new EvaluatedRequest(correlationId, theiaCloudUser.getIdentifier());
        } else {
            info(correlationId, "User specified in the request does not match the authenticated user.");
            trace(correlationId, request.toString());
            throw new TheiaCloudWebException(Status.FORBIDDEN);
        }
    }

    public void info(String correlationId, String message) {
        logger.info(LogMessageUtil.formatLogMessage(correlationId, message));
    }

    public void warn(String correlationId, String message) {
        logger.warn(LogMessageUtil.formatLogMessage(correlationId, message));
    }

    public void warn(String correlationId, String message, Throwable throwable) {
        logger.warn(LogMessageUtil.formatLogMessage(correlationId, message), throwable);
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

    public void trace(String correlationId, String message, Throwable throwable) {
        logger.trace(LogMessageUtil.formatLogMessage(correlationId, message), throwable);
    }
}
