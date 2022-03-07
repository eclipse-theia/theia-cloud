FROM maven:3.8.4-openjdk-11 AS builder
WORKDIR /workspace
COPY java/common ./common
COPY java/workspace ./workspace
RUN cd /workspace/common/maven-conf && \
    mvn clean install && \
    cd /workspace/common/org.eclipse.theia.cloud.common && \
    mvn clean install && \
    cd /workspace/workspace/org.eclipse.theia.cloud.workspace && \
    mvn clean package -Dquarkus.package.type=uber-jar

FROM openjdk:11-jre-slim-buster
WORKDIR /workspace
COPY --from=builder /workspace/workspace/org.eclipse.theia.cloud.workspace/target/workspace-0.7.0-SNAPSHOT-runner.jar .
ENV APPID default-app-id
ENTRYPOINT java -Dtheia.cloud.app.id=${APPID} -jar ./workspace-0.7.0-SNAPSHOT-runner.jar
CMD [ "" ]