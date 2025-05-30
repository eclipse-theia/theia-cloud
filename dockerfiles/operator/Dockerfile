FROM eclipse-temurin:21-jdk AS builder
RUN apt-get update && apt-get install -y maven
WORKDIR /operator
COPY java/common ./common
COPY java/operator ./operator
RUN cd /operator/common/maven-conf && \
    mvn clean install --no-transfer-progress && \
    cd /operator/common/org.eclipse.theia.cloud.common && \
    mvn clean install --no-transfer-progress && \
    cd /operator/operator/org.eclipse.theia.cloud.operator && \
    mvn clean install --no-transfer-progress && \
    cd /operator/operator/org.eclipse.theia.cloud.defaultoperator && \
    mvn clean verify --no-transfer-progress

FROM eclipse-temurin:21-jre-alpine
RUN mkdir /templates
WORKDIR /log-config
COPY java/operator/org.eclipse.theia.cloud.defaultoperator/log4j2.xml .
WORKDIR /operator
COPY --from=builder /operator/operator/org.eclipse.theia.cloud.defaultoperator/target/defaultoperator-1.1.0-SNAPSHOT-jar-with-dependencies.jar .
# to get more debug information from the kubernetes client itself, add -Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG below
ENTRYPOINT [ "java", "-Dlog4j2.configurationFile=/log-config/log4j2.xml", "-jar", "./defaultoperator-1.1.0-SNAPSHOT-jar-with-dependencies.jar" ]
CMD [ "" ]
