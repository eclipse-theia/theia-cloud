# Session availability checks

The operator publishes a session's public URL after its availability check succeeds. With Keycloak enabled, the public URL is served through oauth2-proxy. An unauthenticated request can reach a login page while the application is still starting. Consequently, reaching that page alone does not establish that the application can answer HTTP requests. This is the problem described in [issue #493](https://github.com/eclipse-theia/theia-cloud/issues/493).

## Availability contract

For Keycloak-enabled sessions, publication requires both an internal application check and the existing public URL check. Sessions without Keycloak retain the public-only check. The public check retains redirect and TLS behavior and its one-retry fallback for responses other than 200, 404, and 503. For authenticated sessions, a failed public or internal attempt clears that pending fallback; the additional internal check does not authenticate as an end user.

The internal check sends an HTTP request to the application's root path, `/`, through its internal Kubernetes Service. It accepts a direct response from 200 through 399 and does not follow redirects. A redirect therefore demonstrates a response from the application, without allowing a redirect destination such as an identity provider to supply the result. A connection failure, read timeout, or response outside that range leaves the session waiting. This is an application HTTP availability check, not a guarantee that every application feature or dependency is ready.

Each authenticated-session probe connection has a five-second connect timeout and a five-second read timeout. These are separate limits, not a five-second total deadline for a request, polling iteration, or session startup. Internal and public checks run within the existing bounded polling loop. Publishing still requires an acceptable internal response in the relevant iteration; an earlier successful internal response cannot authorize publication after the application becomes unavailable.

Only one authenticated availability worker runs per namespace and Session UID in an operator process. Polling stops for deleted, replaced, completed, or errored sessions, and checks the UID again when writing the URL. Temporary Kubernetes API failures retry within the existing polling limit and discard pending public success.

The authenticated polling implementation lives in [`AuthenticatedSessionAvailability`](../java/operator/org.eclipse.theia.cloud.operator/src/main/java/org/eclipse/theia/cloud/operator/handler/AuthenticatedSessionAvailability.java), reached through `AddedHandlerUtil`, with invocation from the [lazy](../java/operator/org.eclipse.theia.cloud.operator/src/main/java/org/eclipse/theia/cloud/operator/handler/session/LazySessionHandler.java) and [eager](../java/operator/org.eclipse.theia.cloud.operator/src/main/java/org/eclipse/theia/cloud/operator/handler/session/EagerSessionHandler.java) session handlers.

## Internal Service and networking

The [internal Service template](../java/operator/org.eclipse.theia.cloud.operator/src/main/resources/templateInternalService.yaml) already exposes the application directly. Its named port, `http-internal`, defaults to Service port **3001** and targets `AppDefinition.spec.port`. For example, an application listening on port 3000 is reached through Service port 3001. URL construction uses the selected Service's actual named port rather than assuming that its exposed port equals the application's port.

