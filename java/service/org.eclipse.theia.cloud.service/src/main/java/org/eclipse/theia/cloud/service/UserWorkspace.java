/********************************************************************************
 * Copyright (C) 2022 EclipseSource and others.
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
package org.eclipse.theia.cloud.service;

import org.eclipse.theia.cloud.common.k8s.resource.WorkspaceSpec;

public class UserWorkspace {
    public String name;

    public String label;

    public String appDefinition;

    public String user;

    public boolean active;

    public UserWorkspace() {
    }

    public UserWorkspace(WorkspaceSpec data) {
	this(data.getName(), data.getLabel(), data.getAppDefinition(), data.getUser());
    }

    public UserWorkspace(String name, String label, String appDefinition, String user) {
	this.name = name;
	this.label = label;
	this.appDefinition = appDefinition;
	this.user = user;
    }

    public void setActive(boolean active) {
	this.active = active;
    }

    public boolean isActive() {
	return active;
    }

}
