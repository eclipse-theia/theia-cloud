/********************************************************************************
 * Copyright (C) 2026 Gaurav Chaudhary and others.
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
package org.eclipse.theia.cloud.common.k8s.resource.session;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.theia.cloud.common.k8s.resource.session.hub.SessionHub;
import org.eclipse.theia.cloud.common.k8s.resource.session.v1beta6.SessionV1beta6;
import org.eclipse.theia.cloud.common.k8s.resource.session.v1beta7.SessionV1beta7;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("deprecation")
class SessionLabelTests {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "Exercise 42 / Étape ★" })
    void preservesLabelAcrossServedVersions(String label) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Session session = new Session();
        SessionSpec spec = new SessionSpec("session", "app", "user");
        spec.setLabel(label);
        session.setSpec(spec);

        SessionV1beta7 beta7 = new SessionV1beta7(new SessionHub(session));
        SessionV1beta6 beta6 = new SessionV1beta6(new SessionHub(mapper.readValue(
                mapper.writeValueAsBytes(beta7), SessionV1beta7.class)));
        Session restored = new Session(new SessionHub(mapper.readValue(
                mapper.writeValueAsBytes(beta6), SessionV1beta6.class)));

        assertEquals(label, restored.getSpec().getLabel());
        assertEquals("session", restored.getSpec().getName());
    }
}
