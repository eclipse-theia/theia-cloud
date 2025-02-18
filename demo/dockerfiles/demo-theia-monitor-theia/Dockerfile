FROM node:20-bookworm AS build-stage
RUN apt-get update && apt-get install -y libxkbfile-dev libsecret-1-dev

WORKDIR /home/theia
COPY theia ./

RUN yarn --frozen-lockfile && \
    NODE_OPTIONS="--max_old_space_size=4096" yarn build && \
    yarn --production --frozen-lockfile && \
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
ENV HOME=/home/theia
ENV THEIA_MINI_BROWSER_HOST_PATTERN={{hostname}}
ENV THEIA_WEBVIEW_ENDPOINT={{hostname}}
WORKDIR /home/theia
COPY --from=build-stage --chown=theia:theia /home/theia /home/theia
EXPOSE 3000
ENV SHELL=/bin/bash
ENV USE_LOCAL_GIT=true
USER theia

WORKDIR /home/theia
ENTRYPOINT [ "node", "/home/theia/examples/browser-app/src-gen/backend/main.js" ]
CMD [ "/home/project", "--hostname=0.0.0.0" ]