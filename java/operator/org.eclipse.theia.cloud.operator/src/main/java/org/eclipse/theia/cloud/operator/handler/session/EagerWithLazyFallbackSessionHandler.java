package org.eclipse.theia.cloud.operator.handler.session;

import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.theia.cloud.common.k8s.client.TheiaCloudClient;
import org.eclipse.theia.cloud.common.k8s.resource.session.Session;
import org.eclipse.theia.cloud.common.tracing.Tracing;

import com.google.inject.Inject;

import io.sentry.ISpan;
import io.sentry.SpanStatus;

/**
 * Tries to handle a session with {@link EagerSessionHandler} first. If there is
 * no prewarmed capacity left, falls back
 * to {@link LazySessionHandler}.
 */
public class EagerWithLazyFallbackSessionHandler implements SessionHandler {

    /**
     * Value indicating session was started lazily after eager capacity was
     * exhausted.
     */
    public static final String SESSION_START_STRATEGY_LAZY_FALLBACK = "lazy-fallback";

    private static final Logger LOGGER = LogManager.getLogger(EagerWithLazyFallbackSessionHandler.class);

    @Inject
    private EagerSessionHandler eager;

    @Inject
    private LazySessionHandler lazy;

    @Inject
    private TheiaCloudClient client;

    @Override
    public boolean sessionAdded(Session session, String correlationId, ISpan span) {
        // Try eager start first
        ISpan eagerSpan = Tracing.childSpan(span, "session.eager_attempt", "Attempt eager session start");
        eagerSpan.setTag("eagersession.strategy", "eager");

        EagerSessionHandler.EagerSessionAddedOutcome eagerOutcome = eager.trySessionAdded(session, correlationId,
                eagerSpan);
        eagerSpan.setData("eager_outcome", eagerOutcome.name());

        if (eagerOutcome == EagerSessionHandler.EagerSessionAddedOutcome.HANDLED) {
            Tracing.finishSuccess(eagerSpan);
            span.setTag("session.strategy", "eager");
            return true;
        }

        if (eagerOutcome == EagerSessionHandler.EagerSessionAddedOutcome.ERROR) {
            eagerSpan.setTag("outcome", "error");
            Tracing.finish(eagerSpan, SpanStatus.INTERNAL_ERROR);
            span.setTag("session.strategy", "eager");
            return false;
        }

        // NO_CAPACITY - fall back to lazy
        eagerSpan.setTag("outcome", "no_capacity");
        Tracing.finish(eagerSpan, SpanStatus.RESOURCE_EXHAUSTED);
        span.setTag("fallback", "true");
        span.setTag("fallback.reason", "no_prewarmed_capacity");

        LOGGER.info(formatLogMessage(correlationId,
                "No prewarmed capacity left. Falling back to lazy session handling."));

        ISpan lazySpan = Tracing.childSpan(span, "session.lazy_fallback", "Fallback to lazy session start");
        lazySpan.setTag("session.strategy", "lazy-fallback");
        lazySpan.setTag("fallback.reason", "no_prewarmed_capacity");

        boolean lazyResult = lazy.sessionAdded(session, correlationId, lazySpan);

        if (lazyResult) {
            annotateSessionStrategy(session, correlationId, SESSION_START_STRATEGY_LAZY_FALLBACK);
            Tracing.finishSuccess(lazySpan);
            span.setTag("session.strategy", "lazy-fallback");
        } else {
            lazySpan.setTag("outcome", "failure");
            Tracing.finish(lazySpan, SpanStatus.INTERNAL_ERROR);
            span.setTag("session.strategy", "lazy-fallback");
        }

        return lazyResult;
    }

    @Override
    public boolean sessionDeleted(Session session, String correlationId, ISpan parentSpan) {
        String strategy = Optional.ofNullable(session.getMetadata()).map(m -> m.getAnnotations())
                .map(a -> a.get(EagerSessionHandler.SESSION_START_STRATEGY_ANNOTATION)).orElse("unknown");

        parentSpan.setTag("session.start_strategy", strategy);

        boolean result;
        if (EagerSessionHandler.SESSION_START_STRATEGY_EAGER.equals(strategy)) {
            ISpan span = Tracing.childSpan(parentSpan, "session.eager_cleanup", "Eager session cleanup");
            span.setTag("cleanup.type", "eager");
            result = eager.sessionDeleted(session, correlationId, span);
            span.setTag("outcome", result ? "success" : "failure");
            Tracing.finish(span, result ? SpanStatus.OK : SpanStatus.INTERNAL_ERROR);
        } else {
            ISpan span = Tracing.childSpan(parentSpan, "session.lazy_cleanup", "Lazy session cleanup");
            span.setTag("cleanup.type", "lazy");
            result = lazy.sessionDeleted(session, correlationId, span);
            span.setTag("outcome", result ? "success" : "failure");
            Tracing.finish(span, result ? SpanStatus.OK : SpanStatus.INTERNAL_ERROR);
        }

        return result;
    }

    private void annotateSessionStrategy(Session session, String correlationId, String strategy) {
        String name = session.getMetadata().getName();
        client.sessions().edit(correlationId, name, s -> {
            Map<String, String> annotations = s.getMetadata().getAnnotations();
            if (annotations == null) {
                annotations = new HashMap<>();
                s.getMetadata().setAnnotations(annotations);
            }
            annotations.put(EagerSessionHandler.SESSION_START_STRATEGY_ANNOTATION, strategy);
        });
    }
}
