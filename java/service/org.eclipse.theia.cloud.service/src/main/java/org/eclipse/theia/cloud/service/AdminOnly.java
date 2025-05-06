/********************************************************************************
 * Copyright (C) 2025 EclipseSource and others.
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
package org.eclipse.theia.cloud.service;

import jakarta.ws.rs.NameBinding;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;

/**
 * <p>
 * Annotation to mark a resource or method as only accessible to admin users.
 * </p>
 * <p>
 * Can be applied to a method or a resource class. In the latter case, the behavior applies to all its methods.
 * </p>
 * 
 * @see AdminOnlyFilter
 * @see TheiaCloudUserProducer
 */
@Inherited
@NameBinding
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
public @interface AdminOnly {
}
