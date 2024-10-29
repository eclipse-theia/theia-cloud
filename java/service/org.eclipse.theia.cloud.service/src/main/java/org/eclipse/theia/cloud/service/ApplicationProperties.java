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

    private static final String THEIACLOUD_APP_ID = "theia.cloud.app.id";
    private static final String THEIACLOUD_USE_KEYCLOAK = "theia.cloud.use.keycloak";

    private final Logger logger;

    private final boolean useKeycloak;
    private final String appId;

    public ApplicationProperties() {
        logger = Logger.getLogger(getClass());
        appId = System.getProperty(THEIACLOUD_APP_ID, "asdfghjkl");
        // Only disable keycloak if the value was explicitly set to exactly "false".
        useKeycloak = !"false".equals(System.getProperty(THEIACLOUD_USE_KEYCLOAK));
        if (!useKeycloak) {
            logger.warn("Keycloak integration was disabled. Anonymous requests are allowed!");
        }
    }

    /**
     * @return the configured application id
     */
    public String getAppId() {
        return appId;
    }

    /**
     * @return true if the service uses keycloak for authn and authz or false if anonymous users are allowed.
     */
    public boolean isUseKeycloak() {
        return useKeycloak;
    }
}
