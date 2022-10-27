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
package org.eclipse.theia.cloud.service.session;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.theia.cloud.service.ServiceRequest;
import org.eclipse.theia.cloud.service.validation.Validate;
import org.eclipse.theia.cloud.service.validation.ValidationProblem;
import org.eclipse.theia.cloud.service.validation.ValidationResult;

@Schema(name = "SessionActivityRequest", description = "A request to report activity for a running session.")
public final class SessionActivityRequest extends ServiceRequest {
    public static final String KIND = "sessionActivityRequest";

    @Schema(description = "The name of the session for which activity is reported.", required = true)
    public String sessionName;

    public SessionActivityRequest() {
	super(KIND);
    }

    public SessionActivityRequest(String appId, String sessionName) {
	super(KIND, appId);
	this.sessionName = sessionName;
    }

    @Override
    public String toString() {
	return "SessionActivityRequest [sessionName=" + sessionName + ", appId=" + appId + ", kind=" + kind + "]";
    }

    @Override
    public ValidationResult validateDataFormat() {
	List<ValidationProblem> problems = new ArrayList<ValidationProblem>();
	validateServiceRequest(problems);
	Validate.existingSessionName(sessionName).ifPresent(problems::add);
	return new ValidationResult(problems);
    }

}
