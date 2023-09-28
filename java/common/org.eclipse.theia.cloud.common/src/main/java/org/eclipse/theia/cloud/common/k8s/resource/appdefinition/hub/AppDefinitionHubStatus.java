/********************************************************************************
 * Copyright (C) 2023 EclipseSource and others.
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
package org.eclipse.theia.cloud.common.k8s.resource.appdefinition.hub;

import org.eclipse.theia.cloud.common.k8s.resource.ResourceStatus;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionStatus;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.v1beta7.AppDefinitionV1beta7Status;

public class AppDefinitionHubStatus extends ResourceStatus {
    public AppDefinitionHubStatus() {
    }

    public AppDefinitionHubStatus(AppDefinitionStatus toHub) {
	if (toHub.getOperatorMessage() != null) {
	    this.setOperatorMessage(toHub.getOperatorMessage());
	}
	if (toHub.getOperatorStatus() != null) {
	    this.setOperatorStatus(toHub.getOperatorStatus());
	}
    }

    public AppDefinitionHubStatus(AppDefinitionV1beta7Status toHub) {
	if (toHub.getOperatorMessage() != null) {
	    this.setOperatorMessage(toHub.getOperatorMessage());
	}
	if (toHub.getOperatorStatus() != null) {
	    this.setOperatorStatus(toHub.getOperatorStatus());
	}
    }
}
