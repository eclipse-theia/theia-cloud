/********************************************************************************
 * Copyright (C) 2022 EclipseSource, Lockular, Ericsson, STMicroelectronics and 
 * others.
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

import java.text.MessageFormat;
import java.util.UUID;

public final class LogMessageUtil {

    private static final String LOG_MSG_PATTERN = "[{0}] {1}";

    private LogMessageUtil() {
    }

    public static String generateCorrelationId() {
	return UUID.randomUUID().toString();
    }

    public static String formatLogMessage(String prefix, String correlationID, String message) {
	return MessageFormat.format(LOG_MSG_PATTERN, (prefix + correlationID), message);
    }

    public static String formatLogMessage(String correlationID, String message) {
	return MessageFormat.format(LOG_MSG_PATTERN, correlationID, message);
    }

}
