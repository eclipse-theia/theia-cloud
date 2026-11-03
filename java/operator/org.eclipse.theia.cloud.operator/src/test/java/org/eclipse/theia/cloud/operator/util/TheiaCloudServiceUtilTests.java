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
package org.eclipse.theia.cloud.operator.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;

class TheiaCloudServiceUtilTests {
    private Service service() {
        return new ServiceBuilder().withNewMetadata().withName("app-int").withNamespace("sessions").endMetadata()
                .withNewSpec().addToSelector("app", "instance-1")
                .addNewPort().withName("monitor").withPort(9000).endPort()
                .addNewPort().withName("http-internal").withPort(3001).withNewTargetPort(8080).endPort()
                .endSpec().build();
    }

    @Test
    void usesNamedServicePortAndNamespace() {
        assertEquals("http://app-int.sessions.svc:3001/", TheiaCloudServiceUtil.getInternalServiceURL(service()).toString());
    }

    @Test
    void supportsCustomServicePort() {
        Service service = service();
        service.getSpec().getPorts().get(1).setPort(4567);
        assertEquals("http://app-int.sessions.svc:4567/", TheiaCloudServiceUtil.getInternalServiceURL(service).toString());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 65536})
    void rejectsInvalidPort(int port) {
        Service service = service();
        service.getSpec().getPorts().get(1).setPort(port);
        assertThrows(IllegalArgumentException.class, () -> TheiaCloudServiceUtil.getInternalServiceURL(service));
    }

    @Test
    void rejectsMissingServiceInformation() {
        assertThrows(IllegalArgumentException.class, () -> TheiaCloudServiceUtil.getInternalServiceURL(null));
        Service service = service();
        service.getMetadata().setNamespace(null);
        assertThrows(IllegalArgumentException.class, () -> TheiaCloudServiceUtil.getInternalServiceURL(service));
        service.getMetadata().setNamespace("sessions");
        service.getSpec().getPorts().remove(1);
        assertThrows(IllegalArgumentException.class, () -> TheiaCloudServiceUtil.getInternalServiceURL(service));
    }

    @Test
    void rejectsDifferentOrEmptySelectorsAndDifferentNamespaces() {
        Service external = service();
        Service internal = service();
        assertTrue(TheiaCloudServiceUtil.selectsSameApplication(external, internal));
        internal.getSpec().setSelector(Map.of("app", "instance-2"));
        assertFalse(TheiaCloudServiceUtil.selectsSameApplication(external, internal));
        external.getSpec().setSelector(Map.of());
        internal.getSpec().setSelector(Map.of());
        assertFalse(TheiaCloudServiceUtil.selectsSameApplication(external, internal));
        internal = service();
        internal.getMetadata().setNamespace("another-namespace");
        assertFalse(TheiaCloudServiceUtil.selectsSameApplication(service(), internal));
    }
}
