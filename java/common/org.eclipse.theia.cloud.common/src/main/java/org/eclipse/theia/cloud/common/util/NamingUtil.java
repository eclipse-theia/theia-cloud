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

import org.eclipse.theia.cloud.common.k8s.resource.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.Session;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;

public final class NamingUtil {
    public static final int VALID_NAME_LIMIT = 62;

    private NamingUtil() {
	// utility
    }

    public static String createName(AppDefinition appDefinition, int instance, String suffix) {
	return createName(instance + "-" + appDefinition.getMetadata().getUid(), suffix,
		getAdditionalInformation(appDefinition));
    }

    public static String createName(Session session, String suffix) {
	return createName(session.getMetadata().getUid(), suffix, getAdditionalInformation(session));
    }

    public static String createName(Workspace workspace, String suffix) {
	return createName(workspace.getMetadata().getUid(), suffix, getAdditionalInformation(workspace));
    }

    private static String createName(String prefix, String suffix, String additionalInformation) {
	return asValidName(prefix + "-" + suffix + "-" + additionalInformation);
    }

    private static String getAdditionalInformation(AppDefinition appDefinition) {
	return appDefinition.getSpec().getName();
    }

    private static String getAdditionalInformation(Session session) {
	String workspace = (session.getSpec().getWorkspace() == null || session.getSpec().getWorkspace().isBlank())
		? "none"
		: session.getSpec().getWorkspace();
	return session.getSpec().getUser() + "-" + workspace + "-" + session.getSpec().getAppDefinition();
    }

    private static String getAdditionalInformation(Workspace workspace) {
	return workspace.getSpec().getName();
    }

    /**
     * Ensures that the given string is a valid Kubernetes string.
     * <p>
     * There are different restrictions for different labels but two relevant
     * standards are RFC 1123 and RFC 1035 so we just apply the stricter one leading
     * to the following conditions:
     * </p>
     * <ul>
     * <li>contain at most 63 characters</li>
     * <li>contain only lowercase alphanumeric characters or '-'</li>
     * <li>start with an alphabetic character</li>
     * <li>end with an alphanumeric character</li>
     */
    public static String asValidName(String originalString) {
	return asValidName(originalString, VALID_NAME_LIMIT);
    }

    public static String asValidName(String text, int limit) {
	if (text == null || text.length() == 0) {
	    return text;
	}

	// ensure no invalid characters are used, replace invalid characters with '-'
	String valid = text.replaceAll("[^a-z0-9A-Z\\-]", "-");

	// ensure text starts and ends with alphabetic character
	if (!Character.isLetter(valid.charAt(0))) {
	    valid = "a" + valid;
	}

	// trim text to correct length
	valid = valid.length() <= limit ? valid : valid.substring(0, limit);

	// ensure text ends with alphanumeric character
	if (!Character.isLetterOrDigit(valid.charAt(valid.length() - 1))) {
	    valid = valid.substring(0, valid.length() - 1) + "z";
	}
	return valid;
    }

}
