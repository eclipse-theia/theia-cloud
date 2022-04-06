FROM node:12-bullseye as build-stage
RUN apt-get update && apt-get install -y libxkbfile-dev libsecret-1-dev
WORKDIR /home/theia
ADD package.json ./package.json
RUN yarn --pure-lockfile && \
    NODE_OPTIONS="--max_old_space_size=4096" yarn theia build --mode development && \
    yarn theia download:plugins && \
    yarn --production && \
    yarn autoclean --init && \
    echo *.ts >> .yarnclean && \
    echo *.ts.map >> .yarnclean && \
    echo *.spec.* >> .yarnclean && \
    yarn autoclean --force && \
    yarn cache clean

FROM node:12-bullseye-slim as production-stage
RUN adduser --system --group theia
RUN chmod g+rw /home && \
    mkdir -p /home/project && \
    chown -R theia:theia /home/theia && \
    chown -R theia:theia /home/project;
RUN apt-get update && apt-get install -y wget apt-transport-https && \
    wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | tee /usr/share/keyrings/adoptium.asc && \
    echo "deb [signed-by=/usr/share/keyrings/adoptium.asc] https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list && \
    apt-get update && apt-get install -y git openssh-client openssh-server bash libsecret-1-0 temurin-17-jdk maven clangd-11 cmake build-essential && \
    apt-get purge -y wget && \
    apt-get clean && \
    update-alternatives --install /usr/bin/clangd clangd /usr/bin/clangd-11 100
ENV HOME /home/theia
ENV THEIA_MINI_BROWSER_HOST_PATTERN {{hostname}}
ENV THEIA_WEBVIEW_ENDPOINT {{hostname}}
WORKDIR /home/theia
COPY --from=build-stage --chown=theia:theia /home/theia /home/theia
COPY --chown=theia:theia project /home/project
COPY --chown=theia:theia settings.json /home/theia/.theia/settings.json
EXPOSE 3000
ENV SHELL=/bin/bash \
    THEIA_DEFAULT_PLUGINS=local-dir:/home/theia/plugins
ENV USE_LOCAL_GIT true
USER theia

# Build projects once
RUN mvn clean verify -f /home/project/java/pom.xml && \
    cd /home/project/web && \
    npm install && npm run build && \
    cd /home/theia

ENTRYPOINT [ "node", "/home/theia/src-gen/backend/main.js" ]
CMD [ "/home/project", "--hostname=0.0.0.0" ]