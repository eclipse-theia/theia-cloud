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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;

import io.fabric8.kubernetes.api.model.ObjectMeta;

public class AppDefinitionHub {

    final Optional<ObjectMeta> metadata;
    final Optional<String> name;
    final Optional<String> image;
    final Optional<String> imagePullPolicy;
    final Optional<String> pullSecret;
    final OptionalInt uid;
    final OptionalInt port;
    final Optional<String> ingressname;
    final OptionalInt minInstances;
    final OptionalInt maxInstances;
    final Optional<String> requestsMemory;
    final Optional<String> requestsCpu;
    final Optional<String> limitsMemory;
    final Optional<String> limitsCpu;
    final OptionalInt downlinkLimit;// kilobits per second
    final OptionalInt uplinkLimit;// kilobits per second
    final Optional<String> mountPath;
    final Optional<List<String>> ingressHostnamePrefixes;

    final OptionalInt timeoutLimit;
    @Deprecated
    Optional<String> timeoutStrategy = Optional.empty();

    final OptionalInt monitorPort;
    final OptionalInt monitorActivityTrackerTimeoutAfter;
    final OptionalInt monitorActivityTrackerNotifyAfter;

    final Optional<String> operatorStatus;
    final Optional<String> operatorMessage;

    final Optional<Map<String, String>> options;

    public AppDefinitionHub(AppDefinition toHub) {
	this.metadata = Optional.ofNullable(toHub.getMetadata());
	this.name = Optional.ofNullable(toHub.getSpec().getName());
	this.image = Optional.ofNullable(toHub.getSpec().getImage());
	this.imagePullPolicy = Optional.ofNullable(toHub.getSpec().getImagePullPolicy());
	this.pullSecret = Optional.ofNullable(toHub.getSpec().getPullSecret());
	this.uid = OptionalInt.of(toHub.getSpec().getUid());
	this.port = OptionalInt.of(toHub.getSpec().getPort());
	this.ingressname = Optional.ofNullable(toHub.getSpec().getIngressname());
	this.minInstances = OptionalInt.of(toHub.getSpec().getMinInstances());
	this.maxInstances = OptionalInt.of(toHub.getSpec().getMaxInstances());
	this.requestsMemory = Optional.ofNullable(toHub.getSpec().getRequestsMemory());
	this.requestsCpu = Optional.ofNullable(toHub.getSpec().getRequestsCpu());
	this.limitsMemory = Optional.ofNullable(toHub.getSpec().getLimitsMemory());
	this.limitsCpu = Optional.ofNullable(toHub.getSpec().getLimitsCpu());
	this.downlinkLimit = OptionalInt.of(toHub.getSpec().getDownlinkLimit());
	this.uplinkLimit = OptionalInt.of(toHub.getSpec().getUplinkLimit());
	this.mountPath = Optional.ofNullable(toHub.getSpec().getMountPath());
	this.options = Optional.ofNullable(toHub.getSpec().getOptions());
	this.ingressHostnamePrefixes = Optional.ofNullable(toHub.getSpec().getIngressHostnamePrefixes());

	this.timeoutLimit = OptionalInt.of(toHub.getSpec().getTimeout());

	if (toHub.getSpec().getMonitor() != null) {
	    this.monitorPort = OptionalInt.of(toHub.getSpec().getMonitor().getPort());
	    if (toHub.getSpec().getMonitor().getActivityTracker() != null) {
		this.monitorActivityTrackerNotifyAfter = OptionalInt
			.of(toHub.getSpec().getMonitor().getActivityTracker().getNotifyAfter());
		this.monitorActivityTrackerTimeoutAfter = OptionalInt
			.of(toHub.getSpec().getMonitor().getActivityTracker().getTimeoutAfter());
	    } else {
		this.monitorActivityTrackerNotifyAfter = OptionalInt.empty();
		this.monitorActivityTrackerTimeoutAfter = OptionalInt.empty();
	    }
	} else {
	    this.monitorPort = OptionalInt.empty();
	    this.monitorActivityTrackerNotifyAfter = OptionalInt.empty();
	    this.monitorActivityTrackerTimeoutAfter = OptionalInt.empty();
	}

	// Status is not a required field
	if (toHub.getStatus() != null) {
	    this.operatorStatus = Optional.ofNullable(toHub.getNonNullStatus().getOperatorStatus());
	    this.operatorMessage = Optional.ofNullable(toHub.getNonNullStatus().getOperatorMessage());
	} else {
	    this.operatorStatus = Optional.empty();
	    this.operatorMessage = Optional.empty();
	}
    }

