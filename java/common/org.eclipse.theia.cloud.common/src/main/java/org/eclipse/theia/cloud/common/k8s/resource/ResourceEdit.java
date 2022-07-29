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
package org.eclipse.theia.cloud.common.k8s.resource;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.fabric8.kubernetes.api.model.HasMetadata;

public final class ResourceEdit {
    private static final Logger LOGGER = LogManager.getLogger(ResourceEdit.class);

    private ResourceEdit() {
    }

    public static <T extends HasMetadata> Consumer<T> updateOwnerReference(int index, String apiVersion, String kind,
	    String name, String uid, String correlationId) {
	return item -> {
	    if (item.getMetadata().getOwnerReferences().size() > index && index >= 0) {
		LOGGER.trace(formatLogMessage(correlationId, "Updating owner reference at index " + index));
		item.getMetadata().getOwnerReferences().get(index).setApiVersion(apiVersion);
		item.getMetadata().getOwnerReferences().get(index).setKind(kind);
		item.getMetadata().getOwnerReferences().get(index).setName(name);
		item.getMetadata().getOwnerReferences().get(index).setUid(uid);
	    } else {
		LOGGER.trace(formatLogMessage(correlationId, "No owner reference at index " + index));
	    }
	};
    }
}
