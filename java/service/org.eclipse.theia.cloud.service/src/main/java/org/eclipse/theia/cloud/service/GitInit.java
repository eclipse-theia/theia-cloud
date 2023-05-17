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
package org.eclipse.theia.cloud.service;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "GitInit", description = "Holds information used to initialize a Workspace with a clone of a Git repository.")
public class GitInit {

    public static final String ID = "git";

    @Schema(description = "The Git repository URL.", required = true)
    public String repository;

    @Schema(description = "The branch, commit-id, or tag name to checkout.", example = "main, bd402d6, tags/1.0.0", required = true)
    public String checkout;

    @Schema(description = "Key for the required auth information, if the repository is not public.", required = false)
    public String authInformation;

    @Override
    public String toString() {
	return "GitInit [repository=" + repository + ", checkout=" + checkout + ", authInformation=" + authInformation
		+ "]";
    }

}
