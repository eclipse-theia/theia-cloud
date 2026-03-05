/********************************************************************************
 * Copyright (C) 2022-2023 EclipseSource, Lockular, Ericsson, STMicroelectronics and 
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
package org.eclipse.theia.cloud.operator.handler.appdef;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.OperatorStatus;
import org.eclipse.theia.cloud.common.k8s.resource.ResourceStatus;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionSpec;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionStatus;
import org.eclipse.theia.cloud.operator.ingress.IngressPathProvider;
import org.eclipse.theia.cloud.operator.util.TheiaCloudIngressUtil;

import com.google.inject.Inject;

public class LazyStartAppDefinitionHandler implements AppDefinitionHandler {

    private static final Logger LOGGER = LogManager.getLogger(LazyStartAppDefinitionHandler.class);

    @Inject
    protected TheiaCloudClient client;

    @Inject
    protected IngressPathProvider ingressPathProvider;

    @Override
    public boolean appDefinitionAdded(AppDefinition appDefinition, String correlationId) {
        try {
            return doAppDefinitionAdded(appDefinition, correlationId);
        } catch (Throwable ex) {
            LOGGER.error(formatLogMessage(correlationId,
                    "An unexpected exception occurred while adding AppDefinition: " + appDefinition), ex);
            client.appDefinitions().updateStatus(correlationId, appDefinition, status -> {
                status.setOperatorStatus(OperatorStatus.ERROR);
                status.setOperatorMessage("Unexpected error. Please check the logs for correlationId " + correlationId);
            });
            return false;
        }
    }

    protected boolean doAppDefinitionAdded(AppDefinition appDefinition, String correlationId) {
        LOGGER.info(formatLogMessage(correlationId, "Handling " + appDefinition));

        // Check current session status and ignore if handling failed or finished before
        Optional<AppDefinitionStatus> status = Optional.ofNullable(appDefinition.getStatus());
        String operatorStatus = status.map(ResourceStatus::getOperatorStatus).orElse(OperatorStatus.NEW);
        if (OperatorStatus.HANDLED.equals(operatorStatus)) {
            LOGGER.trace(formatLogMessage(correlationId,
                    "AppDefinition was successfully handled before and is skipped now. AppDefinition: "
                            + appDefinition));
            return true;
        }
        if (OperatorStatus.HANDLING.equals(operatorStatus)) {
            // TODO We should not return but continue where we left off.
            LOGGER.warn(formatLogMessage(correlationId,
                    "AppDefinition handling was unexpectedly interrupted before. AppDefinition is skipped now and its status is set to ERROR. AppDefinition: "
                            + appDefinition));
            client.appDefinitions().updateStatus(correlationId, appDefinition, s -> {
                s.setOperatorStatus(OperatorStatus.ERROR);
                s.setOperatorMessage("Handling was unexpectedly interrupted before. CorrelationId: " + correlationId);
            });
            return false;
        }
        if (OperatorStatus.ERROR.equals(operatorStatus)) {
            LOGGER.warn(formatLogMessage(correlationId,
                    "AppDefinition could not be handled before and is skipped now. AppDefinition: " + appDefinition));
            return false;
        }

        // Set app definition status to being handled
        client.appDefinitions().updateStatus(correlationId, appDefinition, s -> {
            s.setOperatorStatus(OperatorStatus.HANDLING);
        });

        AppDefinitionSpec spec = appDefinition.getSpec();
        String appDefinitionResourceName = appDefinition.getMetadata().getName();

        /* Create ingress if not existing */
        if (!TheiaCloudIngressUtil.checkForExistingIngressAndAddOwnerReferencesIfMissing(client.kubernetes(),
                client.namespace(), appDefinition, correlationId)) {
            LOGGER.error(formatLogMessage(correlationId,
                    "Expected HTTPRoute '" + spec.getIngressname() + "' for app definition '" + appDefinitionResourceName
                            + "' does not exist. Abort handling app definition."));
            client.appDefinitions().updateStatus(correlationId, appDefinition, s -> {
                s.setOperatorStatus(OperatorStatus.ERROR);
                s.setOperatorMessage("HTTPRoute does not exist.");
            });
            return false;
        } else {
            LOGGER.trace(formatLogMessage(correlationId, "HTTPRoute available already"));
        }

        client.appDefinitions().updateStatus(correlationId, appDefinition, s -> {
            s.setOperatorStatus(OperatorStatus.HANDLED);
        });
        return true;
    }

}
