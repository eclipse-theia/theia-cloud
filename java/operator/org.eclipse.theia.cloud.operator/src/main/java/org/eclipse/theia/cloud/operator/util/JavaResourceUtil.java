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
package org.eclipse.theia.cloud.operator.util;

import static org.eclipse.theia.cloud.operator.util.LogMessageUtil.formatLogMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class JavaResourceUtil {

    private static final Logger LOGGER = LogManager.getLogger(JavaResourceUtil.class);

    private JavaResourceUtil() {
    }

    public static String readResourceAndReplacePlaceholders(Class<?> clazz, String resourceName,
	    Map<String, String> replacements, String correlationId) throws IOException, URISyntaxException {
	try (InputStream inputStream = JavaResourceUtil.class.getResourceAsStream(resourceName)) {
	    String template = new BufferedReader(new InputStreamReader(inputStream)).lines().parallel()
		    .collect(Collectors.joining("\n"));
	    LOGGER.trace(formatLogMessage(correlationId, "Updating template read with classloader " + clazz.getName()
		    + " from " + resourceName + " :\n" + template));
	    for (Entry<String, String> replacement : replacements.entrySet()) {
		template = template.replace(replacement.getKey(), replacement.getValue());
		LOGGER.trace(formatLogMessage(correlationId,
			"Replaced " + replacement.getKey() + " with " + replacement.getValue() + " :\n" + template));
	    }
	    return template;
	}
    }

}
