/********************************************************************************
 * Copyright (C) 2024 EclipseSource and others.
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
package org.eclipse.theia.cloud.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.Workspace;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.WorkspaceSpec;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.ObjectMeta;

/**
 * Unit tests for {@link NamingUtil}.
 */
class NamingUtilTests {

    @Test
    void createName_AppDefinitionAndInstace() {
        AppDefinition appDefinition = createAppDefinition();

        String result = NamingUtil.createName(appDefinition, 1);
        assertEquals("instance-1-some-app-definiti-381261d79c23", result);
    }

    @Test
    void createName_AppDefinitionAndInstaceAndIdentifier() {
        AppDefinition appDefinition = createAppDefinition();

        String result = NamingUtil.createName(appDefinition, 1, "longidentifier");
        assertEquals("instance-1-longidentif-some-app-de-381261d79c23", result);
    }

    @Test
    void createName_SessionAndNullIdentifier() {
        Session session = createSession();

        String result = NamingUtil.createName(session, null);
        assertEquals("session-some-username-test-app-definiti-426930ea37d7", result);
    }

    @Test
    void createName_SessionAndWhitespaceIdentifier() {
        Session session = createSession();

        String result = NamingUtil.createName(session, " ");
        assertEquals("session-some-username-test-app-definiti-426930ea37d7", result);
    }

    @Test
    void createName_SessionAndEmptyIdentifier() {
        Session session = createSession();

        String result = NamingUtil.createName(session, "");
        assertEquals("session-some-username-test-app-definiti-426930ea37d7", result);
    }

    @Test
    void createName_SessionAndIdentifier() {
        Session session = createSession();

        String result = NamingUtil.createName(session, "longidentifier");
        assertEquals("session-longidentif-some-userna-test-app-de-426930ea37d7", result);
    }

    @Test
    void createName_WorkspaceAndNullIdentifier() {
        Workspace workspace = createWorkspace();

        String result = NamingUtil.createName(workspace, null);
        assertEquals("ws-some-username-test-app-definiti-381261d79c23", result);
    }

    @Test
    void createName_WorkspaceAndWhitespaceIdentifier() {
        Workspace workspace = createWorkspace();

        String result = NamingUtil.createName(workspace, " ");
        assertEquals("ws-some-username-test-app-definiti-381261d79c23", result);
    }

    @Test
    void createName_WorkspaceAndEmptyIdentifier() {
        Workspace workspace = createWorkspace();

        String result = NamingUtil.createName(workspace, "");
        assertEquals("ws-some-username-test-app-definiti-381261d79c23", result);
    }

    @Test
    void createName_WorkspaceAndIdentifier() {
        Workspace workspace = createWorkspace();

        String result = NamingUtil.createName(workspace, "longidentifier");
        assertEquals("ws-longidentif-some-userna-test-app-de-381261d79c23", result);
    }

    private AppDefinition createAppDefinition() {
        AppDefinition appDefinition = new AppDefinition();
        ObjectMeta objectMeta = new ObjectMeta();
        objectMeta.setUid("6f1a8966-4d5a-41dc-82ba-381261d79c23");
        appDefinition.setMetadata(objectMeta);
        AppDefinitionSpec spec = new AppDefinitionSpec() {
            @Override
            public String getName() {
                return "some-app-definition";
            }
        };
        appDefinition.setSpec(spec);
        return appDefinition;
    }

    private Session createSession() {
        Session session = new Session();
        ObjectMeta objectMeta = new ObjectMeta();
        objectMeta.setUid("2b8a76db-a049-496f-b897-426930ea37d7");
        session.setMetadata(objectMeta);
        SessionSpec sessionSpec = new SessionSpec("some-session-spec", "test-app-definition",
                "some.username@example.org");
        session.setSpec(sessionSpec);
        return session;
    }

    private Workspace createWorkspace() {
        Workspace workspace = new Workspace();
        ObjectMeta objectMeta = new ObjectMeta();
        objectMeta.setUid("6f1a8966-4d5a-41dc-82ba-381261d79c23");
        workspace.setMetadata(objectMeta);
        WorkspaceSpec workspaceSpec = new WorkspaceSpec("some-workspace-spec", "some-workspace-label",
                "test-app-definition", "some.username@example.org");
        workspace.setSpec(workspaceSpec);
        return workspace;
    }
}
