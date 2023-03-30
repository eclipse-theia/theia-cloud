#!/bin/bash
eval `ssh-agent`
mkdir $HOME/.ssh
touch $HOME/.ssh/known_hosts
[ -e /etc/theia-cloud-ssh/id_theiacloud ] && { sleep 1; echo $GIT_PROMPT1; } | script -q /dev/null -c 'ssh-add /etc/theia-cloud-ssh/id_theiacloud'
python git-init.py "$@"