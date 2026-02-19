package org.eclipse.theia.cloud.operator.handler.session;

import java.time.Instant;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.OperatorStatus;
import org.eclipse.theia.cloud.common.k8s.resource.ResourceStatus;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.k8s.resource.session.SessionStatus;

public final class SessionStatusUtil {

    public enum PreHandleResult {
        PROCEED, ALREADY_HANDLED, INTERRUPTED, PREVIOUS_ERROR
    }

    private SessionStatusUtil() {
    }

    /**
     * Evaluates the session status and returns the appropriate pre-handle result.
     *
     * @param session the session to evaluate
     * @param client the Kubernetes client
     * @param correlationId the correlation ID for tracking
     * @param logger the logger instance
     * @return the pre-handle result indicating how to proceed
     */
    public static PreHandleResult evaluateStatus(Session session, TheiaCloudClient client, String correlationId,
            Logger logger) {
        if (session == null) {
            throw new IllegalArgumentException("Session must not be null");
        }
        if (client == null) {
            throw new IllegalArgumentException("Client must not be null");
        }
        Optional<SessionStatus> status = Optional.ofNullable(session.getStatus());
        String operatorStatus = status.map(ResourceStatus::getOperatorStatus).orElse(OperatorStatus.NEW);

        if (OperatorStatus.HANDLED.equals(operatorStatus)) {
            return PreHandleResult.ALREADY_HANDLED;
        }
        if (OperatorStatus.HANDLING.equals(operatorStatus)) {
            markError(client, session, correlationId,
                    "Handling was unexpectedly interrupted. CorrelationId: " + correlationId);
            return PreHandleResult.INTERRUPTED;
        }
        if (OperatorStatus.ERROR.equals(operatorStatus)) {
            return PreHandleResult.PREVIOUS_ERROR;
        }
        return PreHandleResult.PROCEED;
    }

    /**
     * Marks the session as currently being handled.
     *
     * @param client the Kubernetes client
     * @param session the session to mark
     * @param correlationId the correlation ID for tracking
     */
    public static void markHandling(TheiaCloudClient client, Session session, String correlationId) {
        if (session == null) {
            throw new IllegalArgumentException("Session must not be null");
        }
        if (client == null) {
            throw new IllegalArgumentException("Client must not be null");
        }
        client.sessions().updateStatus(correlationId, session, s -> s.setOperatorStatus(OperatorStatus.HANDLING));
    }

    /**
     * Marks the session as successfully handled.
     *
     * @param client the Kubernetes client
     * @param session the session to mark
     * @param correlationId the correlation ID for tracking
     * @param message optional message to set on the session status
     */
    public static void markHandled(TheiaCloudClient client, Session session, String correlationId, String message) {
        if (session == null) {
            throw new IllegalArgumentException("Session must not be null");
        }
        if (client == null) {
            throw new IllegalArgumentException("Client must not be null");
        }
        client.sessions().updateStatus(correlationId, session, s -> {
            s.setOperatorStatus(OperatorStatus.HANDLED);
            if (message != null) {
                s.setOperatorMessage(message);
            }
            s.setLastActivity(Instant.now().toEpochMilli());
        });
    }

    /**
     * Marks the session with an error status.
     *
     * @param client the Kubernetes client
     * @param session the session to mark
     * @param correlationId the correlation ID for tracking
     * @param message the error message to set
     */
    public static void markError(TheiaCloudClient client, Session session, String correlationId, String message) {
        if (session == null) {
            throw new IllegalArgumentException("Session must not be null");
        }
        if (client == null) {
            throw new IllegalArgumentException("Client must not be null");
        }
        client.sessions().updateStatus(correlationId, session, s -> {
            s.setOperatorStatus(OperatorStatus.ERROR);
            s.setOperatorMessage(message);
        });
    }
}
