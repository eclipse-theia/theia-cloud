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
package org.eclipse.theia.cloud.conversion.mappers.workspace;

import org.eclipse.theia.cloud.common.k8s.resource.workspace.Workspace;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.hub.WorkspaceHub;

import io.javaoperatorsdk.webhook.conversion.Mapper;

public class WorkspaceV1beta3Mapper implements Mapper<Workspace, WorkspaceHub> {

    @Override
    public WorkspaceHub toHub(Workspace resource) {
	return new WorkspaceHub(resource);
    }

    @Override
    public Workspace fromHub(WorkspaceHub hub) {
	return new Workspace(hub);
    }

}
