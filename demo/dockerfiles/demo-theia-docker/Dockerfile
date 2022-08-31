FROM node:16-bullseye as build-stage
RUN apt-get update && apt-get install -y libxkbfile-dev libsecret-1-dev
WORKDIR /home/theia
RUN git clone https://github.com/eclipse-theia/theia-blueprint.git && \
    cd theia-blueprint && \
    git checkout jf/docker && \
    yarn --pure-lockfile && \
    yarn --production && \
    yarn autoclean --init && \
    echo *.ts >> .yarnclean && \
    echo *.ts.map >> .yarnclean && \
    echo *.spec.* >> .yarnclean && \
    yarn autoclean --force && \
    yarn cache clean

FROM node:16-bullseye-slim as production-stage
RUN adduser --system --group theia
RUN chmod g+rw /home && \
    mkdir -p /home/project && \
    chown -R theia:theia /home/theia && \
    chown -R theia:theia /home/project;
RUN apt-get update && apt-get install -y wget apt-transport-https && \
    wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | tee /usr/share/keyrings/adoptium.asc && \
    echo "deb [signed-by=/usr/share/keyrings/adoptium.asc] https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list && \
    apt-get update && apt-get install -y git openssh-client openssh-server bash libsecret-1-0 temurin-17-jdk maven && \
    apt-get purge -y wget && \
    apt-get clean
ENV HOME /home/theia
ENV THEIA_MINI_BROWSER_HOST_PATTERN {{hostname}}
ENV THEIA_WEBVIEW_ENDPOINT {{hostname}}
WORKDIR /home/theia
COPY --from=build-stage --chown=theia:theia /home/theia /home/theia
COPY --chown=theia:theia project /home/project
COPY --chown=theia:theia settings.json /home/theia/theia-blueprint/applications/docker/.theia/settings.json
COPY --chown=theia:theia settings.json /home/theia/theia-blueprint/applications/docker/.theia/settings.json
EXPOSE 3000
ENV SHELL=/bin/bash \
    THEIA_DEFAULT_PLUGINS=local-dir:/home/theia/theia-blueprint/applications/docker/plugins
ENV USE_LOCAL_GIT true
USER theia

# Build projects once
RUN mvn clean verify -f /home/project/java/pom.xml && \
    cd /home/project/web && \
    npm install && npm run build && \
    cd /home/theia

WORKDIR /home/theia/theia-blueprint/applications/docker
ENTRYPOINT [ "node", "/home/theia/theia-blueprint/applications/docker/src-gen/backend/main.js" ]
CMD [ "/home/project", "--hostname=0.0.0.0" ]