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
package org.eclipse.theia.cloud.operator.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TheiaCloudDeploymentUtil}.
 */
class TheiaCloudDeploymentUtilTests {

    @Test
    void extractHost_httpsUrl() {
        assertEquals("host/path", TheiaCloudDeploymentUtil.extractHost("https://host/path/"));
    }

    @Test
    void extractHost_httpUrl() {
        assertEquals("host/path", TheiaCloudDeploymentUtil.extractHost("http://host/path/"));
    }

    @Test
    void extractHost_httpsSubdomain() {
        assertEquals("uid.ws.apps-crc.testing", TheiaCloudDeploymentUtil.extractHost("https://uid.ws.apps-crc.testing/"));
    }

    @Test
    void extractHost_httpSubdomain() {
        assertEquals("uid.ws.apps-crc.testing", TheiaCloudDeploymentUtil.extractHost("http://uid.ws.apps-crc.testing/"));
    }

    @Test
    void extractHost_noTrailingSlash() {
        assertEquals("host/path", TheiaCloudDeploymentUtil.extractHost("https://host/path"));
    }

    @Test
    void extractHost_instanceBasedUrl() {
        assertEquals("my-app-0.ws.apps-crc.testing",
                TheiaCloudDeploymentUtil.extractHost("http://my-app-0.ws.apps-crc.testing/"));
    }
}
