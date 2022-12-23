/********************************************************************************
 * Copyright (C) 2022-2023 EclipseSource, STMicroelectronics and others.
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

/**
 * Represents a Theia Cloud user independent of the authentication mechanism.
 */
public class TheiaCloudUser {

    public static TheiaCloudUser EMPTY = new TheiaCloudUser(null);

    private String name;

    public TheiaCloudUser(String name) {
	this.name = name;
    }

    /**
     * The name identifying the user. Note that this is not necessarily an actual
     * name but the identifier of users used for sessions, etc. This could be any
     * unique identifier such as a user name, email address or UUID.
     * 
     * @return the name
     */
    public String getName() {
	return name;
    }

    public boolean isAnonymous() {
	return name == null || name.isBlank();
    }
}
