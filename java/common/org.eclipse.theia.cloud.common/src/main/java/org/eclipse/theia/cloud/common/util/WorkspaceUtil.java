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
package org.eclipse.theia.cloud.common.util;

import static org.eclipse.theia.cloud.common.util.NamingUtil.asValidName;

import java.time.Instant;

import org.eclipse.theia.cloud.common.k8s.resource.workspace.Workspace;

public final class WorkspaceUtil {
    private static final String SESSION_SUFFIX = "-session";
    private static final String STORAGE_SUFFIX = "pvc";
    private static final String WORKSPACE_PREFIX = "ws-";
    private static final int WORKSPACE_NAME_LIMIT = NamingUtil.VALID_NAME_LIMIT - SESSION_SUFFIX.length();

    private WorkspaceUtil() {
        // util
    }

    public static String generateUniqueWorkspaceName(String user, String appDefinitionName) {
        return asValidName((WORKSPACE_PREFIX + Instant.now().toEpochMilli() + getWorkspaceDescription(appDefinitionName)
                + "-" + user).toLowerCase(), WORKSPACE_NAME_LIMIT);
    }

    public static String generateNonUniqueWorkspaceName(String user, String appDefinitionName) {
        return asValidName((WORKSPACE_PREFIX + getWorkspaceDescription(appDefinitionName) + "-" + user).toLowerCase(),
                WORKSPACE_NAME_LIMIT);
    }

    public static String getSessionName(String workspaceName) {
        return workspaceName + SESSION_SUFFIX;
    }

    public static String getSessionName(String user, String appDefinitionName, boolean unique) {
        String workspaceName = unique ? generateUniqueWorkspaceName(user, getWorkspaceDescription(appDefinitionName))
                : generateNonUniqueWorkspaceName(user, getWorkspaceDescription(appDefinitionName));
        return getSessionName(workspaceName);
    }

    public static String getStorageName(Workspace workspace) {
        return NamingUtil.createName(workspace, STORAGE_SUFFIX);
    }

    public static String generateWorkspaceLabel(String user, String appDefinitionName) {
        return getWorkspaceDescription(appDefinitionName) + " of " + user;
    }

    protected static String getWorkspaceDescription(String appDefinitionName) {
        return (appDefinitionName == null || appDefinitionName.isBlank()) ? "Workspace" : appDefinitionName;
    }

}
