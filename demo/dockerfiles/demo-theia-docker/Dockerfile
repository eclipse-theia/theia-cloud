FROM node:12-bullseye as build-stage
RUN apt-get update && apt-get install -y libxkbfile-dev libsecret-1-dev
WORKDIR /home/theia
ADD package.json ./package.json
RUN yarn --pure-lockfile && \
    NODE_OPTIONS="--max_old_space_size=4096" yarn theia build && \
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
RUN apt-get update && apt-get install -y git openssh-client openssh-server bash libsecret-1-0 default-jdk maven && \
    apt-get clean
ENV HOME /home/theia
WORKDIR /home/theia
COPY --from=build-stage --chown=theia:theia /home/theia /home/theia
COPY --chown=theia:theia project /home/project
EXPOSE 3000
ENV SHELL=/bin/bash \
    THEIA_DEFAULT_PLUGINS=local-dir:/home/theia/plugins
ENV USE_LOCAL_GIT true
USER theia
RUN mvn clean verify -f /home/project/java/pom.xml && \
    yarn --cwd /home/project/typescript install && \
    yarn --cwd /home/project/typescript build
ENTRYPOINT [ "node", "/home/theia/src-gen/backend/main.js" ]
CMD [ "/home/project", "--hostname=0.0.0.0" ]