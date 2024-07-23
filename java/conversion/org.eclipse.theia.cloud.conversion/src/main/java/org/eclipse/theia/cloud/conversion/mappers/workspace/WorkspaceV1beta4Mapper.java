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

import org.eclipse.theia.cloud.common.k8s.resource.workspace.hub.WorkspaceHub;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.v1beta4.WorkspaceV1beta4;

import io.javaoperatorsdk.webhook.conversion.Mapper;
import io.javaoperatorsdk.webhook.conversion.TargetVersion;

@SuppressWarnings("deprecation")
@TargetVersion("v1beta4")
public class WorkspaceV1beta4Mapper implements Mapper<WorkspaceV1beta4, WorkspaceHub> {

    @Override
    public WorkspaceHub toHub(WorkspaceV1beta4 resource) {
        return new WorkspaceHub(resource);
    }

    @Override
    public WorkspaceV1beta4 fromHub(WorkspaceHub hub) {
        return new WorkspaceV1beta4(hub);
    }

}
