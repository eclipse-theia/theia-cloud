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

public class AppDefinitionHubSpec {

    private final String name;

    private final String image;

    private final String imagePullPolicy;

    private final String pullSecret;

    private final int uid;

    private final int port;

    private final String ingressname;

    private final int minInstances;

    private final Integer maxInstances;

    private final TimeoutHub timeout;

    private final String requestsMemory;

    private final String requestsCpu;

    private final String limitsMemory;

    private final String limitsCpu;

    private final int downlinkLimit;// kilobits per second

    private final int uplinkLimit;// kilobits per second

    private final String mountPath;

    private final MonitorHub monitor;

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

	if (toHub.getTimeout() != null) {
	    this.timeout = new TimeoutHub(//
		    toHub.getTimeout().getStrategy(), //
		    toHub.getTimeout().getLimit());
	} else {
	    this.timeout = new TimeoutHub();
	}

	if (toHub.getMonitor() != null) {

	    MonitorHub.ActivityTrackerHub activityTracker;
	    if (toHub.getMonitor().getActivityTracker() != null) {
		activityTracker = new MonitorHub.ActivityTrackerHub(
			toHub.getMonitor().getActivityTracker().getTimeoutAfter(),
			toHub.getMonitor().getActivityTracker().getNotifyAfter());
	    } else {
		activityTracker = new MonitorHub.ActivityTrackerHub();
	    }

	    this.monitor = new MonitorHub(activityTracker, toHub.getMonitor().getPort());
	} else {
	    this.monitor = new MonitorHub();
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

	if (toHub.getTimeout() != null) {
	    this.timeout = new TimeoutHub(//
		    toHub.getTimeout().getStrategy(), //
		    toHub.getTimeout().getLimit());
	} else {
	    this.timeout = new TimeoutHub();
	}

	if (toHub.getMonitor() != null) {
	    MonitorHub.ActivityTrackerHub activityTracker;
	    if (toHub.getMonitor().getActivityTracker() != null) {
		activityTracker = new MonitorHub.ActivityTrackerHub(
			toHub.getMonitor().getActivityTracker().getTimeoutAfter(),
			toHub.getMonitor().getActivityTracker().getNotifyAfter());
	    } else {
		activityTracker = new MonitorHub.ActivityTrackerHub();
	    }

	    this.monitor = new MonitorHub(activityTracker, toHub.getMonitor().getPort());
	} else {
	    this.monitor = new MonitorHub();
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

    public TimeoutHub getTimeout() {
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

    public MonitorHub getMonitor() {
	return monitor;
    }

    @Override
    public String toString() {
	return "AppDefinitionHubSpec [name=" + name + ", image=" + image + ", imagePullPolicy=" + imagePullPolicy
		+ ", pullSecret=" + pullSecret + ", uid=" + uid + ", port=" + port + ", ingressname=" + ingressname
		+ ", minInstances=" + minInstances + ", maxInstances=" + maxInstances + ", timeout=" + timeout
		+ ", requestsMemory=" + requestsMemory + ", requestsCpu=" + requestsCpu + ", limitsMemory="
		+ limitsMemory + ", limitsCpu=" + limitsCpu + ", downlinkLimit=" + downlinkLimit + ", uplinkLimit="
		+ uplinkLimit + ", mountPath=" + mountPath + ", monitor=" + monitor + "]";
    }

    public static class TimeoutHub {
	private final int limit;

	private final String strategy;

	public TimeoutHub(String strategy, int limit) {
	    this.strategy = strategy;
	    this.limit = limit;
	}

	public TimeoutHub() {
	    this.strategy = "FIXEDTIME";
	    this.limit = 60;
	}

	public int getLimit() {
	    return limit;
	}

	public String getStrategy() {
	    return strategy;
	}

	@Override
	public String toString() {
	    return "TimeoutHub [limit=" + limit + ", strategy=" + strategy + "]";
	}

    }

    public static class MonitorHub {
	private final int port;

	private final ActivityTrackerHub activityTracker;

	public MonitorHub(ActivityTrackerHub activityTracker, int port) {
	    this.activityTracker = activityTracker;
	    this.port = port;
	}

	public MonitorHub() {
	    this.activityTracker = new ActivityTrackerHub();
	    this.port = 3000;
	}

	public int getPort() {
	    return port;
	}

	public ActivityTrackerHub getActivityTracker() {
	    return activityTracker;
	}

	@Override
	public String toString() {
	    return "MonitorHub [port=" + port + ", activityTracker=" + activityTracker + "]";
	}

	public static class ActivityTrackerHub {
	    private final int timeoutAfter;

	    private final int notifyAfter;

	    public ActivityTrackerHub(int timeoutAfter, int notifyAfter) {
		this.timeoutAfter = timeoutAfter;
		this.notifyAfter = notifyAfter;
	    }

	    public ActivityTrackerHub() {
		this.timeoutAfter = 60;
		this.notifyAfter = 30;
	    }

	    public int getTimeoutAfter() {
		return timeoutAfter;
	    }

	    public int getNotifyAfter() {
		return notifyAfter;
	    }

	    @Override
	    public String toString() {
		return "ActivityTrackerHub [timeoutAfter=" + timeoutAfter + ", notifyAfter=" + notifyAfter + "]";
	    }

	}
    }

}
