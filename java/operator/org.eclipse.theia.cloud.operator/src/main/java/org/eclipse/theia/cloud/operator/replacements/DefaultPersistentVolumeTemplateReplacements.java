/********************************************************************************
 * Copyright (C) 2023 EclipseSource, STMicroelectronics and others.
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
package org.eclipse.theia.cloud.operator.replacements;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.theia.cloud.common.k8s.resource.workspace.Workspace;
import org.eclipse.theia.cloud.common.util.WorkspaceUtil;
import org.eclipse.theia.cloud.operator.TheiaCloudOperatorArguments;
import org.eclipse.theia.cloud.operator.util.TheiaCloudHandlerUtil;

import com.google.inject.Inject;

public class DefaultPersistentVolumeTemplateReplacements implements PersistentVolumeTemplateReplacements {

    public static final String PLACEHOLDER_PERSISTENTVOLUMENAME = "placeholder-pv";
    public static final String PLACEHOLDER_LABEL_WORKSPACE_NAME = "placeholder-label-workspace-name";
    public static final String PLACEHOLDER_STORAGE_CLASS_NAME = "placeholder-storage-class-name";
    public static final String PLACEHOLDER_REQUESTED_STORAGE = "placeholder-requested-storage";

    public static final String DEFAULT_REQUESTED_STORAGE = "250Mi";

    @Inject
    TheiaCloudOperatorArguments arguments;

    @Override
    public Map<String, String> getPersistentVolumeReplacements(String namespace, Workspace workspace) {
        Map<String, String> replacements = new LinkedHashMap<String, String>();
        replacements.put(PLACEHOLDER_PERSISTENTVOLUMENAME, WorkspaceUtil.getStorageName(workspace));
        replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_NAMESPACE, namespace);
        replacements.put(PLACEHOLDER_LABEL_WORKSPACE_NAME, workspace.getSpec().getName());
        replacements.put(PLACEHOLDER_REQUESTED_STORAGE,
                orDefault(arguments.getRequestedStorage(), DEFAULT_REQUESTED_STORAGE));
        return replacements;
    }

    @Override
    public Map<String, String> getPersistentVolumeClaimReplacements(String namespace, Workspace workspace) {
        Map<String, String> replacements = new LinkedHashMap<String, String>();
        replacements.put(PLACEHOLDER_PERSISTENTVOLUMENAME, WorkspaceUtil.getStorageName(workspace));
        replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_NAMESPACE, namespace);
        replacements.put(PLACEHOLDER_LABEL_WORKSPACE_NAME, workspace.getSpec().getName());
        replacements.put(PLACEHOLDER_STORAGE_CLASS_NAME, orEmpty(arguments.getStorageClassName()));
        replacements.put(PLACEHOLDER_REQUESTED_STORAGE,
                orDefault(arguments.getRequestedStorage(), DEFAULT_REQUESTED_STORAGE));
        return replacements;
    }

}
