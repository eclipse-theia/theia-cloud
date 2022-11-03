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
package org.eclipse.theia.cloud.monitor;

import picocli.CommandLine.Option;

public class TheiaCloudMonitorArguments {

    @Option(names = {
	    "--port" }, description = "Port at which the REST API of the extension is provided", required = true)
    private Integer port;

    @Option(names = {
	    "--pingInterval" }, description = "Interval in minutes for how often the extension should be pinged", required = false, defaultValue = "1")
    private Integer pingInterval;

    @Option(names = {
	    "--podTimeout" }, description = "Number of minutes until inactivity leads to pod shutdown", required = false, defaultValue = "30")
    private Integer podTimeout;

    @Option(names = {
	    "--notifyTimeout" }, description = "Number of minutes until inactivity leads to a popup to the user. Make higher than --podTimeout to disable", required = false, defaultValue = "25")
    private Integer notifyTimeout;

    public Integer getPort() {
	return port;
    }

    public Integer getPingInterval() {
	return pingInterval;
    }

    public Integer getPodTimeout() {
	return podTimeout;
    }

    public Integer getNotifyTimeout() {
	return notifyTimeout;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((port == null) ? 0 : port.hashCode());
	result = prime * result + ((pingInterval == null) ? 0 : pingInterval.hashCode());
	result = prime * result + ((podTimeout == null) ? 0 : podTimeout.hashCode());
	result = prime * result + ((notifyTimeout == null) ? 0 : notifyTimeout.hashCode());
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
	TheiaCloudMonitorArguments other = (TheiaCloudMonitorArguments) obj;
	if (port == null) {
	    if (other.port != null)
		return false;
	} else if (!port.equals(other.port))
	    return false;
	if (pingInterval == null) {
	    if (other.pingInterval != null)
		return false;
	} else if (!pingInterval.equals(other.pingInterval))
	    return false;
	if (podTimeout == null) {
	    if (other.podTimeout != null)
		return false;
	} else if (!podTimeout.equals(other.podTimeout))
	    return false;
	if (notifyTimeout == null) {
	    if (other.notifyTimeout != null)
		return false;
	} else if (!notifyTimeout.equals(other.notifyTimeout))
	    return false;
	return true;
    }

    @Override
    public String toString() {
	return "TheiaCloudMonitorArguments [port=" + port + ", pingInterval=" + pingInterval + ", podTimeout="
		+ podTimeout + ", notifyTimeout=" + notifyTimeout + "]";
    }

}
