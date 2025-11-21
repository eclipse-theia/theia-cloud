/********************************************************************************
 * Copyright (C) 2022-2024 EclipseSource and others.
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

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.Workspace;

public final class NamingUtil {

    public static final int VALID_NAME_LIMIT = 62;

    public static final char VALID_NAME_PREFIX = 'a';

    public static final char VALID_NAME_SUFFIX = 'z';

    /**
     * Prefix for names generated based on an app definition and an index. These names are typically used for objects
     * related to eagerly started instance pods (e.g. services or deployments).
     */
    public static final String APP_DEFINITION_INSTANCE_PREFIX = "instance-";

    private static final Locale US_LOCALE = new Locale("en", "US");

    private NamingUtil() {
        // utility
    }

    /**
     * @see NamingUtil#createName(AppDefinition, int, String)
     */
    public static String createName(AppDefinition appDefinition, int instance) {
        return createName(appDefinition, instance, null);
    }

    /**
     * Creates a string that can be used as a name for a Kubernetes object with a suffix. The suffix is always preserved
     * in full and other parts are shortened accordingly to stay within length limits.
     * 
     * @param appDefinition the {@link AppDefinition}
     * @param instance      instance id
     * @param suffix        the suffix to append to the name. Must not be null or blank.
     * @return the name with suffix
     */
    public static String createNameWithSuffix(AppDefinition appDefinition, int instance, String suffix) {
        String prefix = APP_DEFINITION_INSTANCE_PREFIX + instance;
        return createNameWithSuffix(prefix, null, appDefinition.getSpec().getName(),
                appDefinition.getMetadata().getUid(), suffix);
    }

    /**
     * Creates a string that can be used as a name for a Kubernetes object. When the same arguments are passed to this
     * method again, the resulting name will be the same. For different arguments the resulting name will be unique in
     * the cluster.
     * <p>
     * Typically, giving an <code>identifier</code> is not necessary because the Kubernetes object type (e.g.
     * Deployment, ConfigMap, ...) already contains this information and names only need to be unique for the same
     * object type. In turn, it is usually desired to have the same name for objects of different types that belong
     * together (e.g. deployment and service). An <code>identifier</code> is useful if there are multiple objects of the
     * same type for an AppDefinition.
     * </p>
     * <p>
     * The created name contains a "instance-" prefix, instance number, the identifier (if given), and the last segment
     * of the App Definition's UID. User, app definition and identifier are shortened to keep the name within
     * Kubernetes' character limit (63) minus 6 characters. The latter allows Kubernetes to add 6 characters at the end
     * of deployment pod names while the pod names pod names will still contain the whole name of the deployment
     * </p>
     * 
     * @param appDefinition the {@link AppDefinition}
     * @param instance      instance id
     * @param identifier    an optional short description/name of the Kubernetes object for which this name will be
     *                      used. <b>This will be shortened to the first 11 characters</b>. May be <code>null</code>.
     * @return the name
     */
    public static String createName(AppDefinition appDefinition, int instance, String identifier) {
        String prefix = APP_DEFINITION_INSTANCE_PREFIX + instance;
        return createName(prefix, identifier, null, appDefinition.getSpec().getName(),
                appDefinition.getMetadata().getUid());
    }

    /**
     * @see NamingUtil#createName(Session, String)
     */
    public static String createName(Session session) {
        return createName("session", null, session.getSpec().getUser(), session.getSpec().getAppDefinition(),
                session.getMetadata().getUid());
    }

    /**
     * Creates a string that can be used as a name for a Kubernetes object with a suffix. The suffix is always preserved
     * in full and other parts are shortened accordingly to stay within length limits.
     * 
     * @param session the {@link Session}
     * @param suffix  the suffix to append to the name. Must not be null or blank.
     * @return the name with suffix
     */
    public static String createNameWithSuffix(Session session, String suffix) {
        return createNameWithSuffix("session", session.getSpec().getUser(), session.getSpec().getAppDefinition(),
                session.getMetadata().getUid(), suffix);
    }

    /**
     * Creates a string that can be used as a name for a Kubernetes object. When the same arguments are passed to this
     * method again, the resulting name will be the same. For different arguments the resulting name will be unique in
     * the cluster.
     * <p>
     * Typically, giving an <code>identifier</code> is not necessary because the Kubernetes object type (e.g.
     * Deployment, ConfigMap, ...) already contains this information and names only need to be unique for the same
     * object type. In turn, it is usually desired to have the same name for objects of different types that belong
     * together (e.g. deployment and service). An <code>identifier</code> is useful if there are multiple objects of the
     * same type for a Session.
     * </p>
     * <p>
     * The created name contains a "session" prefix, the session's user and app definition, the identifier (if given),
     * and the last segment of the Session's UID. User, app definition and identifier are shortened to keep the name
     * within Kubernetes' character limit (63) minus 6 characters. The latter allows Kubernetes to add 6 characters at
     * the end of deployment pod names while the pod names pod names will still contain the whole name of the deployment
     * </p>
     * 
     * @param session    the {@link Session}
     * @param identifier an optional short description/name of the Kubernetes object for which this name will be used.
     *                   <b>This will be shortened to the first 11 characters</b>. May be <code>null</code>.
     * @return the name
     */
    public static String createName(Session session, String identifier) {
        return createName("session", identifier, session.getSpec().getUser(), session.getSpec().getAppDefinition(),
                session.getMetadata().getUid());
    }

    /**
     * @see NamingUtil#createName(Workspace, String)
     */
    public static String createName(Workspace workspace) {
        return createName(workspace, null);
    }

    /**
     * Creates a string that can be used as a name for a Kubernetes object. When the same arguments are passed to this
     * method again, the resulting name will be the same. For different arguments the resulting name will be unique in
     * the cluster.
     * <p>
     * Typically, giving an <code>identifier</code> is not necessary because the Kubernetes object type (e.g.
     * Deployment, ConfigMap, ...) already contains this information and names only need to be unique for the same
     * object type. In turn, it is usually desired to have the same name for objects of different types that belong
     * together (e.g. deployment and service). An <code>identifier</code> is useful if there are multiple objects of the
     * same type for a Workspace.
     * </p>
     * <p>
     * The created name contains a "workspace" prefix, the workspace's user and app definition, the identifier (if
     * given), and the last segment of the Workspace's UID. User, app definition and identifier are shortened to keep
     * the name within Kubernetes' character limit (63).
     * </p>
     * 
     * @param workspace  the {@link Workspace}
     * @param identifier an optional short description/name of the Kubernetes object for which this name will be used.
     *                   <b>This will be shortened to the first 11 characters</b>. May be <code>null</code>.
     * @return the name
     */
    public static String createName(Workspace workspace, String identifier) {
        /*
         * Kubenertes UIDs are standardized UUIDs/GUIDs. This means the uid string will have a length of 36. Unique part
         * of the name will consist of the workspace uuid followed by the identifier. Parts will be separated with "-".
         * This must be shorter than {@link NamingUtil.VALID_NAME_LIMIT} We fill remaining space with additional
         * information about the workspace. This may be trimmed away however.
         */
        return createName("ws", identifier, workspace.getSpec().getUser(), workspace.getSpec().getAppDefinition(),
                workspace.getMetadata().getUid());
    }

    /**
     * Builds a valid Kubernetes object names with a suffix. The suffix is always preserved in full and other parts are
     * shortened accordingly to stay within length limits.
     * 
     * @param prefix        The prefix to start the object name with. Should start with a letter and be at most 13
     *                      characters long. Longer prefixes are possible but might lead to other info being cut short.
     * @param user          The user, may be <code>null</code>
     * @param appDefinition The app definition name, may be <code>null</code>
     * @param uid           a unique Kubernetes object id that the name relates to. I.e. the UID of a session when
     *                      creating the name of a session deployment.
     * @param suffix        the suffix to append to the name. Must not be null or blank.
     * @return the joined and valid Kubernetes name with suffix
     */
    private static String createNameWithSuffix(String prefix, String user, String appDefinition, String uid,
            String suffix) {
        /*
         * Kubenertes UIDs are standardized UUIDs/GUIDs. This means the uid string will have a length of 36. We take the
         * last segment with length 12 to generate unique names for each Session even if user and app definition are the
         * same.
         */
        String shortUid = trimUid(uid);

        // If the user is an email address, only take the part before the @ sign because
        // this is usually sufficient to identify the user.
        String userName = user != null ? user.split("@")[0] : null;

        // Calculate available space considering the suffix
        String validSuffix = suffix != null && !suffix.isBlank() ? suffix : null;
        if (validSuffix == null) {
            throw new IllegalArgumentException("Suffix must not be null or blank");
        }

        int suffixLength = validSuffix.length() + 1; // +1 for the "-" separator
        int availableLength = VALID_NAME_LIMIT - suffixLength;

        // Calculate lengths for the variable parts, ensuring we reserve space for prefix, uid, and separators
        int fixedPartsLength = prefix.length() + 1 + shortUid.length(); // +1 for separator before uid
        int availableForVariableParts = Math.max(0, availableLength - fixedPartsLength);

        // Divide remaining space between user and app definition
        int infoSegmentLength = availableForVariableParts > 0 ? Math.max(1, availableForVariableParts / 2) : 0;

        String shortUserName = trimLength(userName, infoSegmentLength);
        String shortAppDef = trimLength(appDefinition, infoSegmentLength);

        return asValidName(prefix, shortUserName, shortAppDef, shortUid, validSuffix);
    }

    /**
     * Builds a valid Kubernetes object names. Except for the prefix, all segments are limited to a fixed number of
     * characters to ensure that the resulting name includes information from all parameters.
     * 
     * @param prefix        The prefix to start the object name with. Should start with a letter and be at most 13
     *                      characters long. Longer prefixes are possible but might lead to other info being cut short.
     * @param identifier    an optional short description/name of the kubernetes object for which this name will be
     *                      used. <b>This will be shortened to the first 11 characters</b>. May be <code>null</code>.
     * @param user          The user, may be <code>null</code>
     * @param appDefinition The app definition name, may be <code>null</code>
     * @param uid           a unique Kubernetes object id that the name relates to. I.e. the UID of a session when
     *                      creating the name of a session deployment.
     * @return the joined and valid Kubernetes name
     */
    private static String createName(String prefix, String identifier, String user, String appDefinition, String uid) {
        /*
         * Kubenertes UIDs are standardized UUIDs/GUIDs. This means the uid string will have a length of 36. We take the
         * last segment with length 12 to generate unique names for each Session even if user and app definition are the
         * same.
         */
        String shortUid = trimUid(uid);

        // If the user is an email address, only take the part before the @ sign because
        // this is usually sufficient to identify the user.
        String userName = user != null ? user.split("@")[0] : null;

        int infoSegmentLength;
        String shortenedIdentifier = null;
        if (identifier == null || identifier.isBlank()) {
            infoSegmentLength = 17;
        } else {
            infoSegmentLength = 11;
            shortenedIdentifier = trimLength(identifier, infoSegmentLength);
        }
        String shortUserName = trimLength(userName, infoSegmentLength);
        String shortAppDef = trimLength(appDefinition, infoSegmentLength);

        return asValidName(prefix, shortenedIdentifier, shortUserName, shortAppDef, shortUid);
    }

    /**
     * Kubenertes UIDs are standardized UUIDs/GUIDs. This means the uid string will have a length of 36. We take the
     * last segment with length 12 to generate unique names for Kubernetes objects even if other user and app definition
     * are the same.
     */
    private static String trimUid(String uid) {
        return uid.substring(uid.length() - 12, uid.length());
    }

    private static String trimLength(String text, int maxLength) {
        if (text == null || maxLength <= 0) {
            return null;
        }
        return text.substring(0, Math.min(text.length(), maxLength));
    }

    /**
     * Joins the given name segments with "-" and enforces conventions to get a valid kubernetes name. Empty or null
     * segments are removed before joining them.
     * 
     * @param segments String segments to join. Segments may be null or empty. These are removed before joining.
     * @return the name
     */
    private static String asValidName(String... segments) {
        String[] filteredSegments = Arrays.stream(segments).filter(Objects::nonNull).filter(s -> !s.isBlank())
                .toArray(String[]::new);
        return asValidName(String.join("-", filteredSegments));
    }

    /**
     * Ensures that the given string is a valid Kubernetes string.
     * <p>
     * There are different restrictions for different labels but two relevant standards are RFC 1123 and RFC 1035 so we
     * just apply the stricter one leading to the following conditions:
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
