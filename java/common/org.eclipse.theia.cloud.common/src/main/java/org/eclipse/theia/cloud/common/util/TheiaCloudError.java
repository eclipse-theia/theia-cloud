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
package org.eclipse.theia.cloud.common.util;

public class TheiaCloudError {
    private static final String SERIALIZATION_DELIMITER = ":";
    // use same as HTTP status code for internal server error
    private static final int INTERNAL_SERVER_ERROR = 500;

    // client errors: 47x
    public static final TheiaCloudError INVALID_APP_ID = new TheiaCloudError(470, "Invalid application id.");
    public static final TheiaCloudError INVALID_WORKSPACE_NAME = new TheiaCloudError(471, "Invalid workspace name.");
    public static final TheiaCloudError INVALID_APP_DEFINITION_NAME = new TheiaCloudError(473,
            "Invalid app definition name.");
    public static final TheiaCloudError INVALID_SESSION_NAME = new TheiaCloudError(474, "Invalid session name.");
    public static final TheiaCloudError APP_DEFINITION_NAME_MISMATCH = new TheiaCloudError(475,
            "Mismatch between app definition names.");
    public static final TheiaCloudError MISSING_WORKSPACE_NAME = new TheiaCloudError(480, "Missing workspace name.");
    public static final TheiaCloudError MISSING_SESSION_NAME = new TheiaCloudError(481, "Missing session name.");

    // server errors: 52x workspace
    public static final TheiaCloudError WORKSPACE_LAUNCH_TIMEOUT = new TheiaCloudError(520,
            "Unable to launch workspace within time limit.");
    public static final TheiaCloudError METRICS_SERVER_UNAVAILABLE = new TheiaCloudError(521,
            "Metrics server not ready (yet).");

    // server errors: 55x session
    public static final TheiaCloudError SESSION_LAUNCH_TIMEOUT = new TheiaCloudError(551,
            "Unable to launch session within time limit.");
    public static final TheiaCloudError SESSION_SERVER_LIMIT_REACHED = new TheiaCloudError(552,
            "Max instances reached. Could not create session.");
    public static final TheiaCloudError SESSION_USER_LIMIT_REACHED = new TheiaCloudError(553,
            "No more sessions allowed for this user, you reached your limit.");
    public static final TheiaCloudError SESSION_USER_NO_SESSIONS = new TheiaCloudError(554,
            "No sessions allowed for this user.");

    // server errors: 58x optional features
    public static final TheiaCloudError CONFIG_STORE_NOT_AVAILABLE = new TheiaCloudError(580,
            "The Theia Cloud Config Store is not available. It needs to be installed in the application.");

    private final int code;
    private final String reason;

    public TheiaCloudError(String reason) {
        this(INTERNAL_SERVER_ERROR, reason);
    }

    public TheiaCloudError(int code, String reason) {
        this.code = code;
        this.reason = reason;
    }

    public int getCode() {
        return code;
    }

    public String getReason() {
        return reason;
    }

    public String asString() {
        return String.join(SERIALIZATION_DELIMITER, String.valueOf(code), reason);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + code;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TheiaCloudError other = (TheiaCloudError) obj;
        if (code != other.code)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "TheiaCloudError [code=" + code + ", reason=" + reason + "]";
    }

    public static TheiaCloudError fromString(String error) {
        if (!isErrorString(error)) {
            return null;
        }
        try {
            int indexOfDelimiter = error.indexOf(SERIALIZATION_DELIMITER);
            if (indexOfDelimiter == -1) {
                return new TheiaCloudError(error);
            }
            int code = Integer.parseInt(error.substring(0, indexOfDelimiter));
            String reason = error.substring(indexOfDelimiter + 1);
            return new TheiaCloudError(code, reason);

        } catch (Exception exception) {
            return new TheiaCloudError(error);
        }
    }

    public static boolean isErrorString(String error) {
        return error != null && !error.isBlank();
    }
}
