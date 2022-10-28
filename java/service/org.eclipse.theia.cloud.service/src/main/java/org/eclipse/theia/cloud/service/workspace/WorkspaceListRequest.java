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
package org.eclipse.theia.cloud.service.workspace;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.theia.cloud.common.validation.Validate;
import org.eclipse.theia.cloud.common.validation.ValidationProblem;
import org.eclipse.theia.cloud.common.validation.ValidationResult;
import org.eclipse.theia.cloud.service.ServiceRequest;

@Schema(name = "WorkspaceListRequest", description = "Request to list workspaces of a user.")
public final class WorkspaceListRequest extends ServiceRequest {
    public static final String KIND = "workspaceListRequest";

    @Schema(description = "The user identification, usually the email address.", required = true)
    public String user;

    public WorkspaceListRequest() {
	super(KIND);
    }

    public WorkspaceListRequest(String appId, String user) {
	super(KIND, appId);
	this.user = user;
    }

    @Override
    public String toString() {
	return "WorkspaceListRequest [user=" + user + ", appId=" + appId + ", kind=" + kind + "]";
    }

    @Override
    public ValidationResult validateDataFormat() {
	List<ValidationProblem> problems = new ArrayList<ValidationProblem>();
	validateServiceRequest(problems);
	Validate.user(user).ifPresent(problems::add);
	return new ValidationResult(problems);
    }

}
