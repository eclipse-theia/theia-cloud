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

import java.util.Locale;

import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionV8beta;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionV6beta;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.WorkspaceV3beta;

public final class NamingUtil {

    public static final int VALID_NAME_LIMIT = 62;

    private static final int MAX_IDENTIFIER_LENGTH = 24;

    public static final char VALID_NAME_PREFIX = 'a';

    public static final char VALID_NAME_SUFFIX = 'z';

    private static final Locale US_LOCALE = new Locale("en", "US");

    private NamingUtil() {
	// utility
    }

    /**
     * Creates a string that can be used as a name for a kubernetes object.
     * 
     * When the same arguments are passed to this method again, the resulting name
     * will be the same.
     * 
     * For different arguments the resulting name will be unique in the cluster.
     * 
     * @param appDefinition the {@link AppDefinitionV8beta}
     * @param instance      instance id
     * @param identifier    a short description/name of the kubernetes object for
     *                      which this name will be used. This will be part of the
     *                      unique sections of the generated name. <b>The combined
     *                      length of the instance and the identifier must be at
     *                      most 23 characters long!</b>
     * @return the name
     */
    public static String createName(AppDefinitionV8beta appDefinition, int instance, String identifier) {
	/*
	 * Kubenertes UIDs are standardized UUIDs/GUIDs. This means the uid string will
	 * have a length of 36.
	 * 
	 * Unique part of the name will consist of the instance followed by the app
	 * definition's uuid followed by the identifier. Parts will be separated with
	 * "-". This must be shorter than {@link NamingUtil.VALID_NAME_LIMIT}
	 * 
	 * We fill remaining space with additional information about the app definition.
	 * This may be trimmed away however.
	 */
	String prefix = instance + "-";
	return createName(prefix + appDefinition.getMetadata().getUid(), identifier,
		getAdditionalInformation(appDefinition), MAX_IDENTIFIER_LENGTH - prefix.length());
    }

    /**
     * Creates a string that can be used as a name for a kubernetes object.
     * 
     * When the same arguments are passed to this method again, the resulting name
     * will be the same.
     * 
     * For different arguments the resulting name will be unique in the cluster.
     * 
     * @param session    the {@link SessionV6beta}
     * @param identifier a short description/name of the kubernetes object for which
     *                   this name will be used. This will be part of the unique
     *                   sections of the generated name. <b>Must be at most 24
     *                   characters long!</b>
     * @return the name
     */
    public static String createName(SessionV6beta session, String identifier) {
	/*
	 * Kubenertes UIDs are standardized UUIDs/GUIDs. This means the uid string will
	 * have a length of 36.
	 * 
	 * Unique part of the name will consist of the sessions's uuid followed by the
	 * identifier. Parts will be separated with "-". This must be shorter than
	 * {@link NamingUtil.VALID_NAME_LIMIT}
	 * 
	 * We fill remaining space with additional information about the session. This
	 * may be trimmed away however.
	 */
	return createName(session.getMetadata().getUid(), identifier, getAdditionalInformation(session),
		MAX_IDENTIFIER_LENGTH);
    }

    /**
     * Creates a string that can be used as a name for a kubernetes object.
     * 
     * When the same arguments are passed to this method again, the resulting name
     * will be the same.
     * 
     * For different arguments the resulting name will be unique in the cluster.
     * 
     * @param workspace  the {@link WorkspaceV3beta}
     * @param identifier a short description/name of the kubernetes object for which
     *                   this name will be used. This will be part of the unique
     *                   sections of the generated name. <b>Must be at most 24
     *                   characters long!</b>
     * @return the name
     */
    public static String createName(WorkspaceV3beta workspace, String identifier) {
	/*
	 * Kubenertes UIDs are standardized UUIDs/GUIDs. This means the uid string will
	 * have a length of 36.
	 * 
	 * Unique part of the name will consist of the workspace uuid followed by the
	 * identifier. Parts will be separated with "-". This must be shorter than
	 * {@link NamingUtil.VALID_NAME_LIMIT}
	 * 
	 * We fill remaining space with additional information about the workspace. This
	 * may be trimmed away however.
	 */
	return createName(workspace.getMetadata().getUid(), identifier, getAdditionalInformation(workspace),
		MAX_IDENTIFIER_LENGTH);
    }

    /**
     * Joins prefix, identifier, and additionalInformation with "-" and enforces
     * conventions to get a valid kubernetes name.
     * 
     * @param prefix
     * @param identifier
     * @param additionalInformation
     * @param maxIdentifierLength   max length of the identifier
     * @return the name
     * 
     * @throw {@link IllegalArgumentException} in case the passed identifier is too
     *        long
     */
    private static String createName(String prefix, String identifier, String additionalInformation,
	    int maxIdentifierLength) {
	if (identifier.length() > maxIdentifierLength) {
	    throw new IllegalArgumentException(
		    "Identifier " + identifier + " is too long. Max length is " + maxIdentifierLength);
	}
	return asValidName(String.join("-", prefix, identifier, additionalInformation));
    }

    private static String getAdditionalInformation(AppDefinitionV8beta appDefinition) {
	return appDefinition.getSpec().getName();
    }

    private static String getAdditionalInformation(SessionV6beta session) {
	String workspace = (session.getSpec().getWorkspace() == null || session.getSpec().getWorkspace().isBlank())
		? "none"
		: session.getSpec().getWorkspace();
	return session.getSpec().getUser() + "-" + workspace + "-" + session.getSpec().getAppDefinition();
    }

    private static String getAdditionalInformation(WorkspaceV3beta workspace) {
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
	    valid = VALID_NAME_PREFIX + valid;
	}

	// trim text to correct length
	valid = valid.length() <= limit ? valid : valid.substring(0, limit);

	// ensure text ends with alphanumeric character
	if (!Character.isLetterOrDigit(valid.charAt(valid.length() - 1))) {
	    valid = valid.substring(0, valid.length() - 1) + VALID_NAME_SUFFIX;
	}
	return valid.toLowerCase(US_LOCALE);
    }

}
