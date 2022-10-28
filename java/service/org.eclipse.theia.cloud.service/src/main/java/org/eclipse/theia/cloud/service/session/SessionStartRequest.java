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
package org.eclipse.theia.cloud.service.session;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.theia.cloud.common.validation.Validate;
import org.eclipse.theia.cloud.common.validation.ValidationProblem;
import org.eclipse.theia.cloud.common.validation.ValidationResult;
import org.eclipse.theia.cloud.service.ServiceRequest;

@Schema(name = "SessionStartRequest", description = "A request to start a session")
public final class SessionStartRequest extends ServiceRequest {
    public static final String KIND = "sessionStartRequest";

    @Schema(description = "The user identification, usually the email address.", required = true)
    public String user;

    @Schema(description = "The app to launch.", required = true)
    public String appDefinition;

    @Schema(description = "The name of the workspace to mount/create.", required = false)
    public String workspaceName;

    @Schema(description = "Number of minutes to wait for session launch. Default is 3 Minutes.", required = false)
    public int timeout = 3;

    public SessionStartRequest() {
	super(KIND);
    }

    public SessionStartRequest(String appId, String user, String appDefinition, String workspaceName, int timeout) {
	super(KIND, appId);
	this.user = user;
	this.appDefinition = appDefinition;
	this.workspaceName = workspaceName;
	this.timeout = timeout;
    }

    public SessionStartRequest(String appId, String user, String appDefinition, int timeout) {
	super(KIND, appId);
	this.user = user;
	this.appDefinition = appDefinition;
	this.timeout = timeout;
    }

    public SessionStartRequest(String appId, String user, String appDefinition) {
	super(KIND, appId);
	this.user = user;
	this.appDefinition = appDefinition;
    }

    @Schema(hidden = true)
    public boolean isEphemeral() {
	return this.workspaceName == null || this.workspaceName.isBlank();
    }

    @Override
    public String toString() {
	return "SessionStartRequest [user=" + user + ", appDefinition=" + appDefinition + ", workspaceName="
		+ workspaceName + ", appId=" + appId + ", kind=" + kind + "]";
    }

    @Override
    public ValidationResult validateDataFormat() {
	List<ValidationProblem> problems = new ArrayList<ValidationProblem>();
	validateServiceRequest(problems);
	Validate.user(user).ifPresent(problems::add);
	Validate.appDefinitionName(appDefinition).ifPresent(problems::add);
	Validate.workspaceName(workspaceName).ifPresent(problems::add);
	Validate.timeoutMinutes(timeout).ifPresent(problems::add);
	return new ValidationResult(problems);
    }

}
