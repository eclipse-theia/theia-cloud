FROM ghcr.io/eclipse-theia/theia-ide/theia-ide:1.56.1 as production-stage

ENV THEIA_MINI_BROWSER_HOST_PATTERN {{hostname}}
ENV THEIA_WEBVIEW_ENDPOINT {{hostname}}

# Copy project
COPY --chown=theia:theia project /home/project
COPY --chown=theia:theia settings.json /home/theia/theia-blueprint/applications/browser/.theia/settings.json

# Build projects once
RUN mvn clean verify -f /home/project/java/pom.xml && \
    cd /home/project/web && \
    npm install && npm run build && \
    cd /home/theia
