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
package org.eclipse.theia.cloud.common.k8s.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize()
public class AppDefinitionSpec {

    public static final String API = "theia.cloud/v4beta";
    public static final String KIND = "AppDefinition";
    public static final String CRD_NAME = "appdefinitions.theia.cloud";

    @JsonProperty("name")
    private String name;

    @JsonProperty("image")
    private String image;

    @JsonProperty("imagePullPolicy")
    private String imagePullPolicy;

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

    @JsonProperty("timeout")
    private Timeout timeout;

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

    @JsonProperty("mountPath")
    private String mountPath;

    @JsonProperty("monitor")
    private Monitor monitor;

    public String getName() {
	return name;
    }

    public String getImage() {
	return image;
    }

    public String getImagePullPolicy() {
	return imagePullPolicy;
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

    public Timeout getTimeout() {
	return timeout;
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

    public String getMountPath() {
	return mountPath;
    }

    public Monitor getMonitor() {
	return monitor;
    }

    @Override
    public String toString() {
	return "AppDefinitionSpec [name=" + name + ", image=" + image + ", imagePullPolicy=" + imagePullPolicy
		+ ", pullSecret=" + pullSecret + ", uid=" + uid + ", port=" + port + ", host=" + host + ", ingressname="
		+ ingressname + ", minInstances=" + minInstances + ", maxInstances=" + maxInstances + ", timeout="
		+ timeout + ", requestsMemory=" + requestsMemory + ", requestsCpu=" + requestsCpu + ", limitsMemory="
		+ limitsMemory + ", limitsCpu=" + limitsCpu + ", downlinkLimit=" + downlinkLimit + ", uplinkLimit="
		+ uplinkLimit + ", mountPath=" + mountPath + "]";
    }

    public static class Timeout {
	@JsonProperty("limit")
	private int limit;

	@JsonProperty("strategy")
	private String strategy;

	public Timeout() {
	}

	public Timeout(String strategy, int limit) {
	    this.strategy = strategy;
	    this.limit = limit;
	}

	public int getLimit() {
	    return limit;
	}

	public String getStrategy() {
	    return strategy;
	}

	@Override
	public String toString() {
	    return "Timeout [limit=" + limit + ", strategy=" + strategy + "]";
	}
    }

    public static class Monitor {
	@JsonProperty("port")
	private int port;

	@JsonProperty("activityTracker")
	private ActivityTracker activityTracker;

	public Monitor() {
	}

	public Monitor(ActivityTracker activityTracker, int port) {
	    this.activityTracker = activityTracker;
	    this.port = port;
	}

	public int getPort() {
	    return port;
	}

	public ActivityTracker getActivityTrackerModule() {
	    return activityTracker;
	}

	@Override
	public String toString() {
	    return "Monitor [activityTracker=" + activityTracker + ", port=" + port + "]";
	}

	public static class ActivityTracker {
	    @JsonProperty("timeoutAfter")
	    private int timeoutAfter;

	    @JsonProperty("notifyAfter")
	    private int notifyAfter;

	    public ActivityTracker() {
	    }

	    public ActivityTracker(int timeoutAfter, int notifyAfter) {
		this.timeoutAfter = timeoutAfter;
		this.notifyAfter = notifyAfter;
	    }

	    public int getTimeoutAfter() {
		return timeoutAfter;
	    }

	    public int getNotifyAfter() {
		return notifyAfter;
	    }

	    @Override
	    public String toString() {
		return "ActivityTracker [timeoutAfter=" + timeoutAfter + ", notifyAfter=" + notifyAfter + "]";
	    }

	}
    }
}
