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
package org.eclipse.theia.cloud.operator;

import picocli.CommandLine.Option;

public class TheiaCloudArguments {

    public enum CloudProvider {
	GKE
    }

    public enum BandwidthLimiter {
	K8SANNOTATION, WONDERSHAPER, K8SANNOTATIONANDWONDERSHAPER
    }

    @Option(names = { "--keycloak" }, description = "Whether to use keycloak", required = false)
    private boolean useKeycloak;

    @Option(names = { "--eagerStart" }, description = "Whether workspaces shall be started early.", required = false)
    private boolean eagerStart;

    @Option(names = {
	    "--ephemeralStorage" }, description = "Whether workspaces will get persisted storage assigned.", required = false)
    private boolean ephemeralStorage;

    @Option(names = {
	    "--cloudProvider" }, description = "The cloud provider where Theia.Cloud is deployed", required = false)
    private CloudProvider cloudProvider;

    @Option(names = {
	    "--bandwidthLimiter" }, description = "The method of limiting network bandwidth", required = false)
    private BandwidthLimiter bandwidthLimiter;

    public boolean isUseKeycloak() {
	return useKeycloak;
    }

    public boolean isEagerStart() {
	return eagerStart;
    }

    public boolean isEphemeralStorage() {
	return ephemeralStorage;
    }

    public CloudProvider getCloudProvider() {
	return cloudProvider;
    }

    public BandwidthLimiter getBandwidthLimiter() {
	return bandwidthLimiter;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((bandwidthLimiter == null) ? 0 : bandwidthLimiter.hashCode());
	result = prime * result + ((cloudProvider == null) ? 0 : cloudProvider.hashCode());
	result = prime * result + (eagerStart ? 1231 : 1237);
	result = prime * result + (ephemeralStorage ? 1231 : 1237);
	result = prime * result + (useKeycloak ? 1231 : 1237);
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
	if (bandwidthLimiter != other.bandwidthLimiter)
	    return false;
	if (cloudProvider != other.cloudProvider)
	    return false;
	if (eagerStart != other.eagerStart)
	    return false;
	if (ephemeralStorage != other.ephemeralStorage)
	    return false;
	if (useKeycloak != other.useKeycloak)
	    return false;
	return true;
    }

    @Override
    public String toString() {
	return "TheiaCloudArguments [useKeycloak=" + useKeycloak + ", eagerStart=" + eagerStart + ", ephemeralStorage="
		+ ephemeralStorage + ", cloudProvider=" + cloudProvider + ", bandwidthLimiter=" + bandwidthLimiter
		+ "]";
    }

}
