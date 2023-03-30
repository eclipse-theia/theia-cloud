FROM debian:11-slim

RUN apt update && \
    apt install python git -y && \
    apt clean

WORKDIR /tmp
COPY python/git-init/entrypoint.sh .
COPY python/git-init/ssh-keyscan.sh .
COPY python/git-init/git-init.py .
COPY python/git-init/git-askpw.py .

ENV GIT_ASKPASS=/tmp/git-askpw.py

ENTRYPOINT [ "/tmp/entrypoint.sh" ]
CMD ["-h"]