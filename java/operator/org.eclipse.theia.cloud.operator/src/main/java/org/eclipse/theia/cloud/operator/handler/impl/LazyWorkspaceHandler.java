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

import org.eclipse.theia.cloud.common.k8s.resource.Session;
import org.eclipse.theia.cloud.common.k8s.resource.SessionSpecResourceList;
import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.common.k8s.resource.WorkspaceSpecResourceList;
import org.eclipse.theia.cloud.common.k8s.resource.WorkspaceUtil;
import org.eclipse.theia.cloud.operator.TheiaCloudArguments;
import org.eclipse.theia.cloud.operator.di.TheiaCloudOperatorModule;
import org.eclipse.theia.cloud.operator.handler.PersistentVolumeHandler;
import org.eclipse.theia.cloud.operator.handler.WorkspaceHandler;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.fabric8.kubernetes.api.model.PersistentVolume;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimList;
import io.fabric8.kubernetes.api.model.PersistentVolumeList;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public class LazyWorkspaceHandler implements WorkspaceHandler {
    @Inject
    protected NamespacedKubernetesClient client;

    @Inject
    @Named(TheiaCloudOperatorModule.NAMESPACE)
    protected String namespace;

    @Inject
    protected PersistentVolumeHandler persistentVolumeHandler;

    @Inject
    protected TheiaCloudArguments arguments;

    @Inject
    private NonNamespaceOperation<Session, SessionSpecResourceList, Resource<Session>> sessionResourceClient;

    @Inject
    private NonNamespaceOperation<Workspace, WorkspaceSpecResourceList, Resource<Workspace>> workspaceResourceClient;

    @Override
    public boolean workspaceAdded(Workspace workspace, String correlationId) {
	if (arguments.isEphemeralStorage()) {
	    editWorkspaceStorage(workspace, "ephemeral");
	    return true; // no problem
	}

	String storageName = WorkspaceUtil.getStorageName(workspace.getSpec().getName());
	NonNamespaceOperation<PersistentVolume, PersistentVolumeList, Resource<PersistentVolume>> volumes = client
		.persistentVolumes();
	if (volumes.withName(storageName).get() == null) {
	    persistentVolumeHandler.createAndApplyPersistentVolume(correlationId, workspace);
	}

	NonNamespaceOperation<PersistentVolumeClaim, PersistentVolumeClaimList, Resource<PersistentVolumeClaim>> claims = client
		.persistentVolumeClaims().inNamespace(namespace);
	if (claims.withName(storageName).get() == null) {
	    persistentVolumeHandler.createAndApplyPersistentVolumeClaim(correlationId, workspace);
	}

	editWorkspaceStorage(workspace, storageName);
	return true;
    }

    private void editWorkspaceStorage(Workspace workspace, String storage) {
	workspaceResourceClient.withName(workspace.getSpec().getName()).edit(toEdit -> {
	    toEdit.getSpec().setStorage(storage);
	    return toEdit;
	});
    }

    @Override
    public boolean workspaceDeleted(Workspace workspace, String correlationId) {
	String sessionName = WorkspaceUtil.getSessionName(workspace.getSpec().getName());
	sessionResourceClient.withName(sessionName).delete();

	String storageName = WorkspaceUtil.getStorageName(workspace.getSpec().getName());

	NonNamespaceOperation<PersistentVolumeClaim, PersistentVolumeClaimList, Resource<PersistentVolumeClaim>> claims = client
		.persistentVolumeClaims().inNamespace(namespace);
	claims.withName(storageName).delete();

	NonNamespaceOperation<PersistentVolume, PersistentVolumeList, Resource<PersistentVolume>> volumes = client
		.persistentVolumes();
	volumes.withName(storageName).delete();
	return true;
    }

}
