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

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.theia.cloud.operator.resource.TemplateSpecResource;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;

public final class TheiaCloudIngressUtil {

    public static final String INGRESS_NAME_POSTFIX = "-ingress";

    public static final String PLACEHOLDER_INGRESSNAME = "placeholder-ingressname";
    public static final String PLACEHOLDER_HOST = "placeholder-host";

    private TheiaCloudIngressUtil() {
    }

    public static boolean hasExistingIngress(DefaultKubernetesClient client, String namespace,
	    TemplateSpecResource template) {
	return K8sUtil
		.getExistingIngress(client, namespace, template.getMetadata().getName(),
			template.getMetadata().getUid())//
		.isPresent();
    }

    public static String getIngressName(TemplateSpecResource template) {
	return template.getSpec().getName() + INGRESS_NAME_POSTFIX;
    }

    public static String getHostName(TemplateSpecResource template, int instance) {
	return template.getSpec().getName() + "." + instance + "." + template.getSpec().getHost();
    }

    public static Map<String, String> getIngressReplacements(String namespace, TemplateSpecResource template) {
	Map<String, String> replacements = new LinkedHashMap<String, String>();
	replacements.put(PLACEHOLDER_INGRESSNAME, getIngressName(template));
	replacements.put(TheiaCloudHandlerUtil.PLACEHOLDER_NAMESPACE, namespace);
	replacements.put(PLACEHOLDER_HOST, template.getSpec().getHost());
	return replacements;
    }

}
