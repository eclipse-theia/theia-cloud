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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link org.eclipse.theia.cloud.service.ApplicationProperties}
 *
 */
class ApplicationPropertiesTests {

    private static final String THEIA_CLOUD_AUTHENTICATION_ONLY = "theia.cloud.authentication.only";
    private static final String THEIA_CLOUD_GROUPS_USER = "theia.cloud.groups.user";
    private static final String THEIA_CLOUD_USE_KEYCLOAK = "theia.cloud.use.keycloak";

    @Test
    void getUserGroup_propertySet_returnConfiguredValue() {
	System.setProperty(THEIA_CLOUD_GROUPS_USER, "test_role_321");
	ApplicationProperties fixture = new ApplicationProperties();
	assertEquals("test_role_321", fixture.getUserGroup());
    }

    @Test
    void getUserGroup_propertySetToWhitespace_returnUserRole() {
	System.setProperty(THEIA_CLOUD_GROUPS_USER, " \t\n  ");
	ApplicationProperties fixture = new ApplicationProperties();
	assertEquals(AccessRoles.USER, fixture.getUserGroup());
    }

    @Test
    void getUserGroup_propertyNotSet_returnUserRole() {
	System.clearProperty(THEIA_CLOUD_GROUPS_USER);
	ApplicationProperties fixture = new ApplicationProperties();
	assertEquals(AccessRoles.USER, fixture.getUserGroup());
    }

    @Test
    void isAuthenticationOnly_propertyTrue_returnTrue() {
	System.setProperty(THEIA_CLOUD_AUTHENTICATION_ONLY, "true");
	ApplicationProperties fixture = new ApplicationProperties();
	assertTrue(fixture.isAuthenticationOnly());
    }

    @Test
    void isAuthenticationOnly_propertyNotSet_returnFalse() {
	System.clearProperty(THEIA_CLOUD_AUTHENTICATION_ONLY);
	ApplicationProperties fixture = new ApplicationProperties();
	assertFalse(fixture.isAuthenticationOnly());
    }

    @Test
    void isAuthenticationOnly_propertySetToSomeValue_returnFalse() {
	System.setProperty(THEIA_CLOUD_AUTHENTICATION_ONLY, "abc");
	ApplicationProperties fixture = new ApplicationProperties();
	assertFalse(fixture.isAuthenticationOnly());
    }

    @Test
    void isAuthenticationOnly_propertySetToFalse_returnFalse() {
	System.setProperty(THEIA_CLOUD_AUTHENTICATION_ONLY, "false");
	ApplicationProperties fixture = new ApplicationProperties();
	assertFalse(fixture.isAuthenticationOnly());
    }

    @Test
    void isUseKeycloak_propertyTrue_returnTrue() {
	System.setProperty(THEIA_CLOUD_USE_KEYCLOAK, "true");
	ApplicationProperties fixture = new ApplicationProperties();
	assertTrue(fixture.isUseKeycloak());
    }

    @Test
    void isUseKeycloak_propertyNotSet_returnTrue() {
	System.clearProperty(THEIA_CLOUD_USE_KEYCLOAK);
	ApplicationProperties fixture = new ApplicationProperties();
	assertTrue(fixture.isUseKeycloak());
    }

    @Test
    void isUseKeycloak_propertyFalse_returnFalse() {
	System.setProperty(THEIA_CLOUD_USE_KEYCLOAK, "false");
	ApplicationProperties fixture = new ApplicationProperties();
	assertFalse(fixture.isUseKeycloak());
    }

    @Test
    void isUseKeycloak_propertySetToSomeValue_returnTrue() {
	System.setProperty(THEIA_CLOUD_USE_KEYCLOAK, "asdasd");
	ApplicationProperties fixture = new ApplicationProperties();
	assertTrue(fixture.isUseKeycloak());
    }
}
