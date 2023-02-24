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
package org.eclipse.theia.cloud.operator;

import picocli.CommandLine.Option;

public class TheiaCloudArguments {

    public enum CloudProvider {
	K8S, MINIKUBE
    }

    public enum BandwidthLimiter {
	K8SANNOTATION, WONDERSHAPER, K8SANNOTATIONANDWONDERSHAPER
    }

    @Option(names = { "--keycloak" }, description = "Whether to use keycloak", required = false)
    private boolean useKeycloak;

    @Option(names = { "--eagerStart" }, description = "Whether sessions shall be started early.", required = false)
    private boolean eagerStart;

    @Option(names = {
	    "--enableMonitor" }, description = "Whether the monitor service should be started.", required = false)
    private boolean enableMonitor;

    @Option(names = {
	    "--enableActivityTracker" }, description = "Whether the activityTracker module is activated.", required = false)
    private boolean enableActivityTracker;

    @Option(names = {
	    "--monitorInterval" }, description = "Number of minutes between each ping of the monitor.", required = false, defaultValue = "1")
    private Integer monitorInterval;

    @Option(names = {
	    "--cloudProvider" }, description = "The cloud provider where Theia.Cloud is deployed", required = false)
    private CloudProvider cloudProvider;

    @Option(names = {
	    "--bandwidthLimiter" }, description = "The method of limiting network bandwidth", required = false)
    private BandwidthLimiter bandwidthLimiter;

    @Option(names = { "--wondershaperImage" }, description = "Wondershaper Image", required = false)
    private String wondershaperImage = "theiacloud/theia-cloud-wondershaper";

    @Option(names = { "--serviceUrl" }, description = "URL of the Theia Cloud Service", required = false)
    private String serviceUrl;

    @Option(names = {
	    "--sessionsPerUser" }, description = "Number of active sessions a single user is allowed to start.", required = false)
    private Integer sessionsPerUser;

    @Option(names = { "--appId" }, description = "Application ID necessary for service calls", required = false)
    private String appId;

    @Option(names = {
	    "--usePaths" }, description = "Whether paths instead of subdomains are used for the various components", required = false)
    private boolean usePaths;

    @Option(names = { "--instancesPath" }, description = "Subpath instances are hosted at", required = false)
    private String instancesPath;

    @Option(names = {
	    "--storageClassName" }, description = "K8s storage class to use for persistent workspace volume claims.", required = false)
    private String storageClassName;

    @Option(names = {
	    "--requestedStorage" }, description = "Amount of storage requested for persistent workspace volume claims.", required = false)
    private String requestedStorage;

    public boolean isUseKeycloak() {
	return useKeycloak;
    }

    public boolean isEagerStart() {
	return eagerStart;
    }

    public boolean isEnableMonitor() {
	return enableMonitor;
    }

    public boolean isEnableActivityTracker() {
	return enableActivityTracker;
    }

    public CloudProvider getCloudProvider() {
	return cloudProvider;
    }

    public BandwidthLimiter getBandwidthLimiter() {
	return bandwidthLimiter;
    }

    public String getWondershaperImage() {
	return wondershaperImage;
    }

    public String getServiceUrl() {
	return serviceUrl;
    }

    public Integer getSessionsPerUser() {
	return sessionsPerUser;
    }

    public Integer getMonitorInterval() {
	return monitorInterval;
    }

    public String getAppId() {
	return appId;
    }

    public boolean isUsePaths() {
	return usePaths;
    }

    public String getInstancesPath() {
	return instancesPath;
    }

    public String getStorageClassName() {
	return storageClassName;
    }

    public String getRequestedStorage() {
	return requestedStorage;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((appId == null) ? 0 : appId.hashCode());
	result = prime * result + ((bandwidthLimiter == null) ? 0 : bandwidthLimiter.hashCode());
	result = prime * result + ((cloudProvider == null) ? 0 : cloudProvider.hashCode());
	result = prime * result + (eagerStart ? 1231 : 1237);
	result = prime * result + (enableActivityTracker ? 1231 : 1237);
	result = prime * result + (enableMonitor ? 1231 : 1237);
	result = prime * result + ((instancesPath == null) ? 0 : instancesPath.hashCode());
	result = prime * result + ((monitorInterval == null) ? 0 : monitorInterval.hashCode());
	result = prime * result + ((requestedStorage == null) ? 0 : requestedStorage.hashCode());
	result = prime * result + ((serviceUrl == null) ? 0 : serviceUrl.hashCode());
	result = prime * result + ((sessionsPerUser == null) ? 0 : sessionsPerUser.hashCode());
	result = prime * result + ((storageClassName == null) ? 0 : storageClassName.hashCode());
	result = prime * result + (useKeycloak ? 1231 : 1237);
	result = prime * result + (usePaths ? 1231 : 1237);
	result = prime * result + ((wondershaperImage == null) ? 0 : wondershaperImage.hashCode());
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	TheiaCloudArguments other = (TheiaCloudArguments) obj;
	if (appId == null) {
	    if (other.appId != null)
		return false;
	} else if (!appId.equals(other.appId))
	    return false;
	if (bandwidthLimiter != other.bandwidthLimiter)
	    return false;
	if (cloudProvider != other.cloudProvider)
	    return false;
	if (eagerStart != other.eagerStart)
	    return false;
	if (enableActivityTracker != other.enableActivityTracker)
	    return false;
	if (enableMonitor != other.enableMonitor)
	    return false;
	if (instancesPath == null) {
	    if (other.instancesPath != null)
		return false;
	} else if (!instancesPath.equals(other.instancesPath))
	    return false;
	if (monitorInterval == null) {
	    if (other.monitorInterval != null)
		return false;
	} else if (!monitorInterval.equals(other.monitorInterval))
	    return false;
	if (requestedStorage == null) {
	    if (other.requestedStorage != null)
		return false;
	} else if (!requestedStorage.equals(other.requestedStorage))
	    return false;
	if (serviceUrl == null) {
	    if (other.serviceUrl != null)
		return false;
	} else if (!serviceUrl.equals(other.serviceUrl))
	    return false;
	if (sessionsPerUser == null) {
	    if (other.sessionsPerUser != null)
		return false;
	} else if (!sessionsPerUser.equals(other.sessionsPerUser))
	    return false;
	if (storageClassName == null) {
	    if (other.storageClassName != null)
		return false;
	} else if (!storageClassName.equals(other.storageClassName))
	    return false;
	if (useKeycloak != other.useKeycloak)
	    return false;
	if (usePaths != other.usePaths)
	    return false;
	if (wondershaperImage == null) {
	    if (other.wondershaperImage != null)
		return false;
	} else if (!wondershaperImage.equals(other.wondershaperImage))
	    return false;
	return true;
    }

    @Override
    public String toString() {
	return "TheiaCloudArguments [useKeycloak=" + useKeycloak + ", eagerStart=" + eagerStart + ", enableMonitor="
		+ enableMonitor + ", enableActivityTracker=" + enableActivityTracker + ", monitorInterval="
		+ monitorInterval + ", cloudProvider=" + cloudProvider + ", bandwidthLimiter=" + bandwidthLimiter
		+ ", wondershaperImage=" + wondershaperImage + ", serviceUrl=" + serviceUrl + ", sessionsPerUser="
		+ sessionsPerUser + ", appId=" + appId + ", usePaths=" + usePaths + ", instancesPath=" + instancesPath
		+ ", storageClassName=" + storageClassName + ", requestedStorage=" + requestedStorage + "]";
    }

}
