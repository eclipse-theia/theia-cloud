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

import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Injectable bean providing access to the service's configurable application properties.
 */
@ApplicationScoped
public class ApplicationProperties {

    private static final String THEIACLOUD_SERVICE_AUTH_TOKEN = "theia.cloud.service.auth.token";
    private static final String THEIACLOUD_APP_ID = "theia.cloud.app.id"; // Deprecated - use
                                                                          // THEIACLOUD_SERVICE_AUTH_TOKEN
    private static final String THEIACLOUD_USE_KEYCLOAK = "theia.cloud.use.keycloak";
    private static final String THEIACLOUD_ADMIN_GROUP_NAME = "theia.cloud.auth.admin.group";

    private static final String DEFAULT_ADMIN_GROUP_NAME = "theia-cloud/admin";

    private final Logger logger;

    private final boolean useKeycloak;
    private final String serviceAuthToken;
    private final String adminGroupName;

    public ApplicationProperties() {
        logger = Logger.getLogger(getClass());
        serviceAuthToken = getServiceAuthTokenWithFallback();
        adminGroupName = System.getProperty(THEIACLOUD_ADMIN_GROUP_NAME, DEFAULT_ADMIN_GROUP_NAME);
        // Only disable keycloak if the value was explicitly set to exactly "false".
        useKeycloak = !"false".equals(System.getProperty(THEIACLOUD_USE_KEYCLOAK));
        if (!useKeycloak) {
            logger.warn("Keycloak integration was disabled. Anonymous requests are allowed!");
        }
    }

    /**
     * @return the configured service auth token
     */
    public String getServiceAuthToken() {
        return serviceAuthToken;
    }

    /**
     * @deprecated Use {@link #getServiceAuthToken()} instead. This method is maintained for backwards compatibility.
     * @return the configured service auth token
     */
    @Deprecated(since = "1.2.0", forRemoval = true)
    public String getAppId() {
        return serviceAuthToken;
    }

    /**
     * @return true if the service uses keycloak for authn and authz or false if anonymous users are allowed.
     */
    public boolean isUseKeycloak() {
        return useKeycloak;
    }

    /**
     * @return the group name that identifies admin users in the MicroProfile JWT token's groups claim
     */
    public String getAdminGroupName() {
        return adminGroupName;
    }

    /**
     * Get the service auth token with fallback to deprecated app id property. Logs a deprecation warning if the old
     * property is used.
     */
    private String getServiceAuthTokenWithFallback() {
        String serviceAuthToken = System.getProperty(THEIACLOUD_SERVICE_AUTH_TOKEN);
        if (serviceAuthToken != null) {
            return serviceAuthToken;
        }

        String appId = System.getProperty(THEIACLOUD_APP_ID);
        if (appId != null) {
            logger.warn("Using deprecated property '" + THEIACLOUD_APP_ID + "'. " + "Please migrate to '"
                    + THEIACLOUD_SERVICE_AUTH_TOKEN + "' in your configuration.");
            return appId;
        }

        return "asdfghjkl"; // Default value
    }
}
