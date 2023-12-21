/********************************************************************************
 * Copyright (C) 2022-2023 EclipseSource, Lockular, Ericsson, STMicroelectronics and 
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

import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionSpec;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.PodSpec;

public final class TheiaCloudPersistentVolumeUtil {
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
