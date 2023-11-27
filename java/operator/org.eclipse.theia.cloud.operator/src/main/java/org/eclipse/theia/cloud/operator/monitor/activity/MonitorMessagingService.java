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
package org.eclipse.theia.cloud.operator.monitor.activity;

import org.eclipse.theia.cloud.common.k8s.resource.Session;

public interface MonitorMessagingService {

    /**
     * Renders a message to the client.
     * 
     * @param session to which the timeout should be sent
     * @param level   of the message ("warn", "error" or "info")
     * @param message to be displayed
     */
    void sendMessage(Session session, String level, String message);

    /**
     * Renders a fullscreen message to the client.
     * 
     * @param session to which the timeout should be sent
     * @param level   of the message ("warn", "error" or "info")
     * @param message to be displayed
     * @param detail  additional information displayed on the message
     */
    void sendFullscreenMessage(Session session, String level, String message, String detail);

    /**
     * Renders a timeout message to the client.
     * 
     * @param session to which the timeout should be sent
     * @param detail  additional information on the timeout
     */
    void sendTimeoutMessage(Session session, String detail);

}
