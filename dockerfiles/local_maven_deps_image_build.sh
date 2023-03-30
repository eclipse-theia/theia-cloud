#!/bin/sh
# builds a scratch image using packages installed on your system
# Builds "theia-cloud-local-maven-deps" image from which you can copy /root/.m2 folder directly before building
# To not have to download the whole world everytime you change a minor thing
touch ~/.m2/Dockerfile.temp.theia
dockerfileStr="FROM scratch
COPY ./repository /root/.m2/repository
"

printf "$dockerfileStr" > ~/.m2/Dockerfile.temp.theia
(cd ~/.m2 && docker build -t theia-cloud-local-maven-deps . -f Dockerfile.temp.theia)
rm ~/.m2/Dockerfile.temp.theia
