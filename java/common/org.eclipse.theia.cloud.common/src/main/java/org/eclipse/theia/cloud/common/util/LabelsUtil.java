package org.eclipse.theia.cloud.common.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionSpec;

public class LabelsUtil {
    private static final Logger LOGGER = Logger.getLogger(LabelsUtil.class.getName());

    public static final String LABEL_CUSTOM_PREFIX = "theia-cloud.io";

    public static final String LABEL_KEY_SESSION = "app.kubernetes.io/component";
    public static final String LABEL_VALUE_SESSION = "session";

    public static final String LABEL_KEY_THEIACLOUD = "app.kubernetes.io/part-of";
    public static final String LABEL_VALUE_THEIACLOUD = "theia-cloud";

    public static final String LABEL_KEY_USER = LABEL_CUSTOM_PREFIX + "/user";
    public static final String LABEL_KEY_APPDEF = LABEL_CUSTOM_PREFIX + "/app-definition";
    public static final String LABEL_KEY_SESSION_NAME = LABEL_CUSTOM_PREFIX + "/session";

    private static final int MAX_LABEL_LENGTH = 63;

    private static String truncateLabelValue(String value) {
        if (value.length() > MAX_LABEL_LENGTH) {
            LOGGER.warning("Label value truncated: " + value);
            return value.substring(0, MAX_LABEL_LENGTH);
        }
        return value;
    }

    public static Map<String, String> createSessionLabels(SessionSpec sessionSpec,
            AppDefinitionSpec appDefinitionSpec) {
        Map<String, String> labels = new HashMap<>();
        labels.put(LABEL_KEY_SESSION, LABEL_VALUE_SESSION);
        labels.put(LABEL_KEY_THEIACLOUD, LABEL_VALUE_THEIACLOUD);
        String sanitizedUser = sessionSpec.getUser().replaceAll("@", "_at_").replaceAll("[^a-zA-Z0-9]", "_");
        labels.put(LABEL_KEY_USER, truncateLabelValue(sanitizedUser));
        labels.put(LABEL_KEY_APPDEF, truncateLabelValue(appDefinitionSpec.getName()));
        labels.put(LABEL_KEY_SESSION_NAME, truncateLabelValue(sessionSpec.getName()));
        return labels;
    }

    /**
     * Returns the set of label keys that are specific to a specific session, i.e. the user and session name keys.
     * 
     * @return The session specific label keys.
     */
    public static Set<String> getSessionSpecificLabelKeys() {
        return Set.of(LABEL_KEY_SESSION_NAME, LABEL_KEY_USER);
    }
}
