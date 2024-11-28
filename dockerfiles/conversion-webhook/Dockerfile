FROM eclipse-temurin:17-jdk AS builder
RUN apt-get update && apt-get install -y maven
WORKDIR /conversion
COPY java/common ./common
COPY java/conversion ./conversion
RUN cd /conversion/common/maven-conf && \
    mvn clean install --no-transfer-progress && \
    cd /conversion/common/org.eclipse.theia.cloud.common && \
    mvn clean install --no-transfer-progress&& \
    cd /conversion/conversion/org.eclipse.theia.cloud.conversion && \
    mvn clean package -Dmaven.test.skip=true -Dquarkus.package.type=uber-jar --no-transfer-progress

FROM eclipse-temurin:17-jre-alpine
WORKDIR /conversion
COPY --from=builder /conversion/conversion/org.eclipse.theia.cloud.conversion/target/conversion-webhook-1.0.0-runner.jar .

ENTRYPOINT java -jar ./conversion-webhook-1.0.0-runner.jar
CMD [ "" ]