/********************************************************************************
 * Copyright (C) 2022-2023 EclipseSource, STMicroelectronics and others.
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

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import io.quarkus.security.identity.SecurityIdentity;

/**
 * <p>
 * Produces an injectable theia cloud user instance based on the authenticated
 * user. This maps the user's {@link SecurityIdentity} to an injectable
 * {@link TheiaCloudUser} POJO.
 * </p>
 * With this, the {@link TheiaCloudUser} can directly be injected into any
 * resource.
 */
@RequestScoped
public class TheiaCloudUserProducer {

    private final Logger logger = Logger.getLogger(getClass());

    @Inject
    private SecurityIdentity identity;

    @Produces
    @RequestScoped
    TheiaCloudUser getTheiaCloudUser() {
	if (identity.isAnonymous()) {
	    logger.debug("Anonymous user. Return empty TheiaCloudUser.");
	    return TheiaCloudUser.EMPTY;
	}

	String email = null;
	if (identity.getPrincipal() instanceof JsonWebToken) {
	    JsonWebToken jwt = (JsonWebToken) identity.getPrincipal();
	    email = jwt.getClaim(Claims.email);
	} else {
	    // Should never happen when using OpenID Connect but log just in case.
	    logger.errorv("Cannot extract user info: Auth token is not a JWT but a {0}.",
		    identity.getPrincipal().getClass().getName());
	}
	return new TheiaCloudUser(email);
    }
}
