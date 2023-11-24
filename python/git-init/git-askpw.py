#!/usr/bin/env python3
import os

path = "/tmp/theia-cloud-askpw"

if os.path.isfile(path):
    prompt2 = os.environ['GIT_PROMPT2']
    print(prompt2)
else:
    prompt1 = os.environ['GIT_PROMPT1']
    print(prompt1)
    os.mknod(path)
