package org.eclipse.theia.cloud.service.admin.appdefinition;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.theia.cloud.service.ServiceRequest;

public class AppDefinitionUpdateRequest extends ServiceRequest {
    public static final String KIND = "appDefinitionUpdateRequest";

    @Schema(description = "The minimum number of instances to run.", required = false)
    public Integer minInstances;

    @Schema(description = "The maximum number of instances to run.", required = false)
    public Integer maxInstances;

    public AppDefinitionUpdateRequest() {
        super(KIND);
    }

    public AppDefinitionUpdateRequest(String appId) {
        super(KIND, appId);
    }
}
