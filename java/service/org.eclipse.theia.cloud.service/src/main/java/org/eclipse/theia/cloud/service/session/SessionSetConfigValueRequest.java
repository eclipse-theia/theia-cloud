package org.eclipse.theia.cloud.service.session;

import org.eclipse.theia.cloud.service.ServiceRequest;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public class SessionSetConfigValueRequest extends ServiceRequest {

    public static final String KIND = "SessionSetConfigValueRequest";

    @Schema(description = "The configuration key", required = true)
    public String key;

    @Schema(description = "The configuration value", required = true, nullable = true)
    public String value;

    public SessionSetConfigValueRequest(String key, String value) {
        super(KIND);
        this.key = key;
        this.value = value;
    }

    public SessionSetConfigValueRequest(String appId, String key, String value) {
        super(KIND, appId);
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return "SessionSetConfigValueRequest [key=" + key + ", value=" + value + ", appId=" + appId + ", kind=" + kind
                + "]";
    }
}