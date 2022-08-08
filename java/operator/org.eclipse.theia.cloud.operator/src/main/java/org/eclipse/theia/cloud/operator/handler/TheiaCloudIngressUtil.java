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

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.operator.resource.AppDefinitionSpec;
import org.eclipse.theia.cloud.operator.resource.AppDefinitionSpecResource;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRule;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;

public final class TheiaCloudIngressUtil {

    private static final Logger LOGGER = LogManager.getLogger(TheiaCloudIngressUtil.class);

    public static final String PLACEHOLDER_INGRESSNAME = "placeholder-ingressname";
    public static final String PLACEHOLDER_HOST = "placeholder-host";

    private TheiaCloudIngressUtil() {
    }

    public static boolean checkForExistingIngressAndAddOwnerReferencesIfMissing(DefaultKubernetesClient client,
	    String namespace, AppDefinitionSpecResource appDefinition, String correlationId) {
	Optional<Ingress> existingIngressWithParentAppDefinition = K8sUtil.getExistingIngress(client, namespace,
		appDefinition.getMetadata().getName(), appDefinition.getMetadata().getUid());
	if (existingIngressWithParentAppDefinition.isPresent()) {
	    return true;
	}
	Optional<Ingress> ingress = K8sUtil.getExistingIngress(client, namespace,
		appDefinition.getSpec().getIngressname());
	if (ingress.isPresent()) {
	    OwnerReference ownerReference = new OwnerReference();
	    ownerReference.setApiVersion(HasMetadata.getApiVersion(AppDefinitionSpecResource.class));
	    ownerReference.setKind(AppDefinitionSpec.KIND);
	    ownerReference.setName(appDefinition.getMetadata().getName());
	    ownerReference.setUid(appDefinition.getMetadata().getUid());
	    addOwnerReferenceToIngress(client, namespace, ingress.get(), ownerReference);
	}
	return ingress.isPresent();
    }

    public static String getIngressName(AppDefinitionSpecResource appDefinition) {
	return appDefinition.getSpec().getIngressname();
    }

    public static Map<String, String> getIngressReplacements(String namespace,
	    AppDefinitionSpecResource appDefinition) {
	Map<String, String> replacements = new LinkedHashMap<String, String>();
	replacements.put(PLACEHOLDER_INGRESSNAME, getIngressName(appDefinition));
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_NAMESPACE, namespace);
	replacements.put(PLACEHOLDER_HOST, appDefinition.getSpec().getHost());
	return replacements;
    }

    public static void addOwnerReferenceToIngress(DefaultKubernetesClient client, String namespace, Ingress ingress,
	    OwnerReference ownerReference) {
	client.network().v1().ingresses().inNamespace(namespace).withName(ingress.getMetadata().getName())
		.edit(ingressToEdit -> {
		    ingressToEdit.getMetadata().getOwnerReferences().add(ownerReference);
		    return ingressToEdit;
		});
    }

    public static void removeIngressRule(DefaultKubernetesClient client, String namespace, Ingress ingress,
	    String searchedHost, String correlationId) {
	client.network().v1().ingresses().inNamespace(namespace).withName(ingress.getMetadata().getName())
		.edit(ingressToEdit -> {
		    IngressRule ruleToDelete = null;
		    for (IngressRule rule : ingressToEdit.getSpec().getRules()) {
			String ingressHost = rule.getHost();
			if (ingressHost == null) {
			    continue;
			}
			if (searchedHost.equals(ingressHost)) {
			    ruleToDelete = rule;
			    break;
			} else {
			    LOGGER.trace(formatLogMessage(correlationId, ingressHost + " is NOT " + searchedHost));
			}
		    }
		    if (ruleToDelete != null) {
			LOGGER.info(formatLogMessage(correlationId, "Removing ingress rule for path " + searchedHost));
			ingressToEdit.getSpec().getRules().remove(ruleToDelete);
		    }
		    return ingressToEdit;
		});
    }

}
