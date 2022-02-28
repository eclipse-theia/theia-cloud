# Architecture Overview

The Theia.Cloud Operator listens for changes to custom resources inside the cluster. With those custom resources clients may trigger the creation/deletion/handling of workspaces. The Operator is responsible for handling all things that are related to the Kubernetes-Resources for workspaces.

## Overall Overview

![Theia.Cloud Diagram](theia.cloud.png "Theia.Cloud")

## Operator Overview


![Operator Diagram](operator.png "Operator")

## Custom Resources

### Template

A template describing a specific type of a workspace.

|Property|Type|Used for|
|---|---|---|
|name|string|Used to identify the template|
|image|string|The container image launched in every workspace of this template type|
|port|integer|port to expose|
|instances|integer|Max number of instances. Currently will be eagerly launched even without workspace requests (will change)|
|host|string|Domain where the workspaces will be available|

### Workspace

A concrete workspace associated with a user

|Property|Type|Used for|
|---|---|---|
|name|string|Used to identify the workspace|
|template|string|The template name on which this workspace is based|
|user|string|The user ID based on which AuthN/Z will be done|
|url|string|The Operator may fill this field with the URL where the workspace is available (not implemented yet)|

## Used technologies in Cluster

Our default implementation is using these technologies:

* oauth2-proxy as a reverse proxy to handle trafic to the running workspace container https://oauth2-proxy.github.io/oauth2-proxy/
* keycloak for authentication/authorization https://www.keycloak.org/

## Workspace Deployment

A workspace deployment consists of two containers. The first container is the IDE. The second container is the oauth2-proxy which acts as a reverse-proxy for the IDE. The oauth2-proxy is further configured via three config maps. The first map simply adds some template HTMLs for login and error cases. The second config map has the configuration for authenticating against KeyCloak. Finally the third configmap is used to update the allowed email addresses which are accepted. Via this third map we restrict access to the user defined in the Workspace resource.
