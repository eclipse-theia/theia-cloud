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
package org.eclipse.theia.cloud.operator.handler.impl;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.operator.handler.PersistentVolumeCreator;
import org.eclipse.theia.cloud.operator.handler.PersistentVolumeTemplateReplacements;
import org.eclipse.theia.cloud.operator.util.JavaResourceUtil;

import com.google.inject.Inject;

import io.fabric8.kubernetes.api.model.PersistentVolume;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;

/**
 * Creates persistent volume claims using a storage class. Kubernetes creates
 * the persistent volume based on the storage class's configuration on the
 * cluster. Thus, explicit creation of a persistent volume is not required.
 */
public class DefaultPersistentVolumeCreator implements PersistentVolumeCreator {

    private static final Logger LOGGER = LogManager.getLogger(DefaultPersistentVolumeCreator.class);


    protected static final String TEMPLATE_PERSISTENTVOLUMECLAIM_YAML = "/templatePersistentVolumeClaim.yaml";

    @Inject
    protected TheiaCloudClient client;

    @Inject
    protected PersistentVolumeTemplateReplacements replacementsProvider;

    @Override
    public Optional<PersistentVolume> createAndApplyPersistentVolume(String correlationId, Workspace workspace) {
	return Optional.empty();
    }

    @Override
    public Optional<PersistentVolumeClaim> createAndApplyPersistentVolumeClaim(String correlationId,
	    Workspace workspace) {

	Map<String, String> replacements = replacementsProvider.getPersistentVolumeClaimReplacements(client.namespace(),
		workspace);
	String persistentVolumeClaimYaml;
	try {
	    persistentVolumeClaimYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(
		    TEMPLATE_PERSISTENTVOLUMECLAIM_YAML, replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while adjusting template for workspace " + workspace),
		    e);
	    return Optional.empty();
	}
	return client.persistentVolumeClaims().loadAndCreate(correlationId, persistentVolumeClaimYaml,
		claim -> claim.addOwnerReference(workspace));
    }

}
