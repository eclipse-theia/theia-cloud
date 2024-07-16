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
package org.eclipse.theia.cloud.defaultoperator;

import org.eclipse.theia.cloud.operator.LeaderElectionTheiaCloudOperatorLauncher;
import org.eclipse.theia.cloud.operator.TheiaCloudOperatorArguments;
import org.eclipse.theia.cloud.operator.di.AbstractTheiaCloudOperatorModule;

public class DefaultTheiaCloudOperatorLauncher extends LeaderElectionTheiaCloudOperatorLauncher {

    public static void main(String[] args) throws InterruptedException {
	new DefaultTheiaCloudOperatorLauncher().runMain(args);
    }

    @Override
    public AbstractTheiaCloudOperatorModule createModule(TheiaCloudOperatorArguments arguments) {
	return new DefaultTheiaCloudOperatorModule(arguments);
    }

}