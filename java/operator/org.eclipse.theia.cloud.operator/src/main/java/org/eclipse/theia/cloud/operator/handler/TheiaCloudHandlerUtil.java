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
package org.eclipse.theia.cloud.operator.handler;

import static org.eclipse.theia.cloud.operator.util.LogMessageUtil.formatLogMessage;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.operator.resource.TemplateSpecResource;
import org.eclipse.theia.cloud.operator.resource.TemplateSpecResourceList;
import org.eclipse.theia.cloud.operator.resource.WorkspaceSpec;
import org.eclipse.theia.cloud.operator.resource.WorkspaceSpecResource;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;

public final class TheiaCloudHandlerUtil {

    private static final Logger LOGGER = LogManager.getLogger(TheiaCloudHandlerUtil.class);

    public static final String PLACEHOLDER_NAMESPACE = "placeholder-namespace";
    public static final String PLACEHOLDER_APP = "placeholder-app";
    public static final String PLACEHOLDER_PORT = "placeholder-port";
    public static final String PLACEHOLDER_EMAILSCONFIGNAME = "placeholder-emailsconfigname";
    public static final String PLACEHOLDER_CONFIGNAME = "placeholder-configname";

    private TheiaCloudHandlerUtil() {
    }

    public static <T extends HasMetadata> Set<Integer> computeIdsOfMissingItems(int instances, List<T> existingItems,
	    Function<T, Integer> getId) {
	Set<Integer> missing = IntStream.rangeClosed(1, instances).boxed().collect(Collectors.toSet());
	existingItems.stream()//
		.map(getId)//
		.forEach(missing::remove);
	return missing;
    }

    public static String getAppSelector(TemplateSpecResource template, int instance) {
	return template.getSpec().getName() + "-" + instance;
    }

    public static Optional<TemplateSpecResource> getTemplateSpecForWorkspace(DefaultKubernetesClient client,
	    String namespace, String templateID) {
	Optional<TemplateSpecResource> template = client
		.customResources(TemplateSpecResource.class, TemplateSpecResourceList.class).inNamespace(namespace)
		.list().getItems().stream()//
		.filter(templateSpecResource -> templateID.equals(templateSpecResource.getSpec().getName()))//
		.findAny();
	return template;
    }

    public static <T extends HasMetadata> T addOwnerReferenceToItem(String correlationId, String workspaceResourceName,
	    String workspaceResourceUID, T item) {
	OwnerReference serviceOwnerReference = createOwnerReference(workspaceResourceName, workspaceResourceUID);
	LOGGER.info(formatLogMessage(correlationId, "Adding a new owner reference to " + item.getMetadata().getName()));
	item.getMetadata().getOwnerReferences().add(serviceOwnerReference);
	return item;
    }

    public static OwnerReference createOwnerReference(String workspaceResourceName, String workspaceResourceUID) {
	OwnerReference ownerReference = new OwnerReference();
	ownerReference.setApiVersion(HasMetadata.getApiVersion(WorkspaceSpecResource.class));
	ownerReference.setKind(WorkspaceSpec.KIND);
	ownerReference.setName(workspaceResourceName);
	ownerReference.setUid(workspaceResourceUID);
	return ownerReference;
    }

}
