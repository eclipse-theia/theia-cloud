FROM maven:3.8.4-openjdk-11 AS builder
WORKDIR /service
COPY java/common ./common
COPY java/service ./service
RUN cd /service/common/maven-conf && \
    mvn clean install && \
    cd /service/common/org.eclipse.theia.cloud.common && \
    mvn clean install && \
    cd /service/service/org.eclipse.theia.cloud.service && \
    mvn clean package -Dquarkus.package.type=uber-jar

FROM openjdk:11-jre-slim-buster
WORKDIR /service
COPY --from=builder /service/service/org.eclipse.theia.cloud.service/target/service-0.7.0-SNAPSHOT-runner.jar .
ENV APPID default-app-id
ENV SERVICE_PORT 8081


ENV KEYCLOAK_ENABLE true
# Keycloak config
ENV KEYCLOAK_SERVERURL https://keycloak.url/auth/realms/TheiaCloud
ENV KEYCLOAK_CLIENTID theia-cloud
ENV KEYCLOAK_CLIENTSECRET publicbutoauth2proxywantsasecret

ENTRYPOINT java -Dtheia.cloud.app.id=${APPID} \
    -Dquarkus.http.port=${SERVICE_PORT} \
    -Dtheia.cloud.use.keycloak=${KEYCLOAK_ENABLE} \
    -Dquarkus.oidc.auth-server-url=${KEYCLOAK_SERVERURL} \
    -Dquarkus.oidc.client-id=${KEYCLOAK_CLIENTID} \
    -Dquarkus.oidc.credentials.secret=${KEYCLOAK_CLIENTSECRET} \
    -jar ./service-0.7.0-SNAPSHOT-runner.jar
CMD [ "" ]