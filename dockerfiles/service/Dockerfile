FROM eclipse-temurin:21-jdk AS builder
RUN apt-get update && apt-get install -y maven
WORKDIR /service
COPY java/common ./common
COPY java/service ./service
RUN cd /service/common/maven-conf && \
    mvn clean install --no-transfer-progress && \
    cd /service/common/org.eclipse.theia.cloud.common && \
    mvn clean install --no-transfer-progress&& \
    cd /service/service/org.eclipse.theia.cloud.service && \
    mvn clean package -Dmaven.test.skip=true -Dquarkus.package.type=uber-jar --no-transfer-progress

FROM eclipse-temurin:21-jre-alpine
WORKDIR /service
COPY --from=builder /service/service/org.eclipse.theia.cloud.service/target/service-1.1.0-SNAPSHOT-runner.jar .
ENV APPID default-app-id
ENV SERVICE_PORT 8081


ENV KEYCLOAK_ENABLE true
# Keycloak config
ENV KEYCLOAK_SERVERURL https://keycloak.url/auth/realms/TheiaCloud
ENV KEYCLOAK_CLIENTID theia-cloud
ENV KEYCLOAK_CLIENTSECRET publicbutoauth2proxywantsasecret
ENV KEYCLOAK_ADMIN_GROUP theia-cloud/admin

ENTRYPOINT java -Dtheia.cloud.app.id=${APPID} \
    -Dquarkus.http.port=${SERVICE_PORT} \
    -Dtheia.cloud.auth.admin.group=${KEYCLOAK_ADMIN_GROUP} \
    -Dtheia.cloud.use.keycloak=${KEYCLOAK_ENABLE} \
    -Dquarkus.oidc.auth-server-url=${KEYCLOAK_SERVERURL} \
    -Dquarkus.oidc.client-id=${KEYCLOAK_CLIENTID} \
    -Dquarkus.oidc.credentials.secret=${KEYCLOAK_CLIENTSECRET} \
    -jar ./service-1.1.0-SNAPSHOT-runner.jar
CMD [ "" ]