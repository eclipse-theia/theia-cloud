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
package org.eclipse.theia.cloud.operator;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import java.time.Duration;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.operator.di.AbstractTheiaCloudOperatorModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.extended.leaderelection.LeaderCallbacks;
import io.fabric8.kubernetes.client.extended.leaderelection.LeaderElectionConfig;
import io.fabric8.kubernetes.client.extended.leaderelection.LeaderElectionConfigBuilder;
import io.fabric8.kubernetes.client.extended.leaderelection.LeaderElector;
import io.fabric8.kubernetes.client.extended.leaderelection.resourcelock.LeaseLock;

public abstract class LeaderElectionTheiaCloudOperatorLauncher extends TheiaCloudOperatorLauncher {

    private static final Logger LOGGER = LogManager.getLogger(LeaderElectionTheiaCloudOperatorLauncher.class);

    protected static final String LEASE_LOCK_NAME = "theia-cloud-operator-leaders";

    @Override
    public void runMain(String[] args) throws InterruptedException {
	this.args = createArguments(args);

	LOGGER.info(formatLogMessage(COR_ID_INIT,
		"Launching Theia Cloud Leader Election now"));

    this.runLeaderElection(this.args);

	LOGGER.info(formatLogMessage(COR_ID_INIT, "Theia Cloud Leader Election Loop Ended"));
    }

    protected void runLeaderElection(TheiaCloudOperatorArguments args) {
        final String lockIdentity = UUID.randomUUID().toString();
        LOGGER.info(formatLogMessage(COR_ID_INIT,
                "Own lock identity is " + lockIdentity));

        long leaseDurationInSeconds = this.args.getLeaderLeaseDuration();
        long renewDeadlineInSeconds = this.args.getLeaderRenewDeadline();
        long retryPeriodInSeconds = this.args.getLeaderRetryPeriod();

        Config k8sConfig = new ConfigBuilder().build();

        try (KubernetesClient k8sClient = new KubernetesClientBuilder().withConfig(k8sConfig).build()) {
	    String leaseLockNamespace = k8sClient.getNamespace();

	    LeaderElectionConfig leaderElectionConfig = new LeaderElectionConfigBuilder()//
		    .withReleaseOnCancel(true)//
		    .withName("Theia Cloud Operator Leader Election")//

		    // non leaders will check after this time if they can become leader
		    .withLeaseDuration(Duration.ofSeconds(leaseDurationInSeconds))//

		    // time the current leader tries to refresh the lease before giving up
		    .withRenewDeadline(Duration.ofSeconds(renewDeadlineInSeconds))

		    // time each client should wait before performing the next action
		    .withRetryPeriod(Duration.ofSeconds(retryPeriodInSeconds))//

		    .withLock(new LeaseLock(leaseLockNamespace, LEASE_LOCK_NAME, lockIdentity))//
		    .withLeaderCallbacks(
			    new LeaderCallbacks(LeaderElectionTheiaCloudOperatorLauncher.this::onStartLeading,
				    LeaderElectionTheiaCloudOperatorLauncher.this::onStopLeading,
				    LeaderElectionTheiaCloudOperatorLauncher.this::onNewLeader))//
		    .build();
	    LeaderElector leaderElector = k8sClient.leaderElector().withConfig(leaderElectionConfig).build();
	    leaderElector.run();
	}
    }

    protected void onStartLeading() {
	LOGGER.info(formatLogMessage(COR_ID_INIT, "Elected as new leader!"));
	startOperatorAsLeader(args);
    }

    protected void onStopLeading() {
	LOGGER.info(formatLogMessage(COR_ID_INIT, "Removed as leader!"));
	System.exit(0);
    }

    protected void onNewLeader(String newLeader) {
	LOGGER.info(formatLogMessage(COR_ID_INIT, newLeader + " is the new leader."));
    }

    protected void startOperatorAsLeader(TheiaCloudOperatorArguments arguments) {
	AbstractTheiaCloudOperatorModule module = createModule(arguments);
	LOGGER.info(formatLogMessage(COR_ID_INIT, "Using " + module.getClass().getName() + " as DI module"));

	Injector injector = Guice.createInjector(module);
	TheiaCloudOperator theiaCloud = injector.getInstance(TheiaCloudOperator.class);

	LOGGER.info(formatLogMessage(COR_ID_INIT, "Launching Theia Cloud Now"));
	theiaCloud.start();
    }

    @Override
    public abstract AbstractTheiaCloudOperatorModule createModule(TheiaCloudOperatorArguments arguments);

}
