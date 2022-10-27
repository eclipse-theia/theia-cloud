/********************************************************************************
 * Copyright (C) 2022 EclipseSource, STMicroelectronics and others.
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
package org.eclipse.theia.cloud.service.validation;

import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.validator.routines.EmailValidator;

public final class Validate {

    private static final String ALPHANUMERIC_PLUS_MINUS_REGEX = "^[a-zA-Z0-9.\\-]+$";
    private static final Pattern ALPHANUMERIC_PLUS_MINUS_PATTERN = Pattern.compile(ALPHANUMERIC_PLUS_MINUS_REGEX);

    /**
     * 60 is picked arbitrarily at the moment and may be changed if required.
     * Assumption is that no sessions takes longer than 60 minutes to start.
     */
    private static final int MAX_TIMEOUT_MINUTES = 60;

    private Validate() {
    }

    /**
     * <ul>
     * <li>appId from request is compared to the appId passed to the service via a
     * java property. It is not used for anything else, so all values are valid,
     * including null and blank.</li>
     * </ul>
     */
    public static Optional<ValidationProblem> appId(String appId) {
	if (appId == null || appId.isEmpty()) {
	    return Optional.empty();
	}
	return noLineBreaks("appId", appId);
    }

    /**
     * <ul>
     * <li>kind is an optional description for a request. We are using it only in
     * the toString method for logging. All values are valid including null and
     * blank.</li>
     * </ul>
     */
    public static Optional<ValidationProblem> kind(String kind) {
	if (kind == null || kind.isEmpty()) {
	    return Optional.empty();
	}
	return noLineBreaks("kind", kind);
    }

    /**
     * <ul>
     * <li>user has to be a valid email address.</li>
     * </ul>
     */
    public static Optional<ValidationProblem> user(String user) {
	if (user == null || user.isEmpty()) {
	    return Optional.empty();
	}
	return validEmail("user", user);
    }

    private static Optional<ValidationProblem> validEmail(String field, String value) {
	if (!EmailValidator.getInstance().isValid(value)) {
	    return Optional.of(new ValidationProblem(field, value, "Not a valid email."));
	}
	return Optional.empty();
    }

    private static Optional<ValidationProblem> notNullAndNotBlank(String field, String value) {
	if (value == null) {
	    return Optional.of(new ValidationProblem(field, value, "Null is not a valid value."));
	}
	if (value.isBlank()) {
	    return Optional.of(new ValidationProblem(field, value, "Blank strings are not valid."));
	}
	return Optional.empty();
    }

    /**
     * <ul>
     * <li>appDefinition may get written into custom resources. This may lead to
     * attempted escape attacks. Only allow valid DNS subdomains.</li>
     * </ul>
     */
    public static Optional<ValidationProblem> optionalAppDefinition(String appDefinition) {
	if (appDefinition == null || appDefinition.isEmpty()) {
	    return Optional.empty();
	}
	return dnsSubDomainRfc1123("appDefinition", appDefinition);
    }

    /**
     * <ul>
     * <li>appDefinition may get written into custom resources. This may lead to
     * attempted escape attacks. Only allow valid DNS subdomains.</li>
     * </ul>
     */
    public static Optional<ValidationProblem> appDefinition(String appDefinition) {
	return notNullAndNotBlank("appDefinition", appDefinition)//
		.or(() -> dnsSubDomainRfc1123("appDefinition", appDefinition));
    }

    /**
     * <ul>
     * <li>workspaceName may get written into custom resources. This may lead to
     * attempted escape attacks. Only allow valid DNS subdomains.</li>
     * </ul>
     */
    public static Optional<ValidationProblem> workspaceName(String workspaceName) {
	if (workspaceName == null || workspaceName.isEmpty()) {
	    return Optional.empty();
	}
	return dnsSubDomainRfc1123("workspaceName", workspaceName);
    }

    /**
     * See https://tools.ietf.org/html/rfc1123
     * <ul>
     * <li>at most 253 characters</li>
     * <li>only lowercase alphanumeric characters or '-' or '.'</li>
     * <li>start with an alphanumeric character</li>
     * <li>end with an alphanumeric character</li>
     * </ul>
     */
    private static Optional<ValidationProblem> dnsSubDomainRfc1123(String field, String value) {
	// org.apache.commons.validator.routines.DomainValidator has the issue that it
	// checks whether the tld is actually a valid tld. We only want to check the
	// string itself not whether the domain works
	if (value.length() > 253 || startsOrEndsWithDotOrMinus(value)
		|| !ALPHANUMERIC_PLUS_MINUS_PATTERN.matcher(value).matches())
	    return Optional.of(new ValidationProblem(field, value, "Not a valid subdomain according to rfc1123"));

	return Optional.empty();
    }

    private static boolean startsOrEndsWithDotOrMinus(String value) {
	if (value.length() == 0) {
	    return true;
	}
	char first = value.charAt(0);
	char last = value.charAt(value.length() - 1);
	if ('.' == first || '.' == last || '-' == first || '-' == last) {
	    return true;
	}
	return false;
    }

    /**
     * <ul>
     * <li>label may get written into custom resources. It is a short human readable
     * description. This may lead to attempted escape attacks. Disallow strings with
     * line breaks. Empty and null strings are valid.</li>
     * </ul>
     */
    public static Optional<ValidationProblem> label(String label) {
	if (label == null || label.isEmpty()) {
	    return Optional.empty();
	}
	return noLineBreaks("label", label);
    }

    private static Optional<ValidationProblem> noLineBreaks(String field, String value) {
	if (value.lines().count() > 1) {
	    return Optional.of(new ValidationProblem(field, value, "Multi line strings are not valid."));
	}
	return Optional.empty();
    }

    /**
     * <ul>
     * <li>timeout value needs to be bigger than 0</li>
     * <li>Prevent too big timeout values so that attackers may not starve creating
     * new sessions by invalid launch requests that wait forever. Currently max
     * value is {@value #MAX_TIMEOUT_MINUTES}</li>
     * </ul>
     */
    public static Optional<ValidationProblem> timeoutMinutes(int timeout) {
	if (timeout < 1) {
	    return Optional.of(new ValidationProblem("timeout", timeout, "Not a valid timeout value."));
	}
	if (timeout > MAX_TIMEOUT_MINUTES) {
	    return Optional.of(new ValidationProblem("timeout", timeout,
		    "Max timeout value is " + MAX_TIMEOUT_MINUTES + " minutes"));
	}
	return Optional.empty();
    }

    /**
     * <ul>
     * <li>A session name used to look up a sessions with this name. All values are
     * valid</li>
     * </ul>
     */
    public static Optional<ValidationProblem> existingSessionName(String sessionName) {
	if (sessionName == null || sessionName.isEmpty()) {
	    return Optional.empty();
	}
	return noLineBreaks("sessionName", sessionName);
    }

    /**
     * <ul>
     * <li>A workspace name used to look up a workspace with this name. All values
     * are valid</li>
     * </ul>
     */
    public static Optional<ValidationProblem> existingWorkspaceName(String workspaceName) {
	if (workspaceName == null || workspaceName.isEmpty()) {
	    return Optional.empty();
	}
	return noLineBreaks("workspaceName", workspaceName);
    }

}
