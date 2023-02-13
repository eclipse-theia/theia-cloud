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

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

/**
 * Injectable bean providing access to the service's configurable application
 * properties.
 *
 */
@ApplicationScoped
public class ApplicationProperties {

    private static final String THEIA_CLOUD_APP_ID = "theia.cloud.app.id";
    private static final String THEIA_CLOUD_USE_KEYCLOAK = "theia.cloud.use.keycloak";
    private static final String THEIA_CLOUD_AUTHENTICATION_ONLY = "theia.cloud.authentication.only";
    private static final String THEIA_CLOUD_GROUPS_USER = "theia.cloud.groups.user";

    private final Logger logger;

    private final boolean useKeycloak;
    private final String appId;
    private final boolean authenticationOnly;
    private final String userGroup;

    public ApplicationProperties() {
	logger = Logger.getLogger(getClass());
	appId = System.getProperty(THEIA_CLOUD_APP_ID, "asdfghjkl");
	// Only disable keycloak if the value was explicitly set to exactly "false".
	useKeycloak = !"false".equals(System.getProperty(THEIA_CLOUD_USE_KEYCLOAK));
	if (!useKeycloak) {
	    logger.warn("Keycloak integration was disabled. Anonymous requests are allowed!");
	}

	// Only activate authn only mode if the value was explicitly set to exactly
	// "true"
	authenticationOnly = "true".equals(System.getProperty(THEIA_CLOUD_AUTHENTICATION_ONLY));
	if (authenticationOnly) {
	    logger.warn("Authentication only. User roles are not verified.");
	}

	final String readUserGroup = System.getProperty(THEIA_CLOUD_GROUPS_USER, AccessRoles.USER).strip();
	if (readUserGroup.isBlank()) {
	    logger.warnv(
		    "Configured user group via property {0} contained only whitespace. Fall back to default group {1}",
		    THEIA_CLOUD_GROUPS_USER, AccessRoles.USER);
	    userGroup = AccessRoles.USER;
	} else {
	    userGroup = readUserGroup;
	}
    }

    /**
     * @return the configured application id
     */
    public String getAppId() {
	return appId;
    }

    /**
     * @return true if the service uses keycloak for authn and authz or false if
     *         anonymous users are allowed.
     */
    public boolean isUseKeycloak() {
	return useKeycloak;
    }

    /**
     * 
     * @return true if the service uses authentication but not authorization. This
     *         means that all users get the user role even if it wasn't assigned by
     *         the identity provider.
     */
    public boolean isAuthenticationOnly() {
	return authenticationOnly;
    }

    /**
     * @return The group defining {@link AccessRoles.USER users} of TheiaCloud. If
     *         not configured or whitespace only, returns the name of the default
     *         role {@link AccessRoles.USER}. Never <code>null</code>
     */
    public String getUserGroup() {
	return userGroup;
    }
}
