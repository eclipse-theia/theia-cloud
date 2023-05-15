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
```

### Test Checkout with container

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

# HTTPS Private
docker run --env GIT_PROMPT1=$HTTP_USERNAME --env GIT_PROMPT2=$HTTP_PASSWORD --rm theiacloud/theia-cloud-git-init:local "$HTTP_PRIVATE" "/tmp/my-repo" "$BRANCH"

# HTTPS Private with Username
docker run --env GIT_PROMPT1=$HTTP_PASSWORD --rm theiacloud/theia-cloud-git-init:local "$HTTP_PRIVATE_WITH_USERNAME" "/tmp/my-repo" "$BRANCH"

# HTTPS Private with Username and Password
docker run --rm theiacloud/theia-cloud-git-init:local "$HTTP_PRIVATE_WITH_USERNAME_AND_PASSWORD" "/tmp/my-repo" "$BRANCH"

# SSH
docker run --env GIT_PROMPT1=$SSH_PASSWORD -v ~/tmp/ssh/:/etc/theia-cloud-ssh --rm theiacloud/theia-cloud-git-init:local "$SSH_REPO" "/tmp/my-repo" "$BRANCH"
```

### Create Kubernetes Resources

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

```yaml
apiVersion: theia.cloud/v5beta
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
