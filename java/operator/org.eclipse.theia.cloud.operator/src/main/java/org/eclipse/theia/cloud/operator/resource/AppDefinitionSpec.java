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
public class AppDefinitionSpec {

    public static final String API = "theia.cloud/v2beta";
    public static final String KIND = "AppDefinition";
    public static final String CRD_NAME = "appdefinitions.theia.cloud";

    @JsonProperty("name")
    private String name;

    @JsonProperty("image")
    private String image;

    @JsonProperty("pullSecret")
    private String pullSecret;

    @JsonProperty("uid")
    private int uid;

    @JsonProperty("port")
    private int port;

    @JsonProperty("host")
    private String host;

    @JsonProperty("ingressname")
    private String ingressname;

    @JsonProperty("minInstances")
    private int minInstances;

    @JsonProperty("maxInstances")
    private int maxInstances;

    @JsonProperty("killAfter")
    private int killAfter;

    @JsonProperty("requestsMemory")
    private String requestsMemory;

    @JsonProperty("requestsCpu")
    private String requestsCpu;

    @JsonProperty("limitsMemory")
    private String limitsMemory;

    @JsonProperty("limitsCpu")
    private String limitsCpu;

    @JsonProperty("downlinkLimit")
    private int downlinkLimit;// kilobits per second

    @JsonProperty("uplinkLimit")
    private int uplinkLimit;// kilobits per second

    public String getName() {
	return name;
    }

    public String getImage() {
	return image;
    }

    public String getPullSecret() {
	return pullSecret;
    }

    public int getUid() {
	return uid;
    }

    public int getPort() {
	return port;
    }

    public String getHost() {
	return host;
    }

    public String getIngressname() {
	return ingressname;
    }

    public int getMinInstances() {
	return minInstances;
    }

    public int getMaxInstances() {
	return maxInstances;
    }

    public int getKillAfter() {
	return killAfter;
    }

    public String getRequestsMemory() {
	return requestsMemory;
    }

    public String getRequestsCpu() {
	return requestsCpu;
    }

    public String getLimitsMemory() {
	return limitsMemory;
    }

    public String getLimitsCpu() {
	return limitsCpu;
    }

    public int getDownlinkLimit() {
	return downlinkLimit;
    }

    public int getUplinkLimit() {
	return uplinkLimit;
    }

    @Override
    public String toString() {
	return "AppDefinitionSpec [name=" + name + ", image=" + image + ", pullSecret=" + pullSecret + ", uid=" + uid
		+ ", port=" + port + ", host=" + host + ", ingressname=" + ingressname + ", minInstances="
		+ minInstances + ", maxInstances=" + maxInstances + ", killAfter=" + killAfter + ", requestsMemory="
		+ requestsMemory + ", requestsCpu=" + requestsCpu + ", limitsMemory=" + limitsMemory + ", limitsCpu="
		+ limitsCpu + ", downlinkLimit=" + downlinkLimit + ", uplinkLimit=" + uplinkLimit + "]";
    }

}
