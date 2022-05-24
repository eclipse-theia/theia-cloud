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
import org.eclipse.theia.cloud.common.k8s.resource.Session;
import org.eclipse.theia.cloud.operator.handler.K8sUtil;
import org.eclipse.theia.cloud.operator.handler.PersistentVolumeHandler;
import org.eclipse.theia.cloud.operator.handler.TheiaCloudPersistentVolumeUtil;
import org.eclipse.theia.cloud.operator.resource.AppDefinitionSpec;
import org.eclipse.theia.cloud.operator.util.JavaResourceUtil;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;

public class PersistentVolumeHandlerImpl implements PersistentVolumeHandler {

    private static final Logger LOGGER = LogManager.getLogger(PersistentVolumeHandlerImpl.class);

    protected static final String USER_DATA = "user-data";

    protected static final String TEMPLATE_PERSISTENTVOLUME_YAML = "/templatePersistentVolume.yaml";
    protected static final String TEMPLATE_PERSISTENTVOLUMECLAIM_YAML = "/templatePersistentVolumeClaim.yaml";

    @Override
    public void createAndApplyPersistentVolume(DefaultKubernetesClient client, String namespace, String correlationId,
	    AppDefinitionSpec appDefinition, Session session) {
	Map<String, String> replacements = TheiaCloudPersistentVolumeUtil.getPersistentVolumeReplacements(namespace,
		session);
	String persistentVolumeYaml;
	try {
	    persistentVolumeYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(TEMPLATE_PERSISTENTVOLUME_YAML,
		    replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while adjusting template for session " + session), e);
	    return;
	}
	K8sUtil.loadAndCreatePersistentVolume(client, namespace, correlationId, persistentVolumeYaml);
    }

    @Override
    public void createAndApplyPersistentVolumeClaim(DefaultKubernetesClient client, String namespace,
	    String correlationId, AppDefinitionSpec appDefinition, Session session) {

	Map<String, String> replacements = TheiaCloudPersistentVolumeUtil
		.getPersistentVolumeClaimReplacements(namespace, session);
	String persistentVolumeClaimYaml;
	try {
	    persistentVolumeClaimYaml = JavaResourceUtil.readResourceAndReplacePlaceholders(
		    TEMPLATE_PERSISTENTVOLUMECLAIM_YAML, replacements, correlationId);
	} catch (IOException | URISyntaxException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Error while adjusting template for session " + session), e);
	    return;
	}

	K8sUtil.loadAndCreatePersistentVolumeClaim(client, namespace, correlationId, persistentVolumeClaimYaml);
    }

    @Override
    public void addVolumeClaim(Deployment deployment, String pvcName, AppDefinitionSpec appDefinition) {
	PodSpec podSpec = deployment.getSpec().getTemplate().getSpec();

	Volume volume = new Volume();
	podSpec.getVolumes().add(volume);
	volume.setName(USER_DATA);
	PersistentVolumeClaimVolumeSource persistentVolumeClaim = new PersistentVolumeClaimVolumeSource();
	volume.setPersistentVolumeClaim(persistentVolumeClaim);
	persistentVolumeClaim.setClaimName(pvcName);

	Container theiaContainer = TheiaCloudPersistentVolumeUtil.getTheiaContainer(podSpec, appDefinition);

	VolumeMount volumeMount = new VolumeMount();
	theiaContainer.getVolumeMounts().add(volumeMount);
	volumeMount.setName(USER_DATA);
	volumeMount.setMountPath(TheiaCloudPersistentVolumeUtil.getMountPath(appDefinition));
    }

}
