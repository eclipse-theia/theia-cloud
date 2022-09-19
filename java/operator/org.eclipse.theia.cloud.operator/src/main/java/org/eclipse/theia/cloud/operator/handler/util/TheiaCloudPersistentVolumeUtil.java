/********************************************************************************
 * Copyright (C) 2022 EclipseSource, Lockular, Ericsson, STMicroelectronics and 
 * others.
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
package org.eclipse.theia.cloud.operator.handler.util;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.theia.cloud.common.k8s.resource.AppDefinitionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.common.util.WorkspaceUtil;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.PodSpec;

public final class TheiaCloudPersistentVolumeUtil {

    public static final String PLACEHOLDER_PERSISTENTVOLUMENAME = "placeholder-pv";
    public static final String PLACEHOLDER_LABEL_WORKSPACE_NAME = "placeholder-label-workspace-name";

    private static final String MOUNT_PATH = "/home/project/persisted";

    private TheiaCloudPersistentVolumeUtil() {

    }

    public static String getMountPath(AppDefinitionSpec appDefinition) {
	String mountPath = appDefinition.getMountPath();
	if (mountPath == null || mountPath.isEmpty()) {
	    return MOUNT_PATH;
	}
	return mountPath;
    }

    public static Map<String, String> getPersistentVolumeReplacements(String namespace, Workspace workspace) {
	Map<String, String> replacements = new LinkedHashMap<String, String>();
	replacements.put(PLACEHOLDER_PERSISTENTVOLUMENAME, WorkspaceUtil.getStorageName(workspace));
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_NAMESPACE, namespace);
	replacements.put(PLACEHOLDER_LABEL_WORKSPACE_NAME, workspace.getSpec().getName());
	return replacements;
    }

    public static Map<String, String> getPersistentVolumeClaimReplacements(String namespace, Workspace workspace) {
	Map<String, String> replacements = new LinkedHashMap<String, String>();
	replacements.put(PLACEHOLDER_PERSISTENTVOLUMENAME, WorkspaceUtil.getStorageName(workspace));
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_NAMESPACE, namespace);
	replacements.put(PLACEHOLDER_LABEL_WORKSPACE_NAME, workspace.getSpec().getName());
	return replacements;
    }

    public static Container getTheiaContainer(PodSpec podSpec, AppDefinitionSpec appDefinition) {
	String image = appDefinition.getImage();
	for (Container container : podSpec.getContainers()) {
	    if (container.getImage().startsWith(image)) {
		return container;
	    }
	}
	if (podSpec.getContainers().size() == 1) {
	    return podSpec.getContainers().get(0);
	}
	return podSpec.getContainers().get(1);
    }

}
