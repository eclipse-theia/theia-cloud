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
package org.eclipse.theia.cloud.service.workspace;

import static org.eclipse.theia.cloud.common.util.NamingUtil.asValidName;
import static org.eclipse.theia.cloud.common.util.WorkspaceUtil.generateWorkspaceLabel;
import static org.eclipse.theia.cloud.common.util.WorkspaceUtil.generateWorkspaceName;

import java.util.Optional;

import org.eclipse.theia.cloud.common.k8s.resource.WorkspaceSpec;

public class UserWorkspace {
    public String name;

    public String label;

    public String appDefinition;

    public String user;

    public boolean active;

    public UserWorkspace() {
    }

    public UserWorkspace(String appDefinition, String user) {
	this(appDefinition, user, null);
    }

    public UserWorkspace(String appDefinition, String user, String label) {
	this(appDefinition, user, null, label);
    }

    public UserWorkspace(String appDefinition, String user, String name, String label) {
	this.name = Optional.ofNullable(asValidName(name)).orElseGet(() -> generateWorkspaceName(user, appDefinition));
	this.label = Optional.ofNullable(label).orElseGet(() -> generateWorkspaceLabel(user, appDefinition));
	this.appDefinition = appDefinition;
	this.user = user;
    }

    public UserWorkspace(WorkspaceSpec data) {
	this(data.getAppDefinition(), data.getUser(), data.getName(), data.getLabel());
    }

    public void setActive(boolean active) {
	this.active = active;
    }

    public boolean isActive() {
	return active;
    }

    @Override
    public String toString() {
	return "UserWorkspace [name=" + name + ", label=" + label + ", appDefinition=" + appDefinition + ", user="
		+ user + ", active=" + active + "]";
    }

}