    @SuppressWarnings("deprecation")
    public AppDefinitionHub(
	    org.eclipse.theia.cloud.common.k8s.resource.appdefinition.v1beta9.AppDefinitionV1beta9 toHub) {
	this.metadata = Optional.ofNullable(toHub.getMetadata());
	this.name = Optional.ofNullable(toHub.getSpec().getName());
	this.image = Optional.ofNullable(toHub.getSpec().getImage());
	this.imagePullPolicy = Optional.ofNullable(toHub.getSpec().getImagePullPolicy());
	this.pullSecret = Optional.ofNullable(toHub.getSpec().getPullSecret());
	this.uid = OptionalInt.of(toHub.getSpec().getUid());
	this.port = OptionalInt.of(toHub.getSpec().getPort());
	this.ingressname = Optional.ofNullable(toHub.getSpec().getIngressname());
	this.minInstances = OptionalInt.of(toHub.getSpec().getMinInstances());
	this.maxInstances = OptionalInt.of(toHub.getSpec().getMaxInstances());
	this.requestsMemory = Optional.ofNullable(toHub.getSpec().getRequestsMemory());
	this.requestsCpu = Optional.ofNullable(toHub.getSpec().getRequestsCpu());
	this.limitsMemory = Optional.ofNullable(toHub.getSpec().getLimitsMemory());
	this.limitsCpu = Optional.ofNullable(toHub.getSpec().getLimitsCpu());
	this.downlinkLimit = OptionalInt.of(toHub.getSpec().getDownlinkLimit());
	this.uplinkLimit = OptionalInt.of(toHub.getSpec().getUplinkLimit());
	this.mountPath = Optional.ofNullable(toHub.getSpec().getMountPath());
	this.options = Optional.empty();
	this.ingressHostnamePrefixes = Optional.empty();

	this.timeoutLimit = OptionalInt.of(toHub.getSpec().getTimeout());

	if (toHub.getSpec().getMonitor() != null) {
	    this.monitorPort = OptionalInt.of(toHub.getSpec().getMonitor().getPort());
	    if (toHub.getSpec().getMonitor().getActivityTracker() != null) {
		this.monitorActivityTrackerNotifyAfter = OptionalInt
			.of(toHub.getSpec().getMonitor().getActivityTracker().getNotifyAfter());
		this.monitorActivityTrackerTimeoutAfter = OptionalInt
			.of(toHub.getSpec().getMonitor().getActivityTracker().getTimeoutAfter());
	    } else {
		this.monitorActivityTrackerNotifyAfter = OptionalInt.empty();
		this.monitorActivityTrackerTimeoutAfter = OptionalInt.empty();
	    }
	} else {
	    this.monitorPort = OptionalInt.empty();
	    this.monitorActivityTrackerNotifyAfter = OptionalInt.empty();
	    this.monitorActivityTrackerTimeoutAfter = OptionalInt.empty();
	}

	// Status is not a required field
	if (toHub.getStatus() != null) {
	    this.operatorStatus = Optional.ofNullable(toHub.getNonNullStatus().getOperatorStatus());
	    this.operatorMessage = Optional.ofNullable(toHub.getNonNullStatus().getOperatorMessage());
	} else {
	    this.operatorStatus = Optional.empty();
	    this.operatorMessage = Optional.empty();
	}
    }

