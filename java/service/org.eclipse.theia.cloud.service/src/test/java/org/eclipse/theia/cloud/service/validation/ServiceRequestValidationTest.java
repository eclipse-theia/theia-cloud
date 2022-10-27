/********************************************************************************
 * Copyright (C) 2022 EclipseSource, STMicroelectronics and others.
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
package org.eclipse.theia.cloud.service.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.theia.cloud.service.LaunchRequest;
import org.junit.jupiter.api.Test;

public class ServiceRequestValidationTest {

    private static final String VALID_APP_ID = "appId";
    private static final String VALID_USER = "sdoe@theia-cloud.io";
    private static final String VALID_APP_DEFINITION = "theia.blueprint";
    private static final String VALID_WORKSPACE_NAME = "sdoe-theia-workspace";
    private static final String VALID_WORKSPACE_LABEL = "Sasha's Theia workspace";
    private static final int VALID_TIMEOUT = 3;

    private static LaunchRequest createLaunchRequest(//
	    String appId, //
	    String kind, //
	    String user, //
	    String appDefinition, //
	    String workspaceName, //
	    String label, //
	    boolean ephemeral, //
	    int timeout//

    ) {
	LaunchRequest request = new LaunchRequest();
	request.appId = appId;
	request.kind = kind;
	request.user = user;
	request.appDefinition = appDefinition;
	request.workspaceName = workspaceName;
	request.label = label;
	request.ephemeral = ephemeral;
	request.timeout = timeout;
	return request;
    }

    @Test
    public void testLaunchRequestNull() {
	/* setup */
	LaunchRequest request = createLaunchRequest(null, null, null, null, null, null, false, 1);

	/* act */
	ValidationResult result = request.validateDataFormat();

	/* assert */
	assertTrue(result.isOK(), result.toString());
    }

    @Test
    public void testLaunchRequestEmpty() {
	/* setup */
	LaunchRequest request = createLaunchRequest("", "", "", "", "", "", false, 1);

	/* act */
	ValidationResult result = request.validateDataFormat();

	/* assert */
	assertTrue(result.isOK(), result.toString());
    }

    @Test
    public void testLaunchRequestValid() {
	/* setup */
	LaunchRequest request = createLaunchRequest(VALID_APP_ID, LaunchRequest.KIND, VALID_USER, VALID_APP_DEFINITION,
		VALID_WORKSPACE_NAME, VALID_WORKSPACE_LABEL, false, VALID_TIMEOUT);

	/* act */
	ValidationResult result = request.validateDataFormat();

	/* assert */
	assertTrue(result.isOK(), result.toString());
    }

    @Test
    public void testLaunchRequestInvalidAppId() {
	/* setup */
	LaunchRequest request = createLaunchRequest("appId\n---", LaunchRequest.KIND, VALID_USER, VALID_APP_DEFINITION,
		VALID_WORKSPACE_NAME, VALID_WORKSPACE_LABEL, false, 3);

	/* act */
	ValidationResult result = request.validateDataFormat();

	/* assert */
	assertFalse(result.isOK(), result.toString());
	assertEquals(1, result.problems.size(), result.toString());
	assertEquals("appId", result.problems.get(0).field, result.toString());
    }

    @Test
    public void testLaunchRequestInvalidKind() {
	/* setup */
	LaunchRequest request = createLaunchRequest(VALID_APP_ID, LaunchRequest.KIND + "\n---", VALID_USER,
		VALID_APP_DEFINITION, VALID_WORKSPACE_NAME, VALID_WORKSPACE_LABEL, false, 3);

	/* act */
	ValidationResult result = request.validateDataFormat();

	/* assert */
	assertFalse(result.isOK(), result.toString());
	assertEquals(1, result.problems.size(), result.toString());
	assertEquals("kind", result.problems.get(0).field, result.toString());
    }

    @Test
    public void testLaunchRequestInvalidUser1() {
	/* setup */
	LaunchRequest request = createLaunchRequest(VALID_APP_ID, LaunchRequest.KIND, "sdoe@theia-cloud",
		VALID_APP_DEFINITION, VALID_WORKSPACE_NAME, VALID_WORKSPACE_LABEL, false, 3);

	/* act */
	ValidationResult result = request.validateDataFormat();

	/* assert */
	assertFalse(result.isOK(), result.toString());
	assertEquals(1, result.problems.size(), result.toString());
	assertEquals("user", result.problems.get(0).field, result.toString());
    }

    @Test
    public void testLaunchRequestInvalidUser2() {
	/* setup */
	LaunchRequest request = createLaunchRequest(VALID_APP_ID, LaunchRequest.KIND, "sdoe@theia-cloud.eclipse",
		VALID_APP_DEFINITION, VALID_WORKSPACE_NAME, VALID_WORKSPACE_LABEL, false, 3);

	/* act */
	ValidationResult result = request.validateDataFormat();

	/* assert */
	assertFalse(result.isOK(), result.toString());
	assertEquals(1, result.problems.size(), result.toString());
	assertEquals("user", result.problems.get(0).field, result.toString());
    }

    @Test
    public void testLaunchRequestInvalidUser3() {
	/* setup */
	LaunchRequest request = createLaunchRequest(VALID_APP_ID, LaunchRequest.KIND, "sdoe@theia+cloud.io",
		VALID_APP_DEFINITION, VALID_WORKSPACE_NAME, VALID_WORKSPACE_LABEL, false, 3);

	/* act */
	ValidationResult result = request.validateDataFormat();

	/* assert */
	assertFalse(result.isOK(), result.toString());
	assertEquals(1, result.problems.size(), result.toString());
	assertEquals("user", result.problems.get(0).field, result.toString());
    }

    @Test
    public void testLaunchRequestInvalidUser4() {
	/* setup */
	LaunchRequest request = createLaunchRequest(VALID_APP_ID, LaunchRequest.KIND, "sdoe", VALID_APP_DEFINITION,
		VALID_WORKSPACE_NAME, VALID_WORKSPACE_LABEL, false, 3);

	/* act */
	ValidationResult result = request.validateDataFormat();

	/* assert */
	assertFalse(result.isOK(), result.toString());
	assertEquals(1, result.problems.size(), result.toString());
	assertEquals("user", result.problems.get(0).field, result.toString());
    }

    @Test
    public void testLaunchRequestInvalidUser5() {
	/* setup */
	LaunchRequest request = createLaunchRequest(VALID_APP_ID, LaunchRequest.KIND, "s\n---doe@theia-cloud.io",
		VALID_APP_DEFINITION, VALID_WORKSPACE_NAME, VALID_WORKSPACE_LABEL, false, 3);

	/* act */
	ValidationResult result = request.validateDataFormat();

	/* assert */
	assertFalse(result.isOK(), result.toString());
	assertEquals(1, result.problems.size(), result.toString());
	assertEquals("user", result.problems.get(0).field, result.toString());
    }

    @Test
    public void testLaunchRequestInvalidUser6() {
	/* setup */
	LaunchRequest request = createLaunchRequest(VALID_APP_ID, LaunchRequest.KIND, "sdoe @ theia-cloud.io",
		VALID_APP_DEFINITION, VALID_WORKSPACE_NAME, VALID_WORKSPACE_LABEL, false, 3);

	/* act */
	ValidationResult result = request.validateDataFormat();

	/* assert */
	assertFalse(result.isOK(), result.toString());
	assertEquals(1, result.problems.size(), result.toString());
	assertEquals("user", result.problems.get(0).field, result.toString());
    }

    @Test
    public void testLaunchRequestValidUser() {
	/* setup */
	LaunchRequest request = createLaunchRequest(VALID_APP_ID, LaunchRequest.KIND, "sdoe+@theia-cloud.io",
		VALID_APP_DEFINITION, VALID_WORKSPACE_NAME, VALID_WORKSPACE_LABEL, false, 3);

	/* act */
	ValidationResult result = request.validateDataFormat();

	/* assert */
	assertTrue(result.isOK(), result.toString());
    }

    @Test
    public void testLaunchRequestInvalidAppDefinition1() {
	/* setup */
	LaunchRequest request = createLaunchRequest(VALID_APP_ID, LaunchRequest.KIND, VALID_USER, "theia blueprint",
		VALID_WORKSPACE_NAME, VALID_WORKSPACE_LABEL, false, 3);

	/* act */
	ValidationResult result = request.validateDataFormat();

	/* assert */
	assertFalse(result.isOK(), result.toString());
	assertEquals(1, result.problems.size(), result.toString());
	assertEquals("appDefinition", result.problems.get(0).field, result.toString());
    }

    @Test
    public void testLaunchRequestInvalidAppDefinition2() {
	/* setup */
	LaunchRequest request = createLaunchRequest(VALID_APP_ID, LaunchRequest.KIND, VALID_USER,
		"theia.blueprint\n---", VALID_WORKSPACE_NAME, VALID_WORKSPACE_LABEL, false, 3);

	/* act */
	ValidationResult result = request.validateDataFormat();

	/* assert */
	assertFalse(result.isOK(), result.toString());
	assertEquals(1, result.problems.size(), result.toString());
	assertEquals("appDefinition", result.problems.get(0).field, result.toString());
    }

    @Test
    public void testLaunchRequestInvalidAppDefinition3() {
	/* setup */
	LaunchRequest request = createLaunchRequest(VALID_APP_ID, LaunchRequest.KIND, VALID_USER, "theia+blueprint",
		VALID_WORKSPACE_NAME, VALID_WORKSPACE_LABEL, false, 3);

	/* act */
	ValidationResult result = request.validateDataFormat();

	/* assert */
	assertFalse(result.isOK(), result.toString());
	assertEquals(1, result.problems.size(), result.toString());
	assertEquals("appDefinition", result.problems.get(0).field, result.toString());
    }

    @Test
    public void testLaunchRequestInvalidAppDefinition4() {
	/* setup */
	LaunchRequest request = createLaunchRequest(VALID_APP_ID, LaunchRequest.KIND, VALID_USER,
		"theia.blueprint.theia.blueprint.theia.blueprint.theia.blueprint.theia.blueprint.theia.blueprint.theia.blueprint.theia.blueprint.theia.blueprint.theia.blueprint.theia.blueprint.theia.blueprint.theia.blueprint.theia.blueprint.theia.blueprint.theia.blueprin",
		VALID_WORKSPACE_NAME, VALID_WORKSPACE_LABEL, false, 3);

	/* act */
	ValidationResult result = request.validateDataFormat();

	/* assert */
	assertFalse(result.isOK(), result.toString());
	assertEquals(1, result.problems.size(), result.toString());
	assertEquals("appDefinition", result.problems.get(0).field, result.toString());
    }

    @Test
    public void testLaunchRequestValidAppDefinition() {
	/* setup */
	LaunchRequest request = createLaunchRequest(VALID_APP_ID, LaunchRequest.KIND, VALID_USER,
		"theia.blueprint.theia.blueprint.theia.blueprint.theia.blueprint.theia.blueprint.theia.blueprint.theia.blueprint.theia.blueprint.theia.blueprint.theia.blueprint.theia.blueprint.theia.blueprint.theia.blueprint.theia.blueprint.theia.blueprint.theia.bluepri",
		VALID_WORKSPACE_NAME, VALID_WORKSPACE_LABEL, false, 3);

	/* act */
	ValidationResult result = request.validateDataFormat();

	/* assert */
	assertTrue(result.isOK(), result.toString());
    }

    @Test
    public void testLaunchRequestInvalidWorkspaceName1() {
	/* setup */
	LaunchRequest request = createLaunchRequest(VALID_APP_ID, LaunchRequest.KIND, VALID_USER, VALID_APP_DEFINITION,
		"sdoe theia-workspace", VALID_WORKSPACE_LABEL, false, 3);

	/* act */
	ValidationResult result = request.validateDataFormat();

	/* assert */
	assertFalse(result.isOK(), result.toString());
	assertEquals(1, result.problems.size(), result.toString());
	assertEquals("workspaceName", result.problems.get(0).field, result.toString());
    }

    @Test
    public void testLaunchRequestInvalidWorkspaceName2() {
	/* setup */
	LaunchRequest request = createLaunchRequest(VALID_APP_ID, LaunchRequest.KIND, VALID_USER, VALID_APP_DEFINITION,
		"sdoe-theia-workspace\n---", VALID_WORKSPACE_LABEL, false, 3);

	/* act */
	ValidationResult result = request.validateDataFormat();

	/* assert */
	assertFalse(result.isOK(), result.toString());
	assertEquals(1, result.problems.size(), result.toString());
	assertEquals("workspaceName", result.problems.get(0).field, result.toString());
    }

    @Test
    public void testLaunchRequestInvalidWorkspaceName3() {
	/* setup */
	LaunchRequest request = createLaunchRequest(VALID_APP_ID, LaunchRequest.KIND, VALID_USER, VALID_APP_DEFINITION,
		"sdoe(theia-workspace)", VALID_WORKSPACE_LABEL, false, 3);

	/* act */
	ValidationResult result = request.validateDataFormat();

	/* assert */
	assertFalse(result.isOK(), result.toString());
	assertEquals(1, result.problems.size(), result.toString());
	assertEquals("workspaceName", result.problems.get(0).field, result.toString());
    }

    @Test
    public void testLaunchRequestInvalidWorkspaceName4() {
	/* setup */
	LaunchRequest request = createLaunchRequest(VALID_APP_ID, LaunchRequest.KIND, VALID_USER, VALID_APP_DEFINITION,
		"sdoe-theia-workspace-sdoe-theia-workspace-sdoe-theia-workspace-sdoe-theia-workspace-sdoe-theia-workspace-sdoe-theia-workspace-sdoe-theia-workspace-sdoe-theia-workspace-sdoe-theia-workspace-sdoe-theia-workspace-sdoe-theia-workspace-sdoe-theia-workspace-sd",
		VALID_WORKSPACE_LABEL, false, 3);

	/* act */
	ValidationResult result = request.validateDataFormat();

	/* assert */
	assertFalse(result.isOK(), result.toString());
	assertEquals(1, result.problems.size(), result.toString());
	assertEquals("workspaceName", result.problems.get(0).field, result.toString());
    }

    @Test
    public void testLaunchRequestValidWorkspaceName() {
	/* setup */
	LaunchRequest request = createLaunchRequest(VALID_APP_ID, LaunchRequest.KIND, VALID_USER, VALID_APP_DEFINITION,
		"sdoe-theia-workspace-sdoe-theia-workspace-sdoe-theia-workspace-sdoe-theia-workspace-sdoe-theia-workspace-sdoe-theia-workspace-sdoe-theia-workspace-sdoe-theia-workspace-sdoe-theia-workspace-sdoe-theia-workspace-sdoe-theia-workspace-sdoe-theia-workspace-s",
		VALID_WORKSPACE_LABEL, false, 3);

	/* act */
	ValidationResult result = request.validateDataFormat();

	/* assert */
	assertTrue(result.isOK(), result.toString());
    }

    @Test
    public void testLaunchRequestInvalidWorkspaceLabel() {
	/* setup */
	LaunchRequest request = createLaunchRequest(VALID_APP_ID, LaunchRequest.KIND, VALID_USER, VALID_APP_DEFINITION,
		VALID_WORKSPACE_NAME, "Sasha's Theia workspace\n---", false, 3);

	/* act */
	ValidationResult result = request.validateDataFormat();

	/* assert */
	assertFalse(result.isOK(), result.toString());
	assertEquals(1, result.problems.size(), result.toString());
	assertEquals("label", result.problems.get(0).field, result.toString());
    }

    @Test
    public void testLaunchRequestInvalidTimeout1() {
	/* setup */
	LaunchRequest request = createLaunchRequest(VALID_APP_ID, LaunchRequest.KIND, VALID_USER, VALID_APP_DEFINITION,
		VALID_WORKSPACE_NAME, VALID_WORKSPACE_LABEL, false, -1);

	/* act */
	ValidationResult result = request.validateDataFormat();

	/* assert */
	assertFalse(result.isOK(), result.toString());
	assertEquals(1, result.problems.size(), result.toString());
	assertEquals("timeout", result.problems.get(0).field, result.toString());
    }

    @Test
    public void testLaunchRequestInvalidTimeout2() {
	/* setup */
	LaunchRequest request = createLaunchRequest(VALID_APP_ID, LaunchRequest.KIND, VALID_USER, VALID_APP_DEFINITION,
		VALID_WORKSPACE_NAME, VALID_WORKSPACE_LABEL, false, 0);

	/* act */
	ValidationResult result = request.validateDataFormat();

	/* assert */
	assertFalse(result.isOK(), result.toString());
	assertEquals(1, result.problems.size(), result.toString());
	assertEquals("timeout", result.problems.get(0).field, result.toString());
    }

    @Test
    public void testLaunchRequestInvalidTimeout3() {
	/* setup */
	LaunchRequest request = createLaunchRequest(VALID_APP_ID, LaunchRequest.KIND, VALID_USER, VALID_APP_DEFINITION,
		VALID_WORKSPACE_NAME, VALID_WORKSPACE_LABEL, false, 61);

	/* act */
	ValidationResult result = request.validateDataFormat();

	/* assert */
	assertFalse(result.isOK(), result.toString());
	assertEquals(1, result.problems.size(), result.toString());
	assertEquals("timeout", result.problems.get(0).field, result.toString());
    }

    @Test
    public void testLaunchRequestValidTimeout() {
	/* setup */
	LaunchRequest request = createLaunchRequest(VALID_APP_ID, LaunchRequest.KIND, VALID_USER, VALID_APP_DEFINITION,
		VALID_WORKSPACE_NAME, VALID_WORKSPACE_LABEL, false, 60);

	/* act */
	ValidationResult result = request.validateDataFormat();

	/* assert */
	assertTrue(result.isOK(), result.toString());
    }

}
