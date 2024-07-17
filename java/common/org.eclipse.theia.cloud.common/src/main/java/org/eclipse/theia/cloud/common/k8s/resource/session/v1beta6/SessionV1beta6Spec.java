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
package org.eclipse.theia.cloud.common.k8s.resource.session.v1beta6;

import java.util.List;
import java.util.Map;

import org.eclipse.theia.cloud.common.k8s.resource.UserScopedSpec;
import org.eclipse.theia.cloud.common.k8s.resource.session.hub.SessionHub;
import org.eclipse.theia.cloud.common.util.TheiaCloudError;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Deprecated
@JsonDeserialize()
public class SessionV1beta6Spec implements UserScopedSpec {

    @JsonProperty("name")
    private String name;

    @JsonProperty("appDefinition")
    private String appDefinition;

    @JsonProperty("user")
    private String user;

    @JsonProperty("url")
    private String url;

    @JsonProperty("error")
    private String error;

    @JsonProperty("workspace")
    private String workspace;

    @JsonProperty("lastActivity")
    private long lastActivity;

    @JsonProperty("sessionSecret")
    private String sessionSecret;

    @JsonProperty("envVars")
    private Map<String, String> envVars;

    @JsonProperty("envVarsFromConfigMaps")
    private List<String> envVarsFromConfigMaps;

    @JsonProperty("envVarsFromSecrets")
    private List<String> envVarsFromSecrets;

    /**
     * Default constructor.
     */
    public SessionV1beta6Spec() {
    }

    public SessionV1beta6Spec(String name, String appDefinition, String user) {
        this(name, appDefinition, user, null);
    }

    public SessionV1beta6Spec(String name, String appDefinition, String user, String workspace) {
        this(name, appDefinition, user, workspace, Map.of(), List.of(), List.of());
    }

    public SessionV1beta6Spec(String name, String appDefinition, String user, String workspace,
            Map<String, String> envVars) {
        this(name, appDefinition, user, workspace, envVars, List.of(), List.of());
    }

    public SessionV1beta6Spec(String name, String appDefinition, String user, String workspace,
            Map<String, String> envVars, List<String> envVarsFromConfigMaps) {
        this(name, appDefinition, user, workspace, envVars, envVarsFromConfigMaps, List.of());
    }

    public SessionV1beta6Spec(String name, String appDefinition, String user, String workspace,
            Map<String, String> envVars, List<String> envVarsFromConfigMaps, List<String> envVarsFromSecrets) {
        this.name = name;
        this.appDefinition = appDefinition;
        this.user = user;
        this.workspace = workspace;
        this.envVars = envVars;
        this.envVarsFromConfigMaps = envVarsFromConfigMaps;
        this.envVarsFromSecrets = envVarsFromSecrets;
    }

    public SessionV1beta6Spec(SessionHub fromHub) {
        this.name = fromHub.getName().orElse(null);
        this.appDefinition = fromHub.getAppDefinition().orElse(null);
        this.user = fromHub.getUser().orElse(null);
        this.url = fromHub.getUrl().orElse(null);
        this.error = fromHub.getError().orElse(null);
        this.workspace = fromHub.getWorkspace().orElse(null);
        this.lastActivity = fromHub.getLastActivity().orElse((long) 0);
        this.sessionSecret = fromHub.getSessionSecret().orElse(null);
        this.envVars = fromHub.getEnvVars().orElse(null);
        this.envVarsFromConfigMaps = fromHub.getEnvVarsFromConfigMaps().orElse(null);
        this.envVarsFromSecrets = fromHub.getEnvVarsFromSecrets().orElse(null);
    }

    public String getName() {
        return name;
    }

    public String getAppDefinition() {
        return appDefinition;
    }

    public boolean hasAppDefinition() {
        return getAppDefinition() != null && !getAppDefinition().isBlank();
    }

    @Override
    public String getUser() {
        return user;
    }

    public String getUrl() {
        return url;
    }

    public String getSessionSecret() {
        return sessionSecret;
    }

    public void setSessionSecret(String sessionSecret) {
        this.sessionSecret = sessionSecret;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean hasUrl() {
        return getUrl() != null && !getUrl().isBlank();
    }

    public String getError() {
        return error;
    }

    public void setError(TheiaCloudError error) {
        setError(error.asString());
    }

    public void setError(String error) {
        this.error = error;
    }

    public boolean hasError() {
        return TheiaCloudError.isErrorString(getError());
    }

    public long getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(long lastActivity) {
        this.lastActivity = lastActivity;
    }

    public String getWorkspace() {
        return workspace;
    }

    public Map<String, String> getEnvVars() {
        return envVars;
    }

    public List<String> getEnvVarsFromConfigMaps() {
        return envVarsFromConfigMaps;
    }

    public List<String> getEnvVarsFromSecrets() {
        return envVarsFromSecrets;
    }

    @JsonIgnore
    public boolean isEphemeral() {
        return isEphemeral(workspace);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((appDefinition == null) ? 0 : appDefinition.hashCode());
        result = prime * result + ((user == null) ? 0 : user.hashCode());
        result = prime * result + ((workspace == null) ? 0 : workspace.hashCode());
        result = prime * result + ((sessionSecret == null) ? 0 : sessionSecret.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SessionV1beta6Spec other = (SessionV1beta6Spec) obj;
        if (appDefinition == null) {
            if (other.appDefinition != null)
                return false;
        } else if (!appDefinition.equals(other.appDefinition))
            return false;
        if (user == null) {
            if (other.user != null)
                return false;
        } else if (!user.equals(other.user))
            return false;
        if (workspace == null) {
            if (other.workspace != null)
                return false;
        } else if (!workspace.equals(other.workspace))
            return false;
        if (sessionSecret == null) {
            if (other.sessionSecret != null)
                return false;
        } else if (!sessionSecret.equals(other.sessionSecret))
            return false;
        if (envVars == null) {
            if (other.envVars != null)
                return false;
        } else if (!envVars.equals(other.envVars))
            return false;
        if (envVarsFromConfigMaps == null) {
            if (other.envVarsFromConfigMaps != null)
                return false;
        } else if (!envVarsFromConfigMaps.equals(other.envVarsFromConfigMaps))
            return false;
        if (envVarsFromSecrets == null) {
            if (other.envVarsFromSecrets != null)
                return false;
        } else if (!envVarsFromSecrets.equals(other.envVarsFromSecrets))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "SessionSpec [name=" + name + ", appDefinition=" + appDefinition + ", user=" + user + ", url=" + url
                + ", error=" + error + ", workspace=" + workspace + ", lastActivity=" + lastActivity + "]";
    }

    public static boolean isEphemeral(String workspace) {
        return workspace == null || workspace.isBlank();
    }

}
