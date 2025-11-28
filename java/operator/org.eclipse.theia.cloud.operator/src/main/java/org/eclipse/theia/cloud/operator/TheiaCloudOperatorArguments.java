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
import java.util.logging.Logger;

public class TheiaCloudOperatorArguments {

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
            "--cloudProvider" }, description = "The cloud provider where Theia Cloud is deployed", required = false)
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

    @Option(names = { "--serviceAuthToken" }, description = "Service authentication token necessary for service calls", required = false)
    private String serviceAuthToken;
    
    @Option(names = { "--appId" }, description = "(Deprecated) Use --serviceAuthToken instead. Application ID necessary for service calls", required = false, hidden = true)
    private String appId;

    @Option(names = {
            "--instancesHost" }, description = "Hostname instances are hosted at. Does not include subpaths.", required = true)
    private String instancesHost;

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

    @Option(names = {
            "--keycloakURL" }, description = "The URL of the keycloak instance, if keycloak is enabled.", required = false)
    private String keycloakURL;

    @Option(names = {
            "--keycloakRealm" }, description = "The authentication realm, if keycloak is enabled.", required = false)
    private String keycloakRealm;

    @Option(names = {
            "--keycloakClientId" }, description = "The client id of the auth application, if keycloak is enabled", required = false)
    private String keycloakClientId;

    @Option(names = {
            "--leaderLeaseDuration" }, description = "The lease duration for leader election in seconds.", required = false)
    private int leaderLeaseDuration = 10;

    @Option(names = {
            "--leaderRenewDeadline" }, description = "The renew deadline of the leader election in seconds.", required = false)
    private int leaderRenewDeadline = 5;

    @Option(names = {
            "--leaderRetryPeriod" }, description = "The retry period for the leader election in seconds.", required = false)
    private int leaderRetryPeriod = 2;

    @Option(names = {
            "--maxWatchIdleTime" }, description = "When a kubernetes watcher is idle for more than this time (in milliseconds) we assume that there is a problem and restart.", required = false)
    private long maxWatchIdleTime = 1000 * 60 * 60; // 1 Hour

    @Option(names = {
            "--continueOnException" }, description = "Whether the operator will continue to run in case of unexpected exceptions.", required = false)
    private boolean continueOnException;

    @Option(names = {
            "--oAuth2ProxyVersion" }, description = "The version to use of the quay.io/oauth2-proxy/oauth2-proxy image.", required = false, defaultValue = "latest")
    private String oAuth2ProxyVersion;

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

    /**
     * @return the configured service auth token
     */
    public String getServiceAuthToken() {
        return getServiceAuthTokenWithFallback();
    }
    
    /**
     * @deprecated Use {@link #getServiceAuthToken()} instead. This method is maintained for backwards compatibility.
     * @return the configured service auth token
     */
    @Deprecated(since = "1.2.0", forRemoval = true)
    public String getAppId() {
        return getServiceAuthTokenWithFallback();
    }

    public String getInstancesHost() {
        return instancesHost;
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

    public String getKeycloakURL() {
        return keycloakURL;
    }

    public String getKeycloakRealm() {
        return keycloakRealm;
    }

    public String getKeycloakClientId() {
        return keycloakClientId;
    }

    public int getLeaderLeaseDuration() {
        return leaderLeaseDuration;
    }

    public int getLeaderRenewDeadline() {
        return leaderRenewDeadline;
    }

    public int getLeaderRetryPeriod() {
        return leaderRetryPeriod;
    }

    public long getMaxWatchIdleTime() {
        return maxWatchIdleTime;
    }

    public boolean isContinueOnException() {
        return continueOnException;
    }

    public String getOAuth2ProxyVersion() {
        return oAuth2ProxyVersion;
    }
    
    /**
     * Get the service auth token with fallback to deprecated app id argument.
     * Logs a deprecation warning if the old argument is used.
     */
    private String getServiceAuthTokenWithFallback() {
        if (serviceAuthToken != null) {
            return serviceAuthToken;
        }
        
        if (appId != null) {
            Logger logger = Logger.getLogger(TheiaCloudOperatorArguments.class.getName());
            logger.warning("Using deprecated command line argument '--appId'. " +
                          "Please migrate to '--serviceAuthToken' in your configuration.");
            return appId;
        }
        
        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((serviceAuthToken == null) ? 0 : serviceAuthToken.hashCode());
        result = prime * result + ((appId == null) ? 0 : appId.hashCode());
        result = prime * result + ((bandwidthLimiter == null) ? 0 : bandwidthLimiter.hashCode());
        result = prime * result + ((cloudProvider == null) ? 0 : cloudProvider.hashCode());
        result = prime * result + (continueOnException ? 1231 : 1237);
        result = prime * result + (eagerStart ? 1231 : 1237);
        result = prime * result + (enableActivityTracker ? 1231 : 1237);
        result = prime * result + (enableMonitor ? 1231 : 1237);
        result = prime * result + ((instancesHost == null) ? 0 : instancesHost.hashCode());
        result = prime * result + ((instancesPath == null) ? 0 : instancesPath.hashCode());
        result = prime * result + ((keycloakClientId == null) ? 0 : keycloakClientId.hashCode());
        result = prime * result + ((keycloakRealm == null) ? 0 : keycloakRealm.hashCode());
        result = prime * result + ((keycloakURL == null) ? 0 : keycloakURL.hashCode());
        result = prime * result + leaderLeaseDuration;
        result = prime * result + leaderRenewDeadline;
        result = prime * result + leaderRetryPeriod;
        result = prime * result + (int) (maxWatchIdleTime ^ (maxWatchIdleTime >>> 32));
        result = prime * result + ((monitorInterval == null) ? 0 : monitorInterval.hashCode());
        result = prime * result + ((requestedStorage == null) ? 0 : requestedStorage.hashCode());
        result = prime * result + ((serviceUrl == null) ? 0 : serviceUrl.hashCode());
        result = prime * result + ((sessionsPerUser == null) ? 0 : sessionsPerUser.hashCode());
        result = prime * result + ((storageClassName == null) ? 0 : storageClassName.hashCode());
        result = prime * result + (useKeycloak ? 1231 : 1237);
        result = prime * result + (usePaths ? 1231 : 1237);
        result = prime * result + ((wondershaperImage == null) ? 0 : wondershaperImage.hashCode());
        result = prime * result + ((oAuth2ProxyVersion == null) ? 0 : oAuth2ProxyVersion.hashCode());
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
        TheiaCloudOperatorArguments other = (TheiaCloudOperatorArguments) obj;
        if (serviceAuthToken == null) {
            if (other.serviceAuthToken != null)
                return false;
        } else if (!serviceAuthToken.equals(other.serviceAuthToken))
            return false;
        if (appId == null) {
            if (other.appId != null)
                return false;
        } else if (!appId.equals(other.appId))
            return false;
        if (bandwidthLimiter != other.bandwidthLimiter)
            return false;
        if (cloudProvider != other.cloudProvider)
            return false;
        if (continueOnException != other.continueOnException)
            return false;
        if (eagerStart != other.eagerStart)
            return false;
        if (enableActivityTracker != other.enableActivityTracker)
            return false;
        if (enableMonitor != other.enableMonitor)
            return false;
        if (instancesHost == null) {
            if (other.instancesHost != null)
                return false;
        } else if (!instancesHost.equals(other.instancesHost))
            return false;
        if (instancesPath == null) {
            if (other.instancesPath != null)
                return false;
        } else if (!instancesPath.equals(other.instancesPath))
            return false;
        if (keycloakClientId == null) {
            if (other.keycloakClientId != null)
                return false;
        } else if (!keycloakClientId.equals(other.keycloakClientId))
            return false;
        if (keycloakRealm == null) {
            if (other.keycloakRealm != null)
                return false;
        } else if (!keycloakRealm.equals(other.keycloakRealm))
            return false;
        if (keycloakURL == null) {
            if (other.keycloakURL != null)
                return false;
        } else if (!keycloakURL.equals(other.keycloakURL))
            return false;
        if (leaderLeaseDuration != other.leaderLeaseDuration)
            return false;
        if (leaderRenewDeadline != other.leaderRenewDeadline)
            return false;
        if (leaderRetryPeriod != other.leaderRetryPeriod)
            return false;
        if (maxWatchIdleTime != other.maxWatchIdleTime)
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
        if (oAuth2ProxyVersion == null) {
            if (other.oAuth2ProxyVersion != null)
                return false;
        } else if (!oAuth2ProxyVersion.equals(other.oAuth2ProxyVersion))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "TheiaCloudArguments [useKeycloak=" + useKeycloak + ", eagerStart=" + eagerStart + ", enableMonitor="
                + enableMonitor + ", enableActivityTracker=" + enableActivityTracker + ", monitorInterval="
                + monitorInterval + ", cloudProvider=" + cloudProvider + ", bandwidthLimiter=" + bandwidthLimiter
                + ", wondershaperImage=" + wondershaperImage + ", serviceUrl=" + serviceUrl + ", sessionsPerUser="
                + sessionsPerUser + ", serviceAuthToken=" + serviceAuthToken + ", appId=" + appId + ", instancesHost=" + instancesHost + ", usePaths=" + usePaths
                + ", instancesPath=" + instancesPath + ", storageClassName=" + storageClassName + ", requestedStorage="
                + requestedStorage + ", keycloakURL=" + keycloakURL + ", keycloakRealm=" + keycloakRealm
                + ", keycloakClientId=" + keycloakClientId + ", leaderLeaseDuration=" + leaderLeaseDuration
                + ", leaderRenewDeadline=" + leaderRenewDeadline + ", leaderRetryPeriod=" + leaderRetryPeriod
                + ", maxWatchIdleTime=" + maxWatchIdleTime + ", continueOnException=" + continueOnException
                + ", oAuth2ProxyVersion=" + oAuth2ProxyVersion + "]";
    }

}
