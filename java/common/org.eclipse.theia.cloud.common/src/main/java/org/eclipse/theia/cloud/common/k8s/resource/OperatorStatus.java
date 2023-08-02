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
package org.eclipse.theia.cloud.common.k8s.resource;

/**
 * Constant values to describe resource handling.
 */
public interface OperatorStatus {

    /**
     * The default status describing that the resource is new and was not handled
     * before.
     */
    String NEW = "NEW";

    /** The operator tried to handle this resource but an error occurred. */
    String ERROR = "ERROR";

    /** The operator started handling this resource. */
    String HANDLING = "HANDLING";

    /** The operator successfully finished handling this resource. */
    String HANDLED = "HANDLED";
}
