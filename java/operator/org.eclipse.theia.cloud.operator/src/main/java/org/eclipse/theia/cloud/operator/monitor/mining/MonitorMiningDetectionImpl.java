/********************************************************************************
 * Copyright (C) 2022 EclipseSource and others.
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
package org.eclipse.theia.cloud.operator.monitor.mining;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.util.LogMessageUtil;

import com.google.inject.Inject;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.ContainerMetrics;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics;

public class MonitorMiningDetectionImpl implements MonitorMiningDetection {

    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    private static final Logger LOGGER = LogManager.getLogger(MonitorMiningDetectionImpl.class);

    private static final String CORRELATION_ID = "mining-detection";

    @Inject
    private TheiaCloudClient resourceClient;

    @Override
    public void start(int interval) {
	LOGGER.info(LogMessageUtil.formatLogMessage(CORRELATION_ID,
		"Launching Mining Detection with interval of " + interval + " minutes"));
	EXECUTOR.scheduleWithFixedDelay(this::collectCPUUsage, 0, interval, TimeUnit.MINUTES);

    }

    protected void collectCPUUsage() {
	List<Pod> pods = resourceClient.kubernetes().pods().list().getItems();
	for (Pod pod : pods) {
	    Optional<Container> container = getTheiaCloudIDEContainer(pod);
	    if (container.isPresent()) {
		PodMetrics podMetrics = resourceClient.kubernetes().top().pods().metrics(resourceClient.namespace(),
			pod.getMetadata().getName());
		Optional<ContainerMetrics> containerMetrics = podMetrics.getContainers().stream()
			.filter(con -> con.getName().equals(container.get().getName())).findFirst();
		if (containerMetrics.isEmpty()) {
		    LOGGER.trace(LogMessageUtil.formatLogMessage(CORRELATION_ID,
			    MessageFormat.format("Pod {0} is not an IDE pod.", pod.getMetadata().getName())));
		    continue;
		}
		String cpuAmount = containerMetrics.get().getUsage().get("cpu").getAmount();
		String cpuFormat = containerMetrics.get().getUsage().get("cpu").getFormat();
		LOGGER.info(LogMessageUtil.formatLogMessage(CORRELATION_ID, MessageFormat
			.format("Pod {0} : CPU Usage {1} {2}", pod.getMetadata().getName(), cpuAmount, cpuFormat)));

	    }
	}
    }

    private Optional<Container> getTheiaCloudIDEContainer(Pod pod) {
	return pod.getSpec().getContainers().stream().filter(container -> {
	    return container.getEnv().stream().anyMatch(env -> "THEIACLOUD_SESSION_NAME".equals(env.getName()));
	}).findAny();
    }

}
