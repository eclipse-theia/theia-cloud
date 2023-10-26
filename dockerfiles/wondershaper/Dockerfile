FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y iproute2 wondershaper && \
    apt-get clean
# kilobits per second
ENV DOWNLINK 4000
ENV UPLINK 4000
ENTRYPOINT wondershaper eth0 ${DOWNLINK} ${UPLINK} || true
CMD [ "" ]