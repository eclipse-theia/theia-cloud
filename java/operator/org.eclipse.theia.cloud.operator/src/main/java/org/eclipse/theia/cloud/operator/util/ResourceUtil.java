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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ResourceUtil {

    private static final Logger LOGGER = LogManager.getLogger(ResourceUtil.class);

    private ResourceUtil() {
    }

    public static String readResourceAndReplacePlaceholders(Class<?> clazz, String resourceName,
	    Map<String, String> replacements, String correlationId) throws IOException, URISyntaxException {
	String template = Files.readString(Paths.get(clazz.getResource(resourceName).toURI()), StandardCharsets.UTF_8);
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