    @SuppressWarnings("deprecation")
    public AppDefinitionHub(
	    org.eclipse.theia.cloud.common.k8s.resource.appdefinition.v1beta8.AppDefinitionV1beta8 toHub) {
	this.metadata = Optional.ofNullable(toHub.getMetadata());
	this.name = Optional.ofNullable(toHub.getSpec().getName());
	this.image = Optional.ofNullable(toHub.getSpec().getImage());
	this.imagePullPolicy = Optional.ofNullable(toHub.getSpec().getImagePullPolicy());
	this.pullSecret = Optional.ofNullable(toHub.getSpec().getPullSecret());
	this.uid = OptionalInt.of(toHub.getSpec().getUid());
	this.port = OptionalInt.of(toHub.getSpec().getPort());
	this.ingressname = Optional.ofNullable(toHub.getSpec().getIngressname());
	this.minInstances = OptionalInt.of(toHub.getSpec().getMinInstances());
	this.maxInstances = OptionalInt.of(toHub.getSpec().getMaxInstances());
	this.requestsMemory = Optional.ofNullable(toHub.getSpec().getRequestsMemory());
	this.requestsCpu = Optional.ofNullable(toHub.getSpec().getRequestsCpu());
	this.limitsMemory = Optional.ofNullable(toHub.getSpec().getLimitsMemory());
	this.limitsCpu = Optional.ofNullable(toHub.getSpec().getLimitsCpu());
	this.downlinkLimit = OptionalInt.of(toHub.getSpec().getDownlinkLimit());
	this.uplinkLimit = OptionalInt.of(toHub.getSpec().getUplinkLimit());
	this.mountPath = Optional.ofNullable(toHub.getSpec().getMountPath());
	this.options = Optional.empty();
	this.ingressHostnamePrefixes = Optional.empty();

	if (toHub.getSpec().getTimeout() != null) {
	    this.timeoutLimit = OptionalInt.of(toHub.getSpec().getTimeout().getLimit());
	    this.timeoutStrategy = Optional.of(toHub.getSpec().getTimeout().getStrategy());
	} else {
	    this.timeoutLimit = OptionalInt.empty();
	    this.timeoutStrategy = Optional.empty();
	}

	if (toHub.getSpec().getMonitor() != null) {
	    this.monitorPort = OptionalInt.of(toHub.getSpec().getMonitor().getPort());
	    if (toHub.getSpec().getMonitor().getActivityTracker() != null) {
		this.monitorActivityTrackerNotifyAfter = OptionalInt
			.of(toHub.getSpec().getMonitor().getActivityTracker().getNotifyAfter());
		this.monitorActivityTrackerTimeoutAfter = OptionalInt
			.of(toHub.getSpec().getMonitor().getActivityTracker().getTimeoutAfter());
	    } else {
		this.monitorActivityTrackerNotifyAfter = OptionalInt.empty();
		this.monitorActivityTrackerTimeoutAfter = OptionalInt.empty();
	    }
	} else {
	    this.monitorPort = OptionalInt.empty();
	    this.monitorActivityTrackerNotifyAfter = OptionalInt.empty();
	    this.monitorActivityTrackerTimeoutAfter = OptionalInt.empty();
	}

	// Status is not a required field
	if (toHub.getStatus() != null) {
	    this.operatorStatus = Optional.ofNullable(toHub.getNonNullStatus().getOperatorStatus());
	    this.operatorMessage = Optional.ofNullable(toHub.getNonNullStatus().getOperatorMessage());
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

    public Optional<String> getImage() {
	return image;
    }

    public Optional<String> getImagePullPolicy() {
	return imagePullPolicy;
    }

    public Optional<String> getPullSecret() {
	return pullSecret;
    }

    public OptionalInt getUid() {
	return uid;
    }

    public OptionalInt getPort() {
	return port;
    }

    public Optional<String> getIngressname() {
	return ingressname;
    }

    public OptionalInt getMinInstances() {
	return minInstances;
    }

    public OptionalInt getMaxInstances() {
	return maxInstances;
    }

    public Optional<String> getRequestsMemory() {
	return requestsMemory;
    }

    public Optional<String> getRequestsCpu() {
	return requestsCpu;
    }

    public Optional<String> getLimitsMemory() {
	return limitsMemory;
    }

    public Optional<String> getLimitsCpu() {
	return limitsCpu;
    }

    public OptionalInt getDownlinkLimit() {
	return downlinkLimit;
    }

    public OptionalInt getUplinkLimit() {
	return uplinkLimit;
    }

    public Optional<String> getMountPath() {
	return mountPath;
    }

    public OptionalInt getTimeoutLimit() {
	return timeoutLimit;
    }

    public Optional<String> getTimeoutStrategy() {
	return timeoutStrategy;
    }

    public OptionalInt getMonitorPort() {
	return monitorPort;
    }

    public OptionalInt getMonitorActivityTrackerTimeoutAfter() {
	return monitorActivityTrackerTimeoutAfter;
    }

    public OptionalInt getMonitorActivityTrackerNotifyAfter() {
	return monitorActivityTrackerNotifyAfter;
    }

    public Optional<String> getOperatorStatus() {
	return operatorStatus;
    }

    public Optional<String> getOperatorMessage() {
	return operatorMessage;
    }

    public Optional<Map<String, String>> getOptions() {
	return options;
    }

    public Optional<List<String>> getIngressHostnamePrefixes() {
	return ingressHostnamePrefixes;
    }

}
