/********************************************************************************
 * Copyright (C) 2026 EclipseSource and others.
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
package org.eclipse.theia.cloud.operator.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionSpec;
import org.eclipse.theia.cloud.common.util.NamingUtil;
import org.eclipse.theia.cloud.operator.TheiaCloudOperatorArguments;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.ObjectMeta;

/**
 * Unit tests for {@link OpenShiftRouteRoutingStrategy} hostname and route name
 * computation methods.
 */
class OpenShiftRouteRoutingStrategyTests {

    private static final String INSTANCES_HOST = "ws.apps-crc.testing";

    private OpenShiftRouteRoutingStrategy strategy;

    @BeforeEach
    void setUp() throws Exception {
        TheiaCloudOperatorArguments args = new TheiaCloudOperatorArguments();
        Field instancesHostField = TheiaCloudOperatorArguments.class.getDeclaredField("instancesHost");
        instancesHostField.setAccessible(true);
        instancesHostField.set(args, INSTANCES_HOST);

        strategy = new OpenShiftRouteRoutingStrategy(args);
    }

    @Test
    void computeSessionHostname_usesUidAndInstancesHost() {
        Session session = createSession("abc-123-uid");

        String hostname = strategy.computeSessionHostname(session);

        assertEquals("abc-123-uid.ws.apps-crc.testing", hostname);
    }

    @Test
    void computeInstanceHostname_usesAppNameInstanceAndHost() {
        AppDefinition appDef = createAppDefinition("my-app");

        String hostname = strategy.computeInstanceHostname(appDef, 0);

        assertEquals("my-app-0.ws.apps-crc.testing", hostname);
    }

    @Test
    void computeInstanceHostname_differentInstances() {
        AppDefinition appDef = createAppDefinition("editor");

        assertEquals("editor-0.ws.apps-crc.testing", strategy.computeInstanceHostname(appDef, 0));
        assertEquals("editor-1.ws.apps-crc.testing", strategy.computeInstanceHostname(appDef, 1));
        assertEquals("editor-5.ws.apps-crc.testing", strategy.computeInstanceHostname(appDef, 5));
    }

    @Test
    void computeInstanceRouteName_endsWith_route() {
        AppDefinition appDef = createAppDefinition("my-app");

        String routeName = strategy.computeInstanceRouteName(appDef, 0);

        assertTrue(routeName.endsWith("-route"));
    }

    @Test
    void computeInstanceRouteName_matchesNamingUtil() {
        AppDefinition appDef = createAppDefinition("my-app");

        String routeName = strategy.computeInstanceRouteName(appDef, 0);

        String expected = NamingUtil.createNameWithSuffix(appDef, 0, "route");
        assertEquals(expected, routeName);
    }

    @Test
    void computeInstanceRouteName_differentInstances_produceDifferentNames() {
        AppDefinition appDef = createAppDefinition("my-app");

        String route0 = strategy.computeInstanceRouteName(appDef, 0);
        String route1 = strategy.computeInstanceRouteName(appDef, 1);

        assertNotEquals(route0, route1);
    }

    @Test
    void computeInstanceRouteName_withinKubernetesNameLimit() {
        AppDefinition appDef = createAppDefinition("very-long-app-definition-name-for-testing");

        String routeName = strategy.computeInstanceRouteName(appDef, 99);

        assertTrue(routeName.length() <= NamingUtil.VALID_NAME_LIMIT);
        assertTrue(routeName.endsWith("-route"));
    }

    @Test
    void computeSessionHostname_differentSessions_produceDifferentHostnames() {
        Session session1 = createSession("uid-1");
        Session session2 = createSession("uid-2");

        assertNotEquals(strategy.computeSessionHostname(session1), strategy.computeSessionHostname(session2));
    }

    @Test
    void computeInstanceHostname_differentApps_produceDifferentHostnames() {
        AppDefinition appDef1 = createAppDefinition("app-one");
        AppDefinition appDef2 = createAppDefinition("app-two");

        assertNotEquals(strategy.computeInstanceHostname(appDef1, 0), strategy.computeInstanceHostname(appDef2, 0));
    }

    @Test
    void computeInstanceHostname_sanitizesInvalidDnsCharacters() {
        AppDefinition appDef = createAppDefinition("My.App");

        String hostname = strategy.computeInstanceHostname(appDef, 0);

        assertEquals("my-app-0.ws.apps-crc.testing", hostname);
    }

    @Test
    void computeInstanceHostname_truncatesLongNames() {
        String longName = "a".repeat(70);
        AppDefinition appDef = createAppDefinition(longName);

        String hostname = strategy.computeInstanceHostname(appDef, 0);
        String subdomainLabel = hostname.substring(0, hostname.indexOf("."));

        assertTrue(subdomainLabel.length() <= 63,
                "Subdomain label must be at most 63 characters but was " + subdomainLabel.length());
    }

    private Session createSession(String uid) {
        Session session = new Session();
        ObjectMeta meta = new ObjectMeta();
        meta.setUid(uid);
        session.setMetadata(meta);
        SessionSpec spec = new SessionSpec("test-session", "test-app", "user@example.org");
        session.setSpec(spec);
        return session;
    }

    private AppDefinition createAppDefinition(String appName) {
        AppDefinition appDef = new AppDefinition();
        ObjectMeta meta = new ObjectMeta();
        meta.setUid("6f1a8966-4d5a-41dc-82ba-381261d79c23");
        appDef.setMetadata(meta);
        AppDefinitionSpec spec = new AppDefinitionSpec() {
            @Override
            public String getName() {
                return appName;
            }
        };
        appDef.setSpec(spec);
        return appDef;
    }
}
