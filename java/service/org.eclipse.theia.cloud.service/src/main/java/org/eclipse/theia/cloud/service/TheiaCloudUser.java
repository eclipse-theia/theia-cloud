/********************************************************************************
 * Copyright (C) 2022-2025 EclipseSource, STMicroelectronics and others.
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

import java.util.Objects;

/**
 * Represents a Theia Cloud user independent of the authentication mechanism.
 */
public class TheiaCloudUser {

    public static final TheiaCloudUser ANONYMOUS = new TheiaCloudUser(null, false);

    private final String identifier;
    private final boolean admin;

    /**
     * Creates a non-admin user.
     *
     * @param identifier the user identifier
     */
    public TheiaCloudUser(String identifier) {
        this(identifier, false);
    }

    /**
     * Creates a user with the specified admin flag.
     *
     * @param identifier the user identifier
     * @param admin      {@code true} if the user is an admin
     */
    public TheiaCloudUser(String identifier, boolean admin) {
        this.identifier = identifier;
        this.admin = admin;
    }

    /**
     * The user's unique identifier. This is not guaranteed to be suitable as a display name because it can be any
     * unique identifier such as a user name, email address or UUID.
     * 
     * @return the unique identifier, never null or {@link java.lang.String#isBlank() blank} if {@link #isAnonymous()}
     *         returns false
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Returns whether the user is an admin.
     *
     * @return {@code true} if admin and not anonymous, {@code false} otherwise
     */
    public boolean isAdmin() {
        return !isAnonymous() && admin;
    }

    /**
     * Determines if the user is anonymous.
     *
     * @return {@code true} if the user is anonymous
     */
    public boolean isAnonymous() {
        return identifier == null || identifier.isBlank();
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, admin);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TheiaCloudUser other = (TheiaCloudUser) obj;
        return Objects.equals(identifier, other.identifier) && admin == other.admin;
    }

    @Override
    public String toString() {
        return "TheiaCloudUser [identifier=" + identifier + ", admin=" + admin + "]";
    }
}