The internal address uses the Service name and namespace. It does not hardcode a cluster domain such as `cluster.local`, and it does not copy the public ingress path into the application request. Kubernetes documents namespace-qualified Service names in its [DNS reference](https://kubernetes.io/docs/concepts/services-networking/dns-pod-service/). The public URL continues to come from the configured routing strategy.

Eager sessions must reserve the internal Service corresponding to the selected public Service, so both target the same application instance. Selecting an arbitrary unused internal Service would allow another healthy prewarmed application to hide a failure in the user's application. If handling resumes after a partial reservation, the handler must continue allocation and availability checking using the matching resources. Returning `false` does not itself schedule another attempt: [`BasicTheiaCloudOperator`](../java/operator/org.eclipse.theia.cloud.operator/src/main/java/org/eclipse/theia/cloud/operator/BasicTheiaCloudOperator.java) invokes the handler in response to session events. Recovery on a later event or replay is distinct from automatic retry scheduling.

The operator must be able to resolve the Service address and connect to the application through the cluster network. Run the operator inside the test cluster for the procedure below. The [local operator debugging guide](../java/operator/README.md) also supports running a Java process on the host, but that process needs additional cluster DNS and routing access for internal probes. Kubernetes API access alone is insufficient. In installations with NetworkPolicies, allow the required operator egress and application ingress, plus DNS access; see the [Kubernetes NetworkPolicy documentation](https://kubernetes.io/docs/concepts/services-networking/network-policies/).

The default [session Deployment](../java/operator/org.eclipse.theia.cloud.operator/src/main/resources/templateDeployment.yaml) does not configure an application readiness probe. This fix adds no Kubernetes probes or externally exposed authentication bypass. Application owners can configure their own probes separately; Kubernetes describes their traffic-routing effects in its [readiness probe documentation](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/).

## Local reproduction and validation

Use a disposable Minikube installation with Keycloak and oauth2-proxy. The [Terraform guide](../terraform/terraform.md) describes the getting-started installation, and the [test configurations](../terraform/test-configurations/test.md) provide authentication, path-routing, and eager-start examples. Some development configurations require a sibling `theia-cloud-helm` checkout and local Terraform state paths: verify those paths before applying them. Use the current chart's image settings to deploy the operator built from this checkout, following the [operator image build instructions](Building.md#operator). Keep the operator inside the cluster.

### Controllable application fixture

Create these two files in a temporary directory outside the repository. The fixture remains running and returns 503 until a file is created inside its container. It deliberately has no readiness probe, allowing Kubernetes and oauth2-proxy to remain available while its HTTP response is unready.

`server.py`:

```python
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from socket import gethostname


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        ready = Path("/tmp/application-ready").exists()
        self.send_response(200 if ready else 503)
        self.end_headers()
        state = "ready" if ready else "starting"
        self.wfile.write(f"{gethostname()}: {state}\n".encode())


ThreadingHTTPServer(("0.0.0.0", 3000), Handler).serve_forever()
```

`Dockerfile`:

```dockerfile
FROM python:3.12-alpine
COPY server.py /app/server.py
USER 1000
EXPOSE 3000
CMD ["python", "/app/server.py"]
```

From that temporary directory, load the image into the Minikube profile used by the installation:

```sh
minikube image build -t session-availability-fixture:493 .
```

Configure a test AppDefinition using this image, `imagePullPolicy: IfNotPresent`, `port: 3000`, and the installation's existing ingress or Route configuration. Disable monitoring for the fixture because it does not implement the monitor protocol. Do not add a readiness probe. Use separate sessions for the unpatched and patched operator runs so an already published URL cannot affect the comparison.

### Authenticated lazy session

1. With the unpatched operator, start a session for the fixture. Confirm the application and proxy containers are running and the internal Service returns 503. From a cluster-connected HTTP client, request `http://<internal-service>.<namespace>:3001/` without following redirects.
2. Request the public session URL without credentials and follow redirects. Confirm the response reaches the Keycloak login page. Observe whether the session's `status.url` is published despite the application's 503 response. Record the session resource, responses, and operator log timestamps.
3. Deploy the patched operator and start a fresh session with the same fixture. While the application returns 503, confirm that `status.url` remains unset even though the public login page is reachable. Observe more than one polling iteration.
4. Make that session's application ready, using its actual namespace, pod name, and application container name:

   ```sh
   kubectl -n <namespace> exec <session-pod> -c <application-container> -- touch /tmp/application-ready
   ```

5. Confirm the same internal Service now responds with 200, then wait for the next polling iteration and verify that the operator publishes the public URL. Authenticate in a browser and confirm the fixture responds through oauth2-proxy with the selected pod hostname. A fresh unauthenticated request must still encounter authentication.

### Authenticated eager sessions

Use the [eager-start configuration](../terraform/test-configurations/3-04_try-now_paths_eager-start/README.md) as a reference, with the fixture AppDefinition configured for at least **two** prewarmed instances. Keep one instance returning 503 and make the other return 200 using the marker command above.

Start sessions until the unready instance is assigned. For each assigned session, compare the public and internal Services' selectors with the selected pod's labels. They must identify the same instance. The ready instance must not allow the unready instance's session URL to be published. Then make the unready instance ready and verify publication. Exercise both allocation orders in automated tests so the result cannot depend on API list ordering.

Also exercise replay after an external Service has been reserved but before the matching internal Service has been reserved: handling must resume with the matching pair and reach availability checking. A failed handler return alone is not evidence that replay occurred; use a deliberate test invocation or a verified session event and record it.

### Compatibility and failure checks

- Repeat a successful startup with Keycloak disabled; its public-only behavior must remain intact.
- Repeat authenticated startup with path-based ingress and with host-based routing. The internal request must remain `/` in both cases.
- In automated tests, cover direct internal redirects without following them, 4xx/5xx responses, refused connections, read timeouts, and internal recovery followed by failure before publication.
- Cover public failure while the internal app succeeds, and preserve the existing public response/TLS handling. Verify a missing or invalid internal Service port cannot silently downgrade an authenticated session to public-only checking.
- Run the operator's focused regression tests and the affected Java module suites. Report test results separately from the live cluster observations; local HTTP fixtures alone do not establish Kubernetes DNS, Service routing, or authentication integration.

The procedure above describes expected validation. It is not a record that these checks have already passed.
