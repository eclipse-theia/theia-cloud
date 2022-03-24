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
package org.eclipse.theia.cloud.workspace;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.theia.cloud.common.k8s.resource.Workspace;
import org.eclipse.theia.cloud.common.k8s.resource.WorkspaceSpec;
import org.eclipse.theia.cloud.common.k8s.resource.WorkspaceSpecResourceList;
import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;

public final class K8sUtil {

    private static final Logger LOGGER = Logger.getLogger(K8sUtil.class);

    private static final String COR_ID_INIT = "init";

    private static String NAMESPACE = "";
    private static DefaultKubernetesClient CLIENT = createClient();

    private K8sUtil() {
    }

    private static DefaultKubernetesClient createClient() {
	Config config = new ConfigBuilder().build();

	/* don't close resource */
	DefaultKubernetesClient client = new DefaultKubernetesClient(config);

	String namespace = client.getNamespace();
	K8sUtil.NAMESPACE = namespace;
	LOGGER.info(formatLogMessage(COR_ID_INIT, "Namespace: " + namespace));

	String workspaceAPIVersion = HasMetadata.getApiVersion(Workspace.class);
	LOGGER.info(
		formatLogMessage(COR_ID_INIT, "Registering WorkspaceSpecResource in version " + workspaceAPIVersion));
	KubernetesDeserializer.registerCustomKind(workspaceAPIVersion, WorkspaceSpec.KIND, Workspace.class);

	return client;
    }

    public static Reply launchWorkspace(String correlationId, String name, String template, String user) {

	NonNamespaceOperation<Workspace, WorkspaceSpecResourceList, Resource<Workspace>> workspaces = CLIENT
		.customResources(Workspace.class, WorkspaceSpecResourceList.class).inNamespace(K8sUtil.NAMESPACE);

	String createSpecName;
	Resource<Workspace> existingWorkspace = workspaces.withName(name);
	WorkspaceSpec existingWorkspaceSpec = null;
	if (existingWorkspace.get() == null) {
	    Workspace workspaceSpecResource = new Workspace();

	    ObjectMeta metadata = new ObjectMeta();
	    workspaceSpecResource.setMetadata(metadata);
	    metadata.setName(name);

	    WorkspaceSpec workspaceSpec = new WorkspaceSpec(name, template, user);
	    workspaceSpecResource.setSpec(workspaceSpec);

	    Workspace created = workspaces.create(workspaceSpecResource);
	    createSpecName = created.getSpec().getName();
	} else {
	    createSpecName = existingWorkspace.get().getSpec().getName();
	    existingWorkspaceSpec = existingWorkspace.get().getSpec();
	}

	AtomicReference<String> atomicReferenceURL = new AtomicReference<String>(null);
	AtomicReference<String> atomicReferenceError = new AtomicReference<String>(null);
	CountDownLatch latch = new CountDownLatch(1);

	Watch watch = null;
	if (existingWorkspaceSpec != null
		&& ((existingWorkspaceSpec.getUrl() != null && !existingWorkspaceSpec.getUrl().isBlank())
			|| (existingWorkspaceSpec.getError() != null && !existingWorkspaceSpec.getError().isBlank()))) {
	    LOGGER.info(formatLogMessage(correlationId, "Workspace existing with result"));
	    if (existingWorkspaceSpec.getUrl() != null && !existingWorkspaceSpec.getUrl().isBlank()) {
		atomicReferenceURL.set(existingWorkspaceSpec.getUrl());
		latch.countDown();
	    }
	    if (existingWorkspaceSpec.getError() != null && !existingWorkspaceSpec.getError().isBlank()) {
		atomicReferenceError.set(existingWorkspaceSpec.getError());
		workspaces.withName(name).delete();
		latch.countDown();
	    }
	} else {
	    watch = workspaces.watch(new Watcher<Workspace>() {

		@Override
		public void eventReceived(Action action, Workspace resource) {
		    LOGGER.trace(
			    formatLogMessage(correlationId, "Received workspace event " + action + " for " + resource));
		    if (createSpecName.equals(resource.getSpec().getName())) {
			if (resource.getSpec().getUrl() != null && !resource.getSpec().getUrl().isBlank()) {
			    LOGGER.info(formatLogMessage(correlationId, "Received URL for " + resource));
			    atomicReferenceURL.set(resource.getSpec().getUrl());
			    latch.countDown();
			} else if (resource.getSpec().getError() != null && !resource.getSpec().getError().isBlank()) {
			    LOGGER.info(formatLogMessage(correlationId,
				    "Received Error for " + resource + ". Deleting workspace again."));
			    atomicReferenceError.set(resource.getSpec().getError());
			    workspaces.withName(name).delete();
			    latch.countDown();
			}
		    }
		}

		@Override
		public void onClose(WatcherException cause) {
		}

	    });
	}

	try {
	    latch.await(1, TimeUnit.MINUTES);
	} catch (InterruptedException e) {
	    LOGGER.error(formatLogMessage(correlationId, "Timeout while waiting for URL for " + name), e);
	    return new Reply(false, "", "Unable to start workspace");
	} finally {
	    if (watch != null) {
		watch.close();
	    }
	}

	return new Reply(atomicReferenceURL.get() != null, atomicReferenceURL.get(), atomicReferenceError.get());

    }

}
