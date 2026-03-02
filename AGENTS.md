# AGENTS.md — Theia Cloud Operator

Reference guide for AI coding agents operating in this repository.
Read this file at the start of every session.

---

## Project Overview

Multi-language, multi-module system for running Theia IDE instances on Kubernetes.

| Module | Language | Purpose |
|--------|----------|---------|
| `java/common/` | Java 21 / Maven | Shared K8s client, CRD models, utilities |
| `java/operator/` | Java 21 / Maven + Guice | Kubernetes Operator (manages Sessions, AppDefinitions) |
| `java/service/` | Java 21 / Maven + Quarkus | REST API for workspace/session management |
| `java/conversion/` | Java 21 / Maven | K8s CRD conversion webhooks |
| `node/` | TypeScript / NPM Workspaces | Landing page (React/Vite), monitor, e2e tests |
| `theia/` | TypeScript / Yarn + Lerna | Theia extensions (monitor, config-store) |
| `dockerfiles/` | — | Multi-stage Docker images for all services |
| `terraform/` | Terraform | Infra provisioning (Minikube, GKE, Helm) |

Current version: `1.2.0-SNAPSHOT`. Active feature branch: `feature/external-ls-v2`.

---

## Build & Test Commands

### Java (Maven)

All Java modules share a parent POM at `java/common/maven-conf/pom.xml`.
Run from `java/` (all modules) or from any individual module directory.

```bash
# Build everything
cd java && mvn clean install

# Build a single module (e.g. operator)
cd java/operator/org.eclipse.theia.cloud.operator && mvn clean install

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=WorkspaceResourceTests

# Run a single test method
mvn test -Dtest=WorkspaceResourceTests#testCreateWorkspace

# Integration tests (Quarkus @QuarkusTest)
mvn verify

# Native Quarkus build
mvn install -Pnative

# Sentry source bundle upload (requires SENTRY_AUTH_TOKEN)
mvn install -Psentry
```

### Node / Frontend (`node/`)

Node ≥ 20 required.

```bash
cd node
npm install
npm run build       # builds all workspaces + monitor
npm run lint        # lints all workspaces + monitor
npm run test        # runs e2e-tests workspace
```

### Theia Extensions (`theia/`)

Yarn 1.x + Lerna.

```bash
cd theia
yarn
yarn build          # clean + lerna run build
yarn lint           # lerna run lint
yarn watch          # lerna run watch (dev mode)
```

---

## Java Code Style

### File Header

Every Java file must start with the EPL-2.0 copyright block:

```java
/********************************************************************************
 * Copyright (C) <year> EclipseSource and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
```

### General Conventions

- **Java version**: 21. Use records, text blocks, and pattern matching where appropriate.
- **Indentation**: 4 spaces, no tabs.
- **Braces**: Egyptian style (opening brace on same line).
- **Line length**: ~120 characters.
- **Imports**: No wildcards. Group: `java.*`, `javax.*`/`jakarta.*`, third-party, project internal. Static imports at top, e.g. `import static org.eclipse.theia.cloud.common.util.LogMessageUtil.formatLogMessage;`.
- **Naming**: PascalCase for classes/records/enums, camelCase for methods/variables, SCREAMING_SNAKE_CASE for constants.
- **Annotations**: `@Inject` (Google Guice in operator; Jakarta in service), `@Singleton`, `@Override` always present.

### Logging

- Logger declaration: `private static final Logger LOGGER = LogManager.getLogger(ClassName.class);` (Log4j 2).
- Always pass `correlationId` as first argument via `formatLogMessage(correlationId, "...")`.
- Use `LOGGER.info(...)` for normal flow, `LOGGER.warn(...)` for best-effort failures, `LOGGER.error(...)` for unexpected errors.
- Never log sensitive data (tokens, passwords).

### Tracing (Sentry)

Use `Tracing.*` — **not** `SentryHelper.*` (removed):

```java
ISpan span = Tracing.childSpan("operation.name", "Human description");
try {
    // work
    Tracing.finishSuccess(span);
} catch (Exception e) {
    Tracing.finishError(span, e);
    throw e;
}
```

Always finalize spans in `finally` or catch blocks — never leave them open.

### Error Handling

- Catch specific exceptions first; add `catch (RuntimeException e)` as a safety net in factory/resource methods so callers always receive `Optional.empty()` rather than propagated exceptions.
- Never use empty catch blocks.
- Service layer: throw `TheiaCloudWebException` (maps to HTTP error responses).
- Operator layer: return `boolean` / `Optional<T>` to signal success/failure; log at `warn`/`error` level.

### Kubernetes (Fabric8)

- Always null-check `getEnv()`, `getVolumes()`, `getVolumeMounts()` — they can return `null` on freshly-templated objects. Initialize to `new ArrayList<>()` before mutation.
- Use `TheiaCloudPersistentVolumeUtil.getTheiaContainer(podSpec, appDefSpec)` to locate the Theia container (never match by container name directly).
- Fabric8 `.edit()` lambdas cannot return values; use `AtomicBoolean` / `AtomicReference` to communicate state out of the lambda.

### Language Server Specifics (operator module)

- LS operations are **best-effort**: if `createLanguageServer()` returns `false`, log a warning but continue the session flow without patching env vars into Theia.
- `javascript`/`node`/`-js-` image substrings must be checked **before** `java`/`jdt` to avoid misclassification (`"javascript".contains("java") == true`).
- Rollback orphaned K8s resources via `factory.deleteResources(session, correlationId)`, not via the K8s client directly.

---

## TypeScript / Frontend Code Style

Configured via `.prettierrc.js` (in `theia/`) and ESLint with `typescript-eslint` + `simple-import-sort`.

| Setting | Value |
|---------|-------|
| Indent | 2 spaces |
| Quotes | Single (`'`) |
| JSX quotes | Single |
| Trailing commas | None |
| Print width | 120 (100 for JSON/YAML) |
| Arrow parens | Avoid for single arg |
| End of line | LF |

- Imports sorted by `eslint-plugin-simple-import-sort`.
- File headers: same EPL-2.0 copyright block as Java (adapted for JS/TS comment syntax).

---

## Key Patterns to Follow

### Dependency Injection

- **Operator**: Google Guice — use `@Inject` on fields, bind in a `Module`.
- **Service**: Quarkus/Jakarta CDI — `@Inject`, `@ApplicationScoped`, `@RequestScoped`.
- Never instantiate service classes with `new` inside production code.

### Optional Usage

- Prefer `Optional<T>` return types over `null` for "may not exist" results.
- Always check `Optional.isEmpty()` before acting, and log at warn/error if absent when presence was expected.

### Tests

- **Framework**: JUnit 5 (`@Test`, `@BeforeEach`), Mockito (`@Mock`, `@InjectMocks`).
- **Quarkus tests**: `@QuarkusTest` + `@InjectMock` for CDI bean injection in tests.
- Test class names end with `Tests` (e.g., `WorkspaceResourceTests`).
- Static imports: `org.junit.jupiter.api.Assertions.*`, `org.mockito.Mockito.*`.

---

## Pre-existing Known Issues

- `Tracing cannot be resolved` LSP errors across many Java files are a **Maven classpath issue in the IDE only** — not real compilation errors. The code compiles fine with `mvn`.
- Do not attempt to fix these LSP errors; they are pre-existing and unrelated to feature work.

---

## Active Branch Context

- Branch: `feature/external-ls-v2`
- PR: #70 on `ls1intum/theia-cloud` fork
- PR addresses CodeRabbit review comments on the External Language Server feature.
- See commit history for the 14 fixes already applied (LS resource factory, eager/lazy session handlers, registry, config).
