FROM eclipse-temurin:21-jdk AS builder
# Accept APT_HTTP_PROXY build-arg for caching
ARG APT_HTTP_PROXY
# Configure apt proxy if provided (hybrid: works on ARC and GitHub runners)
RUN if [ -n "$APT_HTTP_PROXY" ]; then \
      echo "Acquire::http::Proxy \"$APT_HTTP_PROXY\";" > /etc/apt/apt.conf.d/01proxy; \
      # Force HTTP repositories when using proxy (Noble defaults to HTTPS which apt-cacher-ng cannot cache)
      sed -i 's|https://|http://|g' /etc/apt/sources.list.d/*.sources 2>/dev/null || true; \
      sed -i 's|https://|http://|g' /etc/apt/sources.list 2>/dev/null || true; \
      echo "Using APT Proxy: $APT_HTTP_PROXY with HTTP repositories"; \
    else \
      echo "No APT Proxy set, using direct connection"; \
    fi
RUN apt-get update && apt-get install -y maven
WORKDIR /service
COPY java/common ./common
COPY java/service ./service
RUN --mount=type=secret,id=SENTRY_AUTH_TOKEN \
    SENTRY_AUTH_TOKEN_VALUE="$(cat /run/secrets/SENTRY_AUTH_TOKEN 2>/dev/null || true)" && \
    MAVEN_SENTRY_ARGS="" && \
    if [ -n "$SENTRY_AUTH_TOKEN_VALUE" ]; then \
      export SENTRY_AUTH_TOKEN="$SENTRY_AUTH_TOKEN_VALUE" && \
      MAVEN_SENTRY_ARGS="-Dsentry=true" && \
      echo "Sentry build integration enabled"; \
    else \
      echo "No SENTRY_AUTH_TOKEN provided, skipping Sentry build integration"; \
    fi && \
    cd /service/common/maven-conf && \
    mvn clean install --no-transfer-progress $MAVEN_SENTRY_ARGS && \
    cd /service/common/org.eclipse.theia.cloud.common && \
    mvn clean install --no-transfer-progress $MAVEN_SENTRY_ARGS && \
    cd /service/service/org.eclipse.theia.cloud.service && \
    mvn clean package -Dmaven.test.skip=true -Dquarkus.package.type=uber-jar --no-transfer-progress $MAVEN_SENTRY_ARGS

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl unzip
WORKDIR /service
COPY --from=builder /service/service/org.eclipse.theia.cloud.service/target/service-1.2.0-SNAPSHOT-runner.jar .
RUN curl -fsSL -o sentry-agent.zip https://github.com/getsentry/sentry-java/releases/download/8.29.0/sentry-opentelemetry-agent-8.29.0.zip && \
    unzip -j sentry-agent.zip -d . && \
    rm sentry-agent.zip

ENV SERVICE_PORT=8081

ENV APPID=default-app-id

ENV KEYCLOAK_ENABLE=true
# Keycloak config
ENV KEYCLOAK_SERVERURL=https://keycloak.url/auth/realms/TheiaCloud
ENV KEYCLOAK_CLIENTID=theia-cloud
ENV KEYCLOAK_CLIENTSECRET=publicbutoauth2proxywantsasecret
ENV KEYCLOAK_ADMIN_GROUP=theia-cloud/admin

# If set to true, enables Sentry Java agent flags.
ENV SENTRY_ENABLE=false
# Optional path for a mounted Sentry properties file.
# No file is baked into the image at this path. If it is unset or missing,
# Sentry starts without `-Dsentry.properties.file` and falls back to defaults/env vars.
ENV SENTRY_PROPERTIES_FILE=/service/sentry.properties

# Use SERVICE_AUTH_TOKEN if set, otherwise fall back to APPID for backwards compatibility
RUN cat > /entrypoint.sh <<'EOF_ENTRYPOINT' && chmod +x /entrypoint.sh
#!/bin/sh
set -eu

JAVA_OPTS=""
if [ "${SENTRY_ENABLE:-false}" = "true" ]; then
  JAVA_OPTS="$JAVA_OPTS -javaagent:/service/sentry-opentelemetry-agent-8.29.0.jar"
  if [ -n "${SENTRY_PROPERTIES_FILE:-}" ] && [ -f "${SENTRY_PROPERTIES_FILE}" ]; then
    JAVA_OPTS="$JAVA_OPTS -Dsentry.properties.file=${SENTRY_PROPERTIES_FILE}"
  fi
fi

SERVICE_AUTH_TOKEN_EFFECTIVE="${SERVICE_AUTH_TOKEN:-${APPID}}"

exec java $JAVA_OPTS \
  -Dtheia.cloud.service.auth.token="${SERVICE_AUTH_TOKEN_EFFECTIVE}" \
  -Dquarkus.http.port="${SERVICE_PORT}" \
  -Dtheia.cloud.auth.admin.group="${KEYCLOAK_ADMIN_GROUP}" \
  -Dtheia.cloud.use.keycloak="${KEYCLOAK_ENABLE}" \
  -Dquarkus.oidc.auth-server-url="${KEYCLOAK_SERVERURL}" \
  -Dquarkus.oidc.client-id="${KEYCLOAK_CLIENTID}" \
  -Dquarkus.oidc.credentials.secret="${KEYCLOAK_CLIENTSECRET}" \
  -jar ./service-1.2.0-SNAPSHOT-runner.jar \
  "$@"
EOF_ENTRYPOINT

ENTRYPOINT ["/entrypoint.sh"]
