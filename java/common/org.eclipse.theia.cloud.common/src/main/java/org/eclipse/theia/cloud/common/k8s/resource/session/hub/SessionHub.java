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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.theia.cloud.common.k8s.resource.session.Session;

import io.fabric8.kubernetes.api.model.ObjectMeta;

public class SessionHub {

    final Optional<ObjectMeta> metadata;
    final Optional<String> name;
    final Optional<String> appDefinition;
    final Optional<String> user;
    final Optional<String> url;
    final Optional<String> error;

    final Optional<String> workspace;
    final Optional<Long> lastActivity;
    final Optional<String> sessionSecret;
    final Optional<Map<String, String>> envVars;
    final Optional<List<String>> envVarsFromConfigMaps;
    final Optional<List<String>> envVarsFromSecrets;

    final Optional<String> operatorStatus;
    final Optional<String> operatorMessage;

    public SessionHub(Session toHub) {
        this.metadata = Optional.ofNullable(toHub.getMetadata());
        this.name = Optional.ofNullable(toHub.getSpec().getName());
        this.appDefinition = Optional.ofNullable(toHub.getSpec().getAppDefinition());
        this.user = Optional.ofNullable(toHub.getSpec().getUser());
        this.workspace = Optional.ofNullable(toHub.getSpec().getWorkspace());
        this.sessionSecret = Optional.ofNullable(toHub.getSpec().getSessionSecret());
        this.envVars = Optional.ofNullable(toHub.getSpec().getEnvVars());
        this.envVarsFromConfigMaps = Optional.ofNullable(toHub.getSpec().getEnvVarsFromConfigMaps());
        this.envVarsFromSecrets = Optional.ofNullable(toHub.getSpec().getEnvVarsFromSecrets());
        // Status is not a required field
        if (toHub.getStatus() != null) {
            this.lastActivity = Optional.ofNullable(toHub.getStatus().getLastActivity());
            this.url = Optional.ofNullable(toHub.getStatus().getUrl());
            this.error = Optional.ofNullable(toHub.getStatus().getError());
            this.operatorStatus = Optional.ofNullable(toHub.getStatus().getOperatorStatus());
            this.operatorMessage = Optional.ofNullable(toHub.getStatus().getOperatorMessage());
        } else {
            this.lastActivity = Optional.empty();
            this.url = Optional.empty();
            this.error = Optional.empty();
            this.operatorStatus = Optional.empty();
            this.operatorMessage = Optional.empty();
        }
    }

    @SuppressWarnings("deprecation")
    public SessionHub(org.eclipse.theia.cloud.common.k8s.resource.session.v1beta6.SessionV1beta6 toHub) {
        this.metadata = Optional.ofNullable(toHub.getMetadata());
        this.name = Optional.ofNullable(toHub.getSpec().getName());
        this.appDefinition = Optional.ofNullable(toHub.getSpec().getAppDefinition());
        this.user = Optional.ofNullable(toHub.getSpec().getUser());
        this.url = Optional.ofNullable(toHub.getSpec().getUrl());
        this.error = Optional.ofNullable(toHub.getSpec().getError());
        this.workspace = Optional.ofNullable(toHub.getSpec().getWorkspace());
        this.lastActivity = Optional.ofNullable(toHub.getSpec().getLastActivity());
        this.sessionSecret = Optional.ofNullable(toHub.getSpec().getSessionSecret());
        this.envVars = Optional.ofNullable(toHub.getSpec().getEnvVars());
        this.envVarsFromConfigMaps = Optional.ofNullable(toHub.getSpec().getEnvVarsFromConfigMaps());
        this.envVarsFromSecrets = Optional.ofNullable(toHub.getSpec().getEnvVarsFromSecrets());
        // Status is not a required field
        if (toHub.getStatus() != null) {
            this.operatorStatus = Optional.ofNullable(toHub.getStatus().getOperatorStatus());
            this.operatorMessage = Optional.ofNullable(toHub.getStatus().getOperatorMessage());
        } else {
            this.operatorStatus = Optional.empty();
            this.operatorMessage = Optional.empty();
        }
    }

    public Optional<ObjectMeta> getMetadata() {
        return metadata;
    }

    public Optional<String> getName() {
        return name;
    }

    public Optional<String> getAppDefinition() {
        return appDefinition;
    }

    public Optional<String> getUser() {
        return user;
    }

    public Optional<String> getUrl() {
        return url;
    }

    public Optional<String> getError() {
        return error;
    }

    public Optional<String> getWorkspace() {
        return workspace;
    }

    public Optional<Long> getLastActivity() {
        return lastActivity;
    }

    public Optional<String> getSessionSecret() {
        return sessionSecret;
    }

    public Optional<Map<String, String>> getEnvVars() {
        return envVars;
    }

    public Optional<List<String>> getEnvVarsFromConfigMaps() {
        return envVarsFromConfigMaps;
    }

    public Optional<List<String>> getEnvVarsFromSecrets() {
        return envVarsFromSecrets;
    }

    public Optional<String> getOperatorStatus() {
        return operatorStatus;
    }

    public Optional<String> getOperatorMessage() {
        return operatorMessage;
    }

}
