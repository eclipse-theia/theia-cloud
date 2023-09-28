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
package org.eclipse.theia.cloud.common.k8s.resource.appdefinition.hub;

import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.v1beta7.AppDefinitionV1beta7Spec;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AppDefinitionHubSpec {

    private String name;

    private String image;

    private String imagePullPolicy;

    private String pullSecret;

    private int uid;

    private int port;

    private String ingressname;

    private int minInstances;

    private Integer maxInstances;

    private Timeout timeout;

    private String requestsMemory;

    private String requestsCpu;

    private String limitsMemory;

    private String limitsCpu;

    private int downlinkLimit;// kilobits per second

    private int uplinkLimit;// kilobits per second

    private String mountPath;

    private Monitor monitor;

    /**
     * Default constructor.
     */
    public AppDefinitionHubSpec() {
    }

    public AppDefinitionHubSpec(AppDefinitionSpec toHub) {
	this.name = toHub.getName();
	this.image = toHub.getImage();
	this.imagePullPolicy = toHub.getImagePullPolicy();
	this.pullSecret = toHub.getPullSecret();
	this.uid = toHub.getUid();
	this.port = toHub.getPort();
	this.ingressname = toHub.getIngressname();
	this.minInstances = toHub.getMinInstances();
	this.maxInstances = toHub.getMaxInstances();
	this.requestsMemory = toHub.getRequestsMemory();
	this.requestsCpu = toHub.getRequestsCpu();
	this.limitsMemory = toHub.getLimitsMemory();
	this.limitsCpu = toHub.getLimitsCpu();
	this.downlinkLimit = toHub.getDownlinkLimit();
	this.uplinkLimit = toHub.getUplinkLimit();
	this.mountPath = toHub.getMountPath();

	this.timeout = new Timeout();
	if (toHub.getTimeout() != null) {
	    this.timeout.limit = toHub.getTimeout().getLimit();
	    this.timeout.strategy = toHub.getTimeout().getStrategy();
	}

	this.monitor = new Monitor();
	if (toHub.getMonitor() != null) {
	    this.monitor.port = toHub.getMonitor().getPort();

	    this.monitor.activityTracker = new Monitor.ActivityTracker();
	    if (toHub.getMonitor().getActivityTracker() != null) {
		this.monitor.activityTracker.timeoutAfter = toHub.getMonitor().getActivityTracker().getTimeoutAfter();
		this.monitor.activityTracker.notifyAfter = toHub.getMonitor().getActivityTracker().getNotifyAfter();
	    }
	}
    }

    public AppDefinitionHubSpec(AppDefinitionV1beta7Spec toHub) {
	this.name = toHub.getName();
	this.image = toHub.getImage();
	this.imagePullPolicy = toHub.getImagePullPolicy();
	this.pullSecret = toHub.getPullSecret();
	this.uid = toHub.getUid();
	this.port = toHub.getPort();
	this.ingressname = toHub.getIngressname();
	this.minInstances = toHub.getMinInstances();
	this.maxInstances = toHub.getMaxInstances();
	this.requestsMemory = toHub.getRequestsMemory();
	this.requestsCpu = toHub.getRequestsCpu();
	this.limitsMemory = toHub.getLimitsMemory();
	this.limitsCpu = toHub.getLimitsCpu();
	this.downlinkLimit = toHub.getDownlinkLimit();
	this.uplinkLimit = toHub.getUplinkLimit();
	this.mountPath = toHub.getMountPath();

	this.timeout = new Timeout();
	if (toHub.getTimeout() != null) {
	    this.timeout.limit = toHub.getTimeout().getLimit();
	    this.timeout.strategy = toHub.getTimeout().getStrategy();
	}

	this.monitor = new Monitor();
	if (toHub.getMonitor() != null) {
	    this.monitor.port = toHub.getMonitor().getPort();

	    this.monitor.activityTracker = new Monitor.ActivityTracker();
	    if (toHub.getMonitor().getActivityTracker() != null) {
		this.monitor.activityTracker.timeoutAfter = toHub.getMonitor().getActivityTracker().getTimeoutAfter();
		this.monitor.activityTracker.notifyAfter = toHub.getMonitor().getActivityTracker().getNotifyAfter();
	    }
	}
    }

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

    public String getIngressname() {
	return ingressname;
    }

    public int getMinInstances() {
	return minInstances;
    }

    public Integer getMaxInstances() {
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
		+ ", pullSecret=" + pullSecret + ", uid=" + uid + ", port=" + port + ", ingressname=" + ingressname
		+ ", minInstances=" + minInstances + ", maxInstances=" + maxInstances + ", timeout=" + timeout
		+ ", requestsMemory=" + requestsMemory + ", requestsCpu=" + requestsCpu + ", limitsMemory="
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

	public ActivityTracker getActivityTracker() {
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
