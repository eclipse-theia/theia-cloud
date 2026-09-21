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
package org.eclipse.theia.cloud.operator.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;

/**
 * Unit tests for {@link TheiaCloudHandlerUtil}.
 */
class TheiaCloudHandlerUtilTests {

    @Test
    void setSessionOwnerReference_replacesExistingSessionOwner() {
        Route route = new RouteBuilder().withNewMetadata().withName("route")
                .withOwnerReferences(sessionOwner("old-session", "old-uid")).endMetadata().build();

        TheiaCloudHandlerUtil.setSessionOwnerReference("test-correlation", "session-current-uid", "current-uid",
                route);

        assertEquals(1, route.getMetadata().getOwnerReferences().size());
        assertSessionOwner(route.getMetadata().getOwnerReferences().get(0));
    }

    @Test
    void setSessionOwnerReference_missingOwnerReferences_addsSessionOwner() {
        Route route = new RouteBuilder().withNewMetadata().withName("route").endMetadata().build();
        route.getMetadata().setOwnerReferences(null);

        TheiaCloudHandlerUtil.setSessionOwnerReference("test-correlation", "session-current-uid", "current-uid",
                route);

        assertEquals(1, route.getMetadata().getOwnerReferences().size());
        assertSessionOwner(route.getMetadata().getOwnerReferences().get(0));
    }

    @Test
    void setSessionOwnerReference_emptyOwnerReferences_addsSessionOwner() {
        Route route = new RouteBuilder().withNewMetadata().withName("route").withOwnerReferences().endMetadata()
                .build();

        TheiaCloudHandlerUtil.setSessionOwnerReference("test-correlation", "session-current-uid", "current-uid",
                route);

        assertEquals(1, route.getMetadata().getOwnerReferences().size());
        assertSessionOwner(route.getMetadata().getOwnerReferences().get(0));
    }

    @Test
    void setSessionOwnerReference_multipleSessionOwners_keepsOnlyOne() {
        Route route = new RouteBuilder().withNewMetadata().withName("route")
                .withOwnerReferences(sessionOwner("first-session", "first-uid"),
                        sessionOwner("second-session", "second-uid"))
                .endMetadata().build();

        TheiaCloudHandlerUtil.setSessionOwnerReference("test-correlation", "session-current-uid", "current-uid",
                route);

        assertEquals(1, route.getMetadata().getOwnerReferences().size());
        assertSessionOwner(route.getMetadata().getOwnerReferences().get(0));
    }

    @Test
    void setSessionOwnerReference_nonSessionOwner_isPreserved() {
        OwnerReference appDefinitionOwner = new OwnerReferenceBuilder().withApiVersion(AppDefinition.API)
                .withKind(AppDefinition.KIND).withName("app-definition").withUid("app-definition-uid").build();
        Route route = new RouteBuilder().withNewMetadata().withName("route")
                .withOwnerReferences(appDefinitionOwner, sessionOwner("old-session", "old-uid")).endMetadata()
                .build();

        TheiaCloudHandlerUtil.setSessionOwnerReference("test-correlation", "session-current-uid", "current-uid",
                route);

        assertEquals(2, route.getMetadata().getOwnerReferences().size());
        assertEquals(appDefinitionOwner, route.getMetadata().getOwnerReferences().get(0));
        assertSessionOwner(route.getMetadata().getOwnerReferences().get(1));
    }

    private OwnerReference sessionOwner(String name, String uid) {
        return new OwnerReferenceBuilder().withApiVersion(Session.API).withKind(Session.KIND).withName(name)
                .withUid(uid).build();
    }

    private void assertSessionOwner(OwnerReference ownerReference) {
        assertEquals(Session.API, ownerReference.getApiVersion());
        assertEquals(Session.KIND, ownerReference.getKind());
        assertEquals("session-current-uid", ownerReference.getName());
        assertEquals("current-uid", ownerReference.getUid());
    }
}
