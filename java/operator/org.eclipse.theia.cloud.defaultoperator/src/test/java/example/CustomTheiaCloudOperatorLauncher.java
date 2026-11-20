/********************************************************************************
 * Copyright (C) 2026 EclipseSource and others.
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
package example;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.DefaultTheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.defaultoperator.DefaultTheiaCloudOperatorLauncher;
import org.eclipse.theia.cloud.defaultoperator.DefaultTheiaCloudOperatorModule;
import org.eclipse.theia.cloud.operator.TheiaCloudOperatorArguments;
import org.eclipse.theia.cloud.operator.di.AbstractTheiaCloudOperatorModule;

import io.fabric8.kubernetes.client.NamespacedKubernetesClient;

public class CustomTheiaCloudOperatorLauncher extends DefaultTheiaCloudOperatorLauncher {

    public static void main(String[] args) throws InterruptedException {
        new CustomTheiaCloudOperatorLauncher().runMain(args);
    }

    @Override
    public AbstractTheiaCloudOperatorModule createModule(TheiaCloudOperatorArguments arguments) {
        return new CustomTheiaCloudOperatorModule(arguments);
    }

    public static class CustomTheiaCloudOperatorModule extends DefaultTheiaCloudOperatorModule {

        public CustomTheiaCloudOperatorModule(TheiaCloudOperatorArguments arguments) {
            super(arguments);
        }

        @Override
        protected TheiaCloudClient createTheiaCloudClient(NamespacedKubernetesClient client) {
            return new LoggingTheiaCloudClient(client);
        }
    }

    public static class LoggingTheiaCloudClient extends DefaultTheiaCloudClient {

        private static final Logger LOGGER = LogManager.getLogger(LoggingTheiaCloudClient.class);

        public LoggingTheiaCloudClient(NamespacedKubernetesClient client) {
            super(client);
        }

        @Override
        public Optional<String> getClusterIPFromSessionName(String sessionName) {
            Optional<String> ip = super.getClusterIPFromSessionName(sessionName);
            if (ip.isEmpty()) {
                LOGGER.warn("No ClusterIP found for session {}", sessionName);
            }
            return ip;
        }
    }
}
