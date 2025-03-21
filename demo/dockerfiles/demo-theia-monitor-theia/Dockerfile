FROM node:20-bookworm AS build-stage
RUN apt-get update && apt-get install -y libxkbfile-dev libsecret-1-dev
WORKDIR /home/theia
ADD demo/dockerfiles/demo-theia-monitor-theia/package.json ./package.json
ADD demo/dockerfiles/demo-theia-monitor-theia/yarn.lock ./yarn.lock

## Link package from local
ADD theia ./theia
WORKDIR /home/theia/theia
RUN yarn --frozen-lockfile --ignore-scripts && yarn build:workspace @eclipse-theiacloud/monitor-theia
WORKDIR /home/theia

RUN yarn --pure-lockfile && \
    NODE_OPTIONS="--max_old_space_size=4096" yarn theia build && \
    yarn theia download:plugins && \
    yarn --production && \
    yarn autoclean --init && \
    sed -i '/__tests__\|test\|tests\|powered-test/ s/^/#/' .yarnclean && \
    echo *.ts >> .yarnclean && \
    echo *.ts.map >> .yarnclean && \
    echo *.spec.* >> .yarnclean && \
    yarn autoclean --force && \
    yarn cache clean

FROM node:20-bookworm-slim AS production-stage

# Use fixed user id 101 to guarantee it matches the app definition
RUN adduser --system --group --uid 101 theia
RUN chmod g+rw /home && \
    mkdir -p /home/project && \
    mkdir -p /home/theia && \
    chown -R theia:theia /home/theia && \
    chown -R theia:theia /home/project;
RUN apt-get update && apt-get install -y wget apt-transport-https && \
    wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | tee /usr/share/keyrings/adoptium.asc && \
    echo "deb [signed-by=/usr/share/keyrings/adoptium.asc] https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list && \
    apt-get update && apt-get install -y git openssh-client openssh-server bash libsecret-1-0 temurin-21-jdk maven && \
    apt-get purge -y wget && \
    apt-get clean
ENV HOME /home/theia
ENV THEIA_MINI_BROWSER_HOST_PATTERN {{hostname}}
ENV THEIA_WEBVIEW_ENDPOINT {{hostname}}
WORKDIR /home/theia
COPY --from=build-stage --chown=theia:theia /home/theia /home/theia
EXPOSE 3000
ENV SHELL=/bin/bash \
    THEIA_DEFAULT_PLUGINS=local-dir:/home/theia/plugins
ENV USE_LOCAL_GIT true
USER theia

WORKDIR /home/theia
ENTRYPOINT [ "node", "/home/theia/src-gen/backend/main.js" ]
CMD [ "/home/project", "--hostname=0.0.0.0" ]