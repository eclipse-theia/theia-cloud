# Git Init Container

## Scenarios

- HTTP(S)
  - No Auth
  - Ask for password only
  - Ask for username and password
- SSH
  - No Auth
  - Ask for password

## Testing

### Build init container

```bash
docker build -t theiacloud/theia-cloud-git-init:local -f dockerfiles/git-init/Dockerfile .
```

### Generate Test SSH Key Pair

```bash
# don't save in ~/.ssh/... but e.g. in ~/tmp/ssh/id_theiacloud
ssh-keygen -t ed25519 -C "Test TC Git Init SSH Keypair"

# check if key is added already
ssh-add -L

# add the key if necessary
ssh-add ~/tmp/ssh/id_theiacloud
```

### Test Checkout with container

Please also play with wrong password or public SSH Keys that are not (yet) added to the repository to get the known error cases.

```bash
# Adjust URLs and Password/PATs below
# keep spaces in front to avoid command being added to bash history
 export HTTP_PUBLIC=https://github.com/eclipsesource/theia-cloud.git
 export HTTP_PRIVATE=https://gitlab.eclipse.org/username/my.repository.git
 export HTTP_PRIVATE_WITH_USERNAME=https://username@gitlab.eclipse.org/username/my.repository.git
 export HTTP_PRIVATE_WITH_USERNAME_AND_PASSWORD=https://username:pat@gitlab.eclipse.org/username/my.repository.git
 export HTTP_USERNAME=username
 export HTTP_PASSWORD=pat
 export SSH_PASSWORD=sshpw
 export SSH_REPO="git@gitlab.eclipse.org:username/my.repository.git"
 export BRANCH=maintenance_1_1_x

# HTTPS Public
docker run --rm theiacloud/theia-cloud-git-init:local "$HTTP_PUBLIC" "/tmp/my-repo" "$BRANCH"

# For HTTPS auth with PATs as password a lot of providers accept any username
# HTTPS Private
docker run --env GIT_PROMPT1=$HTTP_USERNAME --env GIT_PROMPT2=$HTTP_PASSWORD --rm theiacloud/theia-cloud-git-init:local "$HTTP_PRIVATE" "/tmp/my-repo" "$BRANCH"

# HTTPS Private with Username
docker run --env GIT_PROMPT1=$HTTP_PASSWORD --rm theiacloud/theia-cloud-git-init:local "$HTTP_PRIVATE_WITH_USERNAME" "/tmp/my-repo" "$BRANCH"

# HTTPS Private with Username and Password
docker run --rm theiacloud/theia-cloud-git-init:local "$HTTP_PRIVATE_WITH_USERNAME_AND_PASSWORD" "/tmp/my-repo" "$BRANCH"

# SSH (the expected keyname is id_theiacloud ! With a different naming pattern this command will fail. Rename/Create a copy of you keyname if necessary)
docker run --env GIT_PROMPT1=$SSH_PASSWORD -v ~/tmp/ssh/:/etc/theia-cloud-ssh --rm theiacloud/theia-cloud-git-init:local "$SSH_REPO" "/tmp/my-repo" "$BRANCH"
```

### Create Kubernetes Resources

#### Workspace

If testing on Minikube also mount a directory with expected user permissions: `minikube mount --uid 101 --gid 101 ~/tmp/minikube:/tmp/hostpath-provisioner/theia-cloud`

You might have to adjust your firewall (temporarily).

With below Sessions, the Workspace will be mounted inside the `persisted` subdirectory in the workspace.

```yaml
apiVersion: theia.cloud/v3beta
kind: Workspace
metadata:
  name: ws-asdfghjkl-theia-cloud-demo-foo-theia-cloud-io
  namespace: theiacloud
spec:
  name: ws-asdfghjkl-theia-cloud-demo-foo-theia-cloud-io
  user: foo@theia-cloud.io
```

#### Secret for HTTP(S) auth

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: foo-theiacloud-io-basic-auth
  namespace: theiacloud
  labels:
    theiaCloudInit: git
  annotations:
    theiaCloudUser: foo@theia-cloud.io
type: kubernetes.io/basic-auth
stringData:
  username: username
  password: pat
```

#### Example Session for HTTP(S) auth

```yaml
apiVersion: theia.cloud/v7beta
kind: Session
metadata:
  name: ws-asdfghjkl-theia-cloud-demo-foo-theia-cloud-io-session
  namespace: theiacloud
spec:
  appDefinition: theia-cloud-demo
  envVars: {}
  envVarsFromConfigMaps: []
  envVarsFromSecrets: []
  name: ws-asdfghjkl-theia-cloud-demo-foo-theia-cloud-io-session
  user: foo@theia-cloud.io
  workspace: ws-asdfghjkl-theia-cloud-demo-foo-theia-cloud-io
  sessionSecret: 3e68605f-0c6d-4ae5-9816-738f15d34fc9
  initOperations:
    - id: git
      arguments:
        - https://gitlab.eclipse.org/username/my.repository.git
        - maintenance_1_1_x
        - foo-theiacloud-io-basic-auth
```

#### Secrets for SSH auth

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: foo-theiacloud-io-ssh-auth
  namespace: theiacloud
  labels:
    theiaCloudInit: git
  annotations:
    theiaCloudUser: foo@theia-cloud.io
type: kubernetes.io/ssh-auth
stringData:
  ssh-privatekey: |
    -----BEGIN OPENSSH PRIVATE KEY-----
    b3B...
  password: sshpw
```

#### Example Session for SSH auth

```yaml
apiVersion: theia.cloud/v7beta
kind: Session
metadata:
  name: ws-asdfghjkl-theia-cloud-demo-foo-theia-cloud-io-session
  namespace: theiacloud
spec:
  appDefinition: theia-cloud-demo
  envVars: {}
  envVarsFromConfigMaps: []
  envVarsFromSecrets: []
  name: ws-asdfghjkl-theia-cloud-demo-foo-theia-cloud-io-session
  user: foo@theia-cloud.io
  workspace: ws-asdfghjkl-theia-cloud-demo-foo-theia-cloud-io
  sessionSecret: 3e68605f-0c6d-4ae5-9816-738f15d34fc9
  initOperations:
    - id: git
      arguments:
        - git@gitlab.eclipse.org:username/my.repository.git
        - maintenance_1_1_x
        - foo-theiacloud-io-ssh-auth
```
