# Eager start test config

This contains a test configuration for eagerly started pods.
It does not use persistent workspaces.

It installs two appdefinitions:

- The default app defintion with 0 mininum instances and 10 maximum instances.
  - No pre-warmed pod is started for this
- A CDT cloud app definition with name `cdt-cloud-demo` with 1 minimum instance and 10 maximum instances.
  - 1 pre-warmed pod is started for this.
  - This may be adjusted by increasing the `minInstances` property of the app definition in [theia_cloud.tf](./theia_cloud.tf).
