# Getting started with Minikube

Minikube is a local Kubernetes that allows you to test and develop for Kubernetes.\
In this guide we will show you how to install minikube and helm 3 as well as cert-manager, the cloud native certificate management, the NginX Ingress Controller and Keycloak. We will use existing installation methods for all of the Theia Cloud preqrequisites.\
Finally we will install and try Theia Cloud using helm.

If you have no experience with Kubernetes/Minikube yet, we encourage you to check out some basic tutorials first, e.g. <https://kubernetes.io/docs/tutorials/hello-minikube/> and <https://kubernetes.io/docs/tutorials/kubernetes-basics/>

## Install and start Theia Cloud on minikube

Please refer to [our terraform documentation](../../../terraform/terraform.md) and the ready to use [minikube configuration](../../../terraform/terraform.md#minikube).

### Accept Self Signed Certificates

Since we are running in Minikube on our local machine, which is not exposed to the internet, we cannot use letsencrypt certificates.

Please accept the locally issued certificates:

In Chrome:
`Your connection is not private -> Advanced -> Proceed to ...`

In Firefox:
`Warning: Potential Security Risk Ahead -> Advanced... -> Accept the Risk and Continue`

## Testing local Theia Cloud images

You can test locally build images (e.g. of the landing page, service, or operator) by building them in Minikube and then using them in the Minikube Helm chart.

To build images directly in Minikube, execute `eval $(minikube docker-env)` in a terminal.
With this, the `docker` command in this terminal runs inside Minikube.

Build the docker image as usual.

- Adapt the AppDefinition `image` value to match your built image.

## Troubleshooting

### Operator cannot set the workspace URL to the Session

This usually happens when using custom host names that are mapped in a hosts definition file (e.g. `/etc/hosts` on Linux) to the local IP of Minikube.
One [known case](https://github.com/eclipse-theia/theia-cloud/issues/150) occurred on MacOS with Docker Desktop:
The VM running Kubernetes does not know the hosts settings of the host operating system.
Thus, the custom hostname could not be resolved in the cluster and, thus, the operator could not resolve the session pods' addresses.

**Symptoms:**

- Service times out after launching a session
- Operator log contains messages stating the session pod is not available. Log messages can look like this:

```bash
ERROR org.eclipse.theia.cloud.operator.handler.impl.AddedHandlerUtil - [16695cf8-1e88-4ca9-93b2-483ebb89e5e4] ws.myhostname.io/e91be8be-0ed3-4c81-8a7b-b3d03bad6fd2/ is NOT available yet.
```

- Session URL is never set to the Session custom resource

**Potential solutions:**

- Do not use custom hostnames but use wildcard dns services like [nip.io](https://nip.io/) to get a hostname pointing to your local cluster.
- Alternatively, configure the Kubernetes VM (e.g. Minikube, Docker Desktop) to be able to resolve your custom hostname. Potentially, this can be achieved by editing the `/etc/hosts` file inside the VM.
