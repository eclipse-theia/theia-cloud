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
package org.eclipse.theia.cloud.common.k8s.resource.workspace.v1beta4;

import org.eclipse.theia.cloud.common.k8s.resource.UserScopedSpec;
import org.eclipse.theia.cloud.common.k8s.resource.workspace.hub.WorkspaceHub;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@Deprecated
@JsonDeserialize()
public class WorkspaceV1beta4Spec implements UserScopedSpec {

    @JsonProperty("name")
    private String name;

    @JsonProperty("label")
    private String label;

    @JsonProperty("appDefinition")
    private String appDefinition;

    @JsonProperty("user")
    private String user;

    @JsonProperty("storage")
    private String storage;

    /**
     * Default constructor.
     */
    public WorkspaceV1beta4Spec() {
    }

    public WorkspaceV1beta4Spec(String name, String label, String appDefinition, String user) {
        this.name = name;
        this.appDefinition = appDefinition;
        this.user = user;
        this.label = label;
    }

    public WorkspaceV1beta4Spec(WorkspaceHub spec) {
        this.name = spec.getName().orElse(null); // required
        this.label = spec.getLabel().orElse(null);
        this.appDefinition = spec.getAppDefinition().orElse(null);
        this.user = spec.getUser().orElse(null); // required
        this.storage = spec.getStorage().orElse(null);
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public void setAppDefinition(String appDefinition) {
        this.appDefinition = appDefinition;
    }

    /**
     * Returns the last app definition with which this workspace was started.
     * 
     * @return last app definition
     */
    public String getAppDefinition() {
        return appDefinition;
    }

    /**
     * Returns the user that created this workspace.
     */
    @Override
    public String getUser() {
        return user;
    }

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }

    public boolean hasStorage() {
        return getStorage() != null && !getStorage().isBlank();
    }

    @Override
    public String toString() {
        return "WorkspaceSpec [name=" + name + ", label=" + label + ", appDefinition=" + appDefinition + ", user="
                + user + ", storage=" + storage + "]";
    }

}
