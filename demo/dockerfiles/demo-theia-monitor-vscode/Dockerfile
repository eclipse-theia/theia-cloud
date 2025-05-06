FROM node:18-bullseye-slim as build-stage

# Copy and unzip the vsix file
RUN apt-get update && apt-get install -y unzip && rm -rf /var/lib/apt/lists/*
COPY theia-cloud-monitor-1.1.0-next.vsix /tmp/theia-cloud-monitor.vsix
RUN mkdir /tmp/extracted && unzip /tmp/theia-cloud-monitor.vsix -d /tmp/extracted

FROM theiacloud/theia-cloud-demo:1.1.0-next as production-stage

COPY --chown=theia:theia --from=build-stage /tmp/extracted /home/theia/plugins