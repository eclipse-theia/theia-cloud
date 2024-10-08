package org.eclipse.theia.cloud.common.util;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionSpec;

public class LabelsUtil {
    public static final String LABEL_CUSTOM_PREFIX = "theia-cloud.io";

    public static final String LABEL_KEY_SESSION = "app.kubernetes.io/component";
    public static final String LABEL_VALUE_SESSION = "session";

    public static final String LABEL_KEY_USER = LABEL_CUSTOM_PREFIX + "/user";
    public static final String LABEL_KEY_APPDEF = LABEL_CUSTOM_PREFIX + "/app-definition";

    public static Map<String, String> createSessionLabels(SessionSpec sessionSpec, AppDefinitionSpec appDefinitionSpec) {
        Map<String, String> labels = new HashMap<>();
        labels.put(LABEL_KEY_SESSION, LABEL_VALUE_SESSION);
        String userName = sessionSpec.getUser().split("@")[0];
        labels.put(LABEL_KEY_USER, userName);
        labels.put(LABEL_KEY_APPDEF, appDefinitionSpec.getName());
        return labels;
    }
}
