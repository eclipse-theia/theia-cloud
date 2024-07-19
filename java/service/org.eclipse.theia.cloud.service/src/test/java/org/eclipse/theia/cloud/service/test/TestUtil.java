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
package org.eclipse.theia.cloud.service.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import org.eclipse.theia.cloud.service.NoAnonymousAccess;

import io.quarkus.security.Authenticated;

/**
 * Utility methods for testing service functionality.
 */
public final class TestUtil {

    private TestUtil() {
    }

    /**
     * Verifies that the given method does not allow any anonymous access by checking:
     * <ul>
     * <li>That either the class or the method require authentication and do not permit all access
     * <li>The method and/or its class declare the {@link NoAnonymousAccess} annotation
     * </ul>
     * 
     * @param method The method to verify
     */
    public static void assertNoAnonymousAccessAnnotations(Method method) {
        Class<?> clazz = method.getDeclaringClass();

        boolean classHasAccessControl = ((clazz.getDeclaredAnnotation(Authenticated.class) != null
                || clazz.getDeclaredAnnotation(RolesAllowed.class) != null))
                && clazz.getDeclaredAnnotation(PermitAll.class) == null;
        boolean methodHasAccessControl = (method.getDeclaredAnnotation(Authenticated.class) != null
                || method.getDeclaredAnnotation(RolesAllowed.class) != null);
        boolean methodNoPermitAll = method.getDeclaredAnnotation(PermitAll.class) == null;
        assertTrue((classHasAccessControl || methodHasAccessControl) && methodNoPermitAll,
                "The method and/or its class have declared restricting access control annotations.");

        boolean hasNoAnonymousAccessAnnotation = method.getDeclaredAnnotation(NoAnonymousAccess.class) != null
                || clazz.getDeclaredAnnotation(NoAnonymousAccess.class) != null;
        assertTrue(hasNoAnonymousAccessAnnotation,
                "The method and/or its class declare the annotation to forbid any anonymous access.");
    }
}
