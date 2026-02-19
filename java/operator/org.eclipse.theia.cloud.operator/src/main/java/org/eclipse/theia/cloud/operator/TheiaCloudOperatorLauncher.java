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

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.operator.di.AbstractTheiaCloudOperatorModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

import picocli.CommandLine;
import picocli.CommandLine.ParameterException;

public abstract class TheiaCloudOperatorLauncher {

    static final String COR_ID_INIT = "init";

    private static final Logger LOGGER = LogManager.getLogger(TheiaCloudOperatorLauncher.class);

    protected TheiaCloudOperatorArguments args;

    public void runMain(String[] args) throws InterruptedException {
        try {
            this.args = createArguments(args);
            AbstractTheiaCloudOperatorModule module = createModule(this.args);
            LOGGER.info(formatLogMessage(COR_ID_INIT, "Using " + module.getClass().getName() + " as DI module"));

            Injector injector = Guice.createInjector(module);
            TheiaCloudOperator theiaCloud = injector.getInstance(TheiaCloudOperator.class);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    theiaCloud.stop();
                } catch (Exception e) {
                    LOGGER.error(formatLogMessage(COR_ID_INIT, "Error during operator shutdown"), e);
                }
            }, "theia-cloud-operator-shutdown"));
            LOGGER.info(formatLogMessage(COR_ID_INIT, "Launching Theia Cloud Now"));
            theiaCloud.start();
        } catch (Exception e) {
            LOGGER.error(formatLogMessage(COR_ID_INIT, "Error during operator startup"), e);
            throw e;
        }
    }

    public TheiaCloudOperatorArguments createArguments(String[] args) {
        try {
            TheiaCloudOperatorArguments arguments = new TheiaCloudOperatorArguments();
            CommandLine commandLine = new CommandLine(arguments).setTrimQuotes(true);
            commandLine.parseArgs(args);
            LOGGER.info(formatLogMessage(COR_ID_INIT, "Parsed args: " + arguments));
            return arguments;
        } catch (ParameterException e) {
            LOGGER.error(formatLogMessage(COR_ID_INIT, "Failed to parse command line arguments"), e);
            throw e;
        } catch (Exception e) {
            LOGGER.error(formatLogMessage(COR_ID_INIT, "Unexpected error parsing arguments"), e);
            throw e;
        }
    }

    abstract AbstractTheiaCloudOperatorModule createModule(TheiaCloudOperatorArguments arguments);

}
