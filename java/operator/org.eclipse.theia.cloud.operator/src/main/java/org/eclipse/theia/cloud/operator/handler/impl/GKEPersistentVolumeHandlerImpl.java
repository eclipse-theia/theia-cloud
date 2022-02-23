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
package org.eclipse.theia.cloud.operator.handler.impl;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.operator.handler.K8sUtil;
import org.eclipse.theia.cloud.operator.handler.PersistentVolumeHandler;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudPersistentVolumeUtil;
import org.eclipse.theia.cloud.operator.util.JavaResourceUtil;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;

public class GKEPersistentVolumeHandlerImpl implements PersistentVolumeHandler {

    private static final Logger LOGGER = LogManager.getLogger(GKEPersistentVolumeHandlerImpl.class);

    protected static final String MOUNT_PATH = "/coffee-editor/backend/examples/data";
    protected static final int THEIA_CONTAINER_INDEX = 1;
    protected static final String USER_DATA = "user-data";

    protected static final String TEMPLATE_PERSISTENTVOLUMECLAIM_YAML = "/templatePersistentVolumeClaimGKE.yaml";

    @Override
    public void createAndApplyPersistentVolume(DefaultKubernetesClient client, String namespace, String correlationId,
	    Workspace workspace) {
	/* no needed ? */
    }

    @Override
    public void createAndApplyPersistentVolumeClaim(DefaultKubernetesClient client, String namespace,
	    String correlationId, Workspace workspace) {

	Map<String, String> replacements = TheiaCloudPersistentVolumeUtil
		.getPersistentVolumeClaimReplacements(namespace, workspace);
	String persistentVolumeClaimYaml;
	try {
	    persistentVolumeClaimYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(
		    GKEPersistentVolumeHandlerImpl.class, TEMPLATE_PERSISTENTVOLUMECLAIM_YAML, replacements,
		    correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while adjusting template for workspace " + workspace),
		    e);
	    return;
	}

	K8sUtil.loadAndCreatePersistentVolumeClaim(client, namespace, correlationId, persistentVolumeClaimYaml);
    }

    @Override
    public void addVolumeClaim(Deployment deployment, String pvcName) {
	PodSpec podSpec = deployment.getSpec().getTemplate().getSpec();

	Volume volume = new Volume();
	podSpec.getVolumes().add(volume);
	volume.setName(USER_DATA);
	PersistentVolumeClaimVolumeSource persistentVolumeClaim = new PersistentVolumeClaimVolumeSource();
	volume.setPersistentVolumeClaim(persistentVolumeClaim);
	persistentVolumeClaim.setClaimName(pvcName);

	Container theiaContainer = podSpec.getContainers().get(THEIA_CONTAINER_INDEX);

	VolumeMount volumeMount = new VolumeMount();
	theiaContainer.getVolumeMounts().add(volumeMount);
	volumeMount.setName(USER_DATA);
	volumeMount.setMountPath(MOUNT_PATH);
    }

}
