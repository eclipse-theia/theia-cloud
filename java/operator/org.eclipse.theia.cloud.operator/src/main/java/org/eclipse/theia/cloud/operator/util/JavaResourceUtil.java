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

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class JavaResourceUtil {

    private static final String TEMPLATES = "/templates";
    private static final Logger LOGGER = LogManager.getLogger(JavaResourceUtil.class);

    private JavaResourceUtil() {
    }

    public static String readResourceAndReplacePlaceholders(String resourceName, Map<String, String> replacements,
	    String correlationId) throws IOException, URISyntaxException {
	try (InputStream inputStream = getInputStream(resourceName, correlationId)) {
	    String template = new BufferedReader(new InputStreamReader(inputStream)).lines().parallel()
		    .collect(Collectors.joining("\n"));
	    for (Entry<String, String> replacement : replacements.entrySet()) {
		String value = replacement.getValue() != null ? replacement.getValue() : "";
		template = template.replace(replacement.getKey(), value);
		LOGGER.trace(formatLogMessage(correlationId,
			"Replaced " + replacement.getKey() + " with " + value + " :\n" + template));
	    }
	    return template;
	}
    }

    protected static InputStream getInputStream(String resourceName, String correlationId)
	    throws FileNotFoundException {
	/* check if template is overridden */
	File file = Paths.get(TEMPLATES, resourceName).toFile();
	if (file.exists()) {
	    LOGGER.info(
		    formatLogMessage(correlationId, "Updating custom template read from " + file.getAbsolutePath()));
	    return new FileInputStream(file);
	}

	LOGGER.trace(formatLogMessage(correlationId, "Updating template read with classloader from " + resourceName));
	// Make own method
	return JavaResourceUtil.class.getResourceAsStream(resourceName);
    }

}
