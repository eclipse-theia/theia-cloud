#!/bin/bash
USERID=$(id -u)

if id "$USERID" &>/dev/null; then
    echo 'Existing user'
else
    echo 'Setup user for id'
    # add an entry to /etc/passwd for the user
    echo user:x:$USERID:$USERID:user:/user:/bin/bash >> /etc/passwd
    export HOME=/user
fi

# start SSH agent
eval `ssh-agent`

# prepare required directories and import ssh key, if available
mkdir -p $HOME/.ssh
touch $HOME/.ssh/known_hosts
[ -e /etc/theia-cloud-ssh/id_theiacloud ] && { sleep 1; echo $GIT_PROMPT1; } | script -q /dev/null -c 'ssh-add /etc/theia-cloud-ssh/id_theiacloud'

# hand over to clone script
python3 git-init.py "$@"