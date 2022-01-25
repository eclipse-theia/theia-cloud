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
package org.eclipse.theia.cloud.operator.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize()
public class TemplateSpec {

    public static final String KIND = "Template";
    public static final String CRD_NAME = "templates.theia.cloud";

    @JsonProperty("name")
    private String name;

    @JsonProperty("image")
    private String image;

    @JsonProperty("instances")
    private int instances;

    @JsonProperty("host")
    private String host;

    public String getName() {
	return name;
    }

    public String getImage() {
	return image;
    }

    public int getInstances() {
	return instances;
    }

    public String getHost() {
	return host;
    }

    @Override
    public String toString() {
	return "TemplateSpec [name=" + name + ", image=" + image + ", instances=" + instances + ", host=" + host + "]";
    }

}
