package org.eclipse.theia.cloud.operator.handler.appdef;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinition;
import org.eclipse.theia.cloud.common.k8s.resource.appdefinition.AppDefinitionSpec;
import org.eclipse.theia.cloud.operator.pool.PrewarmedResourcePool;
import org.eclipse.theia.cloud.common.tracing.Tracing;
import io.sentry.Sentry;
import org.eclipse.theia.cloud.operator.util.TheiaCloudIngressUtil;

import com.google.inject.Inject;

import io.sentry.ISpan;
import io.sentry.ITransaction;
import io.sentry.SpanStatus;

/**
 * A {@link AppDefinitionHandler} that manages a pool of prewarmed deployments for eager start sessions. This handler
 * delegates pool management to {@link PrewarmedResourcePool}.
 */
public class EagerStartAppDefinitionAddedHandler implements AppDefinitionHandler {

    private static final Logger LOGGER = LogManager.getLogger(EagerStartAppDefinitionAddedHandler.class);

    @Inject
    private TheiaCloudClient client;

    @Inject
    private PrewarmedResourcePool pool;

    @Override
    public boolean appDefinitionAdded(AppDefinition appDefinition, String correlationId) {
        AppDefinitionSpec spec = appDefinition.getSpec();
        String appDefName = appDefinition.getMetadata().getName();
        int minInstances = spec.getMinInstances();

        ITransaction tx = Tracing.startTransaction("appdef.added", "appdef");
        tx.setTag("app_definition", appDefName);
        tx.setData("min_instances", minInstances);
        tx.setData("correlation_id", correlationId);
        tx.setTag("appdef.action", "added");

        LOGGER.info(formatLogMessage(correlationId, "Handling " + spec));

        try {
            // Verify ingress exists
            ISpan ingressSpan = Tracing.childSpan(tx, "appdef.verify_ingress", "Verify ingress exists");
            ingressSpan.setData("route_name", spec.getIngressname());

            if (!TheiaCloudIngressUtil.checkForExistingIngressAndAddOwnerReferencesIfMissing(client.kubernetes(),
                    client.namespace(), appDefinition, correlationId)) {
                LOGGER.error(formatLogMessage(correlationId,
                        "Expected HTTPRoute '" + spec.getIngressname() + "' for app definition '" + appDefName
                                + "' does not exist. Abort handling app definition."));
                ingressSpan.setTag("outcome", "not_found");
                Tracing.finish(ingressSpan, SpanStatus.NOT_FOUND);
                tx.setTag("error.reason", "ingress_not_found");
                tx.setTag("outcome", "failure");
                Tracing.finish(tx, SpanStatus.NOT_FOUND);
                return false;
            }
            Tracing.finishSuccess(ingressSpan);

            LOGGER.trace(formatLogMessage(correlationId, "HTTPRoute available"));

            // Ensure pool has minimum capacity
            ISpan poolSpan = Tracing.childSpan(tx, "appdef.ensure_capacity", "Ensure pool capacity");
            poolSpan.setData("min_instances", minInstances);

            boolean success = pool.ensureCapacity(appDefinition, minInstances, correlationId);

            poolSpan.setTag("pool.operation", "ensure_capacity");
            poolSpan.setTag("outcome", success ? "success" : "failure");
            Tracing.finish(poolSpan, success ? SpanStatus.OK : SpanStatus.INTERNAL_ERROR);

            tx.setData("min_instances", minInstances);
            tx.setTag("outcome", success ? "success" : "failure");
            Tracing.finish(tx, success ? SpanStatus.OK : SpanStatus.INTERNAL_ERROR);
            return success;

        } catch (Exception e) {
            Tracing.finishError(tx, e);
            throw e;
        }
    }

    @Override
    public boolean appDefinitionDeleted(AppDefinition appDefinition, String correlationId) {
        AppDefinitionSpec spec = appDefinition.getSpec();
        String appDefName = appDefinition.getMetadata().getName();

        ITransaction tx = Tracing.startTransaction("appdef.deleted", "appdef");
        tx.setTag("app_definition", appDefName);
        tx.setData("min_instances", spec.getMinInstances());
        tx.setData("correlation_id", correlationId);
        tx.setTag("appdef.action", "deleted");

        LOGGER.info(formatLogMessage(correlationId, "Deleting resources for " + spec));

        try {
            // Release all pool resources
            ISpan releaseSpan = Tracing.childSpan(tx, "appdef.release_all", "Release all pool resources");
            releaseSpan.setTag("pool.operation", "release_all");

            boolean success = pool.releaseAll(appDefinition, correlationId);

            releaseSpan.setTag("outcome", success ? "success" : "failure");
            Tracing.finish(releaseSpan, success ? SpanStatus.OK : SpanStatus.INTERNAL_ERROR);
            tx.setTag("outcome", success ? "success" : "failure");
            Tracing.finish(tx, success ? SpanStatus.OK : SpanStatus.INTERNAL_ERROR);
            return success;

        } catch (Exception e) {
            Tracing.finishError(tx, e);
            throw e;
        }
    }

    @Override
    public boolean appDefinitionModified(AppDefinition appDefinition, String correlationId) {
        AppDefinitionSpec spec = appDefinition.getSpec();
        String appDefName = appDefinition.getMetadata().getName();
        int minInstances = spec.getMinInstances();

        ITransaction tx = Tracing.startTransaction("appdef.modified", "appdef");
        tx.setTag("app_definition", appDefName);
        tx.setData("min_instances", minInstances);
        tx.setData("correlation_id", correlationId);
        tx.setTag("appdef.action", "modified");
        tx.setData("generation", appDefinition.getMetadata().getGeneration());

        LOGGER.info(formatLogMessage(correlationId, "Reconciling " + spec));

        try {
            // Verify ingress exists
            ISpan ingressSpan = Tracing.childSpan(tx, "appdef.verify_ingress", "Verify ingress exists");
            ingressSpan.setData("route_name", spec.getIngressname());

            if (!TheiaCloudIngressUtil.checkForExistingIngressAndAddOwnerReferencesIfMissing(client.kubernetes(),
                    client.namespace(), appDefinition, correlationId)) {
                LOGGER.error(formatLogMessage(correlationId, "Expected HTTPRoute '" + spec.getIngressname()
                        + "' for app definition '" + appDefName + "' does not exist. Abort handling."));
                ingressSpan.setTag("outcome", "not_found");
                Tracing.finish(ingressSpan, SpanStatus.NOT_FOUND);
                tx.setTag("error.reason", "ingress_not_found");
                tx.setTag("outcome", "failure");
                Tracing.finish(tx, SpanStatus.NOT_FOUND);
                return false;
            }
            Tracing.finishSuccess(ingressSpan);

            // Reconcile pool to target instance count
            ISpan reconcileSpan = Tracing.childSpan(tx, "appdef.reconcile_pool", "Reconcile pool to target");
            reconcileSpan.setData("target_instances", minInstances);
            reconcileSpan.setTag("pool.operation", "reconcile");

            boolean success = pool.reconcile(appDefinition, minInstances, correlationId);

            reconcileSpan.setTag("outcome", success ? "success" : "failure");
            Tracing.finish(reconcileSpan, success ? SpanStatus.OK : SpanStatus.INTERNAL_ERROR);
            tx.setTag("outcome", success ? "success" : "failure");
            Tracing.finish(tx, success ? SpanStatus.OK : SpanStatus.INTERNAL_ERROR);
            return success;

        } catch (Exception e) {
            Tracing.finishError(tx, e);
            throw e;
        }
    }
}
