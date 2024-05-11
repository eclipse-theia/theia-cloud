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
package org.eclipse.theia.cloud.conversion.mappers.session;

import org.eclipse.theia.cloud.common.k8s.resource.session.hub.SessionHub;
import org.eclipse.theia.cloud.common.k8s.resource.session.v1beta7.SessionV1beta7;

import io.javaoperatorsdk.webhook.conversion.Mapper;
import io.javaoperatorsdk.webhook.conversion.TargetVersion;

@SuppressWarnings("deprecation")
@TargetVersion("v1beta7")
public class SessionV1beta7Mapper implements Mapper<SessionV1beta7, SessionHub> {

    @Override
    public SessionHub toHub(SessionV1beta7 resource) {
	return new SessionHub(resource);
    }

    @Override
    public SessionV1beta7 fromHub(SessionHub hub) {
	return new SessionV1beta7(hub);
    }

}
