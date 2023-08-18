/********************************************************************************
 * Copyright (C) 2022-2023 EclipseSource and others.
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
package org.eclipse.theia.cloud.common.k8s.client;

import java.util.concurrent.TimeUnit;

import org.eclipse.theia.cloud.common.k8s.resource.session.SessionV6beta;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionV6betaSpec;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionV6betaSpecResourceList;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionV6betaStatus;

public interface SessionResourceClient
	extends CustomResourceClient<SessionV6betaSpec, SessionV6betaStatus, SessionV6beta, SessionV6betaSpecResourceList> {
    SessionV6beta launch(String correlationId, SessionV6betaSpec spec, long timeout, TimeUnit unit);

    default SessionV6beta launch(String correlationId, SessionV6betaSpec spec, int timeout) {
	return launch(correlationId, spec, timeout, TimeUnit.MINUTES);
    }

    boolean reportActivity(String correlationId, String name);
}
