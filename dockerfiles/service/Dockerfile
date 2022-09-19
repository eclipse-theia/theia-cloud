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
ENTRYPOINT java -Dtheia.cloud.app.id=${APPID} \
    -Dquarkus.http.port=${SERVICE_PORT} \
    -jar ./service-0.7.0-SNAPSHOT-runner.jar
CMD [ "" ]