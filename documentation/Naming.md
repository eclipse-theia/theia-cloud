# Naming Conventions

The project name is **Theia Cloud** (with a space), which is a subproject of Eclipse Theia.

Since various languages and use cases follow different naming conventions, we aim to align with these conventions as closely as possible.

Below are guidelines for commonly used naming conventions specific to this project.

## `theia.cloud`

Use `theia.cloud` in hierarchical structures and identifiers. Examples include:

- **Java package/jar names**: `org.eclipse.theia.cloud`
- **Java system properties**: `-Dtheia.cloud.app.id=yourAppId`
- **Kubernetes group names/metadata**: `theia.cloud`, `theia.cloud/v1beta9`, `sessions.theia.cloud`
- **Theia command identifiers**: `theia.cloud.monitor.activity.report`

## `TheiaCloud` / `theiaCloud`

Use **PascalCase** or **camelCase** for Java/TypeScript class, method, and property names, such as `DefaultTheiaCloudOperatorLauncher` or `window.theiaCloudConfig`.

This format is also appropriate in contexts where spaces are avoided for technical reasons, such as in **Keycloak Realm** names (e.g., `TheiaCloud`).

## `theia-cloud`

This format serves as a preferred technical abbreviation. Use it when no other conventions apply, including:

- **NPM Package names**
- **File names**
- **k8s namespaces**

## `THEIACLOUD`

Use **all uppercase** with underscores for **environment variables**, such as `THEIACLOUD_SESSION_NAME`.

We can't use `THEIA_CLOUD` because `THEIA_` env variables may get special treatment by Theia itself.

## Theia Cloud

In general documentation and free text, use the standard project name **Theia Cloud** (with a space). Avoid spaces in technical contexts.

## `theia-cloud.io`

For **Kubernetes labels**, use the project domain as a prefix to custom labels, such as `theia-cloud.io/workspace-name`.

## Avoid `theiacloud` in Code

Avoid using the project name without separators or spaces, especially without PascalCase or camelCase. This format is reserved for usernames, such as those on Docker or npm.
