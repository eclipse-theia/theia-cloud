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

public class ValidationProblem {

    /* package */ String field;
    /* package */ Object value;
    /* package */ String reason;

    public ValidationProblem(String field, Object value, String reason) {
	this.field = field;
	this.value = value;
	this.reason = reason;
    }

    @Override
    public String toString() {
	return field + " with value '" + value + "' is not valid. Reason: " + reason;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((field == null) ? 0 : field.hashCode());
	result = prime * result + ((reason == null) ? 0 : reason.hashCode());
	result = prime * result + ((value == null) ? 0 : value.hashCode());
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	ValidationProblem other = (ValidationProblem) obj;
	if (field == null) {
	    if (other.field != null)
		return false;
	} else if (!field.equals(other.field))
	    return false;
	if (reason == null) {
	    if (other.reason != null)
		return false;
	} else if (!reason.equals(other.reason))
	    return false;
	if (value == null) {
	    if (other.value != null)
		return false;
	} else if (!value.equals(other.value))
	    return false;
	return true;
    }

}
