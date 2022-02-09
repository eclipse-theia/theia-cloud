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
package org.eclipse.theia.cloud.operator.handler;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.theia.cloud.common.k8s.resource.Workspace;

public final class TheiaCloudPersistentVolumeUtil {

    public static final String PLACEHOLDER_PERSISTENTVOLUMENAME = "placeholder-pv";

    private TheiaCloudPersistentVolumeUtil() {

    }

    public static String getPersistentVolumeName(Workspace workspace) {
	String user = workspace.getSpec().getUser();
	String pvName = user.replace("@", "at").replace(".", "-");
	return pvName;
    }

    public static Map<String, String> getPersistentVolumeReplacements(String namespace, Workspace workspace) {
	Map<String, String> replacements = new LinkedHashMap<String, String>();
	replacements.put(PLACEHOLDER_PERSISTENTVOLUMENAME, getPersistentVolumeName(workspace));
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_NAMESPACE, namespace);
	return replacements;
    }

    public static Map<String, String> getPersistentVolumeClaimReplacements(String namespace, Workspace workspace) {
	Map<String, String> replacements = new LinkedHashMap<String, String>();
	replacements.put(PLACEHOLDER_PERSISTENTVOLUMENAME, getPersistentVolumeName(workspace));
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_NAMESPACE, namespace);
	return replacements;
    }

}
