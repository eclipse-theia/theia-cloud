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

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.monitor.di.AbstractTheiaCloudOperatorModule;
import org.eclipse.theia.cloud.monitor.di.DefaultTheiaCloudMonitorOperatorModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

import picocli.CommandLine;

public class Main {

    private static final Logger LOGGER = LogManager.getLogger(Main.class);

    static final String COR_ID_INIT = "init";

    public static void main(String[] args) throws InterruptedException {
	new Main().runMain(args);
    }

    public void runMain(String[] args) throws InterruptedException {
	TheiaCloudMonitorArguments arguments = createArguments(args);
	AbstractTheiaCloudOperatorModule module = createModule(arguments);
	LOGGER.info(formatLogMessage(COR_ID_INIT, "Using " + module.getClass().getName() + " as DI module"));

	Injector injector = Guice.createInjector(module);
	TheiaCloudMonitor theiaCloudMonitor = injector.getInstance(TheiaCloudMonitor.class);

	LOGGER.info(formatLogMessage(COR_ID_INIT, "Launching Theia Cloud Monitor Now"));
	theiaCloudMonitor.start(arguments.getPort(), arguments.getPingInterval(), arguments.getPodTimeout(),
		arguments.getNotifyTimeout());
    }

    protected TheiaCloudMonitorArguments createArguments(String[] args) {
	TheiaCloudMonitorArguments arguments = new TheiaCloudMonitorArguments();
	CommandLine commandLine = new CommandLine(arguments).setTrimQuotes(true);
	commandLine.parseArgs(args);

	LOGGER.info(formatLogMessage(COR_ID_INIT, "Parsing args: port " + arguments.getPort()));
	LOGGER.info(formatLogMessage(COR_ID_INIT, "Parsing args: pingInterval " + arguments.getPingInterval()));
	LOGGER.info(formatLogMessage(COR_ID_INIT, "Parsing args: podTimeout " + arguments.getPodTimeout()));
	LOGGER.info(formatLogMessage(COR_ID_INIT, "Parsing args: notifyTimeout " + arguments.getNotifyTimeout()));
	return arguments;
    }

    protected AbstractTheiaCloudOperatorModule createModule(TheiaCloudMonitorArguments arguments) {
	return new DefaultTheiaCloudMonitorOperatorModule(arguments);
    }

}
