/********************************************************************************
 * Copyright (C) 2023 EclipseSource and others.
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
package org.eclipse.theia.cloud.common.k8s.resource.session.hub;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.theia.cloud.common.k8s.resource.session.SessionSpec;

public class SessionHubSpec {

    private final String name;
    private final String appDefinition;
    private final String user;
    private final String url;
    private final String error;
    private final String workspace;
    private final long lastActivity;
    private final String sessionSecret;
    private final Map<String, String> envVars;
    private final List<String> envVarsFromConfigMaps;
    private final List<String> envVarsFromSecrets;

    public SessionHubSpec(SessionSpec spec) {
	this.name = spec.getName();
	this.appDefinition = spec.getAppDefinition();
	this.user = spec.getUser();
	this.url = spec.getUrl();
	this.error = spec.getError();
	this.workspace = spec.getWorkspace();
	this.lastActivity = spec.getLastActivity();
	this.sessionSecret = spec.getSessionSecret();
	if (spec.getEnvVars() != null) {
	    this.envVars = new LinkedHashMap<>(spec.getEnvVars());
	} else {
	    this.envVars = new LinkedHashMap<>();
	}
	if (spec.getEnvVarsFromConfigMaps() != null) {
	    this.envVarsFromConfigMaps = new ArrayList<>(spec.getEnvVarsFromConfigMaps());
	} else {
	    this.envVarsFromConfigMaps = new ArrayList<>();
	}
	if (spec.getEnvVarsFromSecrets() != null) {
	    this.envVarsFromSecrets = new ArrayList<>(spec.getEnvVarsFromSecrets());
	} else {
	    this.envVarsFromSecrets = new ArrayList<>();
	}
    }

    @SuppressWarnings("deprecation")
    public SessionHubSpec(org.eclipse.theia.cloud.common.k8s.resource.session.v1beta5.SessionV1beta5Spec spec) {
	this.name = spec.getName();
	this.appDefinition = spec.getAppDefinition();
	this.user = spec.getUser();
	this.url = spec.getUrl();
	this.error = spec.getError();
	this.workspace = spec.getWorkspace();
	this.lastActivity = spec.getLastActivity();
	this.sessionSecret = spec.getSessionSecret();
	if (spec.getEnvVars() != null) {
	    this.envVars = new LinkedHashMap<>(spec.getEnvVars());
	} else {
	    this.envVars = new LinkedHashMap<>();
	}
	if (spec.getEnvVarsFromConfigMaps() != null) {
	    this.envVarsFromConfigMaps = new ArrayList<>(spec.getEnvVarsFromConfigMaps());
	} else {
	    this.envVarsFromConfigMaps = new ArrayList<>();
	}
	if (spec.getEnvVarsFromSecrets() != null) {
	    this.envVarsFromSecrets = new ArrayList<>(spec.getEnvVarsFromSecrets());
	} else {
	    this.envVarsFromSecrets = new ArrayList<>();
	}
    }

    public String getName() {
	return name;
    }

    public String getAppDefinition() {
	return appDefinition;
    }

    public String getUser() {
	return user;
    }

    public String getUrl() {
	return url;
    }

    public String getError() {
	return error;
    }

    public String getWorkspace() {
	return workspace;
    }

    public long getLastActivity() {
	return lastActivity;
    }

    public String getSessionSecret() {
	return sessionSecret;
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

    @Override
    public String toString() {
	return "SessionHubSpec [name=" + name + ", appDefinition=" + appDefinition + ", user=" + user + ", url=" + url
		+ ", error=" + error + ", workspace=" + workspace + ", lastActivity=" + lastActivity
		+ ", sessionSecret=" + sessionSecret + ", envVars=" + envVars + ", envVarsFromConfigMaps="
		+ envVarsFromConfigMaps + ", envVarsFromSecrets=" + envVarsFromSecrets + "]";
    }

}
