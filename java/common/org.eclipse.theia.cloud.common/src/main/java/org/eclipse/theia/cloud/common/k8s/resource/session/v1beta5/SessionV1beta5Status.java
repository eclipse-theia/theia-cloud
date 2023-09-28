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
package org.eclipse.theia.cloud.common.k8s.resource.session.v1beta5;

import org.eclipse.theia.cloud.common.k8s.resource.ResourceStatus;
import org.eclipse.theia.cloud.common.k8s.resource.session.hub.SessionHubStatus;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Deprecated
@JsonDeserialize
public class SessionV1beta5Status extends ResourceStatus {

    public SessionV1beta5Status() {
    }

    public SessionV1beta5Status(SessionHubStatus fromHub) {
	if (fromHub.getOperatorMessage() != null) {
	    this.setOperatorMessage(fromHub.getOperatorMessage());
	}
	if (fromHub.getOperatorStatus() != null) {
	    this.setOperatorStatus(fromHub.getOperatorStatus());
	}
    }
}
