/********************************************************************************
 * Copyright (C) 2022-2023 EclipseSource, Lockular, Ericsson, STMicroelectronics and 
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
package org.eclipse.theia.cloud.common.k8s.resource.appdefinition.v1beta9;

import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.hub.AppDefinitionHub;
import org.eclipse.theia.cloud.common.util.CustomResourceUtil;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Singular;
import io.fabric8.kubernetes.model.annotation.Version;

@Deprecated
@Version("v1beta9")
@Group("theia.cloud")
@Kind("AppDefinition")
@Singular("appdefinition")
@Plural("appdefinitions")
public class AppDefinitionV1beta9 extends CustomResource<AppDefinitionV1beta9Spec, AppDefinitionV1beta9Status>
	implements Namespaced {

    private static final long serialVersionUID = 8749670583218521755L;
    public static final String API = "theia.cloud/v1beta9";
    public static final String KIND = "AppDefinition";
    public static final String CRD_NAME = "appdefinitions.theia.cloud";

    @Override
    public String toString() {
	return CustomResourceUtil.toString(this);
    }

    public AppDefinitionV1beta9() {
    }

    public AppDefinitionV1beta9(AppDefinitionHub fromHub) {
	if (fromHub.getMetadata().isPresent()) {
	    this.setMetadata(fromHub.getMetadata().get());
	}
	this.spec = new AppDefinitionV1beta9Spec(fromHub);
	this.status = new AppDefinitionV1beta9Status(fromHub);
    }

    @JsonIgnore
    public AppDefinitionV1beta9Status getNonNullStatus() {
	return super.getStatus() != null ? super.getStatus() : new AppDefinitionV1beta9Status();
    }

}
