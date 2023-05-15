#!/usr/bin/env python

import argparse
import subprocess
import os
import sys

debugLogging = False
sshKey = "/etc/theia-cloud-ssh/id_theiacloud"
NL = "\n"


def runProcess(args):
    process = subprocess.Popen(
        args, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    stdout, stderr = process.communicate()
    process.wait()
    out = stdout.decode('ascii')
    if len(out) > 0:
        sys.stdout.write(out + NL)
    if process.returncode != 0:
        sys.stderr.write(stderr.decode('ascii') + NL)
    return process.returncode

def getHostname(repository):
    # remove protocol, if any
    split = repository.split("://", 1)
    if len(split) == 1:
        repository = split[0]
    else:
        repository = split[1]
    if debugLogging:
        sys.stdout.write("getHostname 1: " + repository + NL)

    # remove path, if any
    split = repository.split("/", 1)
    repository = split[0]
    if debugLogging:
        sys.stdout.write("getHostname 2: " + repository + NL)

    # remove user information, if any
    split = repository.split("@", 1)
    if len(split) == 1:
        repository = split[0]
    else:
        repository = split[1]
    if debugLogging:
        sys.stdout.write("getHostname 3: " + repository + NL)

    # remove trailing information, if any
    split = repository.split(":", 1)
    repository = split[0]
    if debugLogging:
        sys.stdout.write("getHostname 4: " + repository + NL)

    return repository

parser = argparse.ArgumentParser()
parser.add_argument("repository", help="The repository URL", type=str)
parser.add_argument("directory", help="The directory to clone into", type=str)
parser.add_argument("checkout", help="The branch/commit id/tag to checkout", type=str)
args = parser.parse_args()

# Set up git credential helper
code = runProcess(["git", "config", "--global", "credential.helper", "store"])
if code != 0:
    exit(code)

# Check if SSH key is available, if so prepare clone with SSH
if os.path.isfile(sshKey):
    # Add know host
    code = runProcess(["/tmp/ssh-keyscan.sh", getHostname(args.repository)])
    if code != 0:
        exit(code)

    if debugLogging:
        runProcess(["ssh-add", "-l"])
        runProcess(["cat", "/root/.ssh/known_hosts"])

# Clone repository
code = runProcess(["git", "clone", args.repository, args.directory])
if code != 0:
    exit(code)

if debugLogging:
    runProcess(["ls", "-al", args.directory])

# Checkout
code = runProcess(["git", "-C", args.directory, "checkout", args.checkout])
if code != 0:
    exit(code)

if debugLogging:
    runProcess(["git", "-C", args.directory, "status"])
