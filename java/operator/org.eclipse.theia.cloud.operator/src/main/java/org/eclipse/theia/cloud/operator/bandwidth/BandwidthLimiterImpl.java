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
package org.eclipse.theia.cloud.operator.bandwidth;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.theia.cloud.operator.OperatorArguments;

import com.google.inject.Inject;

import io.fabric8.kubernetes.api.model.Capabilities;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.SecurityContext;
import io.fabric8.kubernetes.api.model.apps.Deployment;

public class BandwidthLimiterImpl implements BandwidthLimiter {

    private static final String K = "k";
    private static final String KUBERNETES_IO_EGRESS_BANDWIDTH = "kubernetes.io/egress-bandwidth";
    private static final String KUBERNETES_IO_INGRESS_BANDWIDTH = "kubernetes.io/ingress-bandwidth";

    private OperatorArguments arguments;

    @Inject
    public BandwidthLimiterImpl(OperatorArguments arguments) {
	this.arguments = arguments;
    }

    @Override
    public void limit(Deployment deployment, int downlinkLimit, int uplinkLimit, String correlationId) {
	org.eclipse.theia.cloud.operator.OperatorArguments.BandwidthLimiter limiter = arguments.getBandwidthLimiter();
	if (limiter == null) {
	    return;
	}
	switch (limiter) {
	case K8SANNOTATION:
	    addAnnotations(deployment, downlinkLimit, uplinkLimit);
	    break;
	case K8SANNOTATIONANDWONDERSHAPER:
	    addAnnotations(deployment, downlinkLimit, uplinkLimit);
	    addWondershaperInitContainer(deployment, downlinkLimit, uplinkLimit);
	    break;
	case WONDERSHAPER:
	    addWondershaperInitContainer(deployment, downlinkLimit, uplinkLimit);
	    break;
	default:
	    break;
	}
    }

    private void addAnnotations(Deployment deployment, int downlinkLimit, int uplinkLimit) {
	Map<String, String> annotations = deployment.getSpec().getTemplate().getMetadata().getAnnotations();
	if (annotations == null) {
	    annotations = new LinkedHashMap<>();
	    deployment.getSpec().getTemplate().getMetadata().setAnnotations(annotations);
	}
	if (downlinkLimit > 0) {
	    annotations.put(KUBERNETES_IO_INGRESS_BANDWIDTH, downlinkLimit + K);
	}
	if (uplinkLimit > 0) {
	    annotations.put(KUBERNETES_IO_EGRESS_BANDWIDTH, uplinkLimit + K);
	}
    }

    private void addWondershaperInitContainer(Deployment deployment, int downlinkLimit, int uplinkLimit) {
	if (downlinkLimit > 0 && uplinkLimit > 0) {
	    List<Container> initContainers = deployment.getSpec().getTemplate().getSpec().getInitContainers();

	    Container wondershaperInitContainer = new Container();
	    initContainers.add(wondershaperInitContainer);

	    wondershaperInitContainer.setName("wondershaper-init");
	    wondershaperInitContainer.setImage(arguments.getWondershaperImage());

	    SecurityContext securityContext = new SecurityContext();
	    wondershaperInitContainer.setSecurityContext(securityContext);

	    Capabilities capabilities = new Capabilities();
	    securityContext.setCapabilities(capabilities);

	    List<String> add = new ArrayList<>();
	    capabilities.setAdd(add);
	    capabilities.getAdd().add("NET_ADMIN");

	    List<EnvVar> env = new ArrayList<>();
	    wondershaperInitContainer.setEnv(env);
	    EnvVar down = new EnvVar();
	    env.add(down);
	    EnvVar up = new EnvVar();
	    env.add(up);

	    down.setName("DOWNLINK");
	    down.setValue(String.valueOf(downlinkLimit));

	    up.setName("UPLINK");
	    up.setValue(String.valueOf(uplinkLimit));
	}
    }

}
