# Changelog

## [0.11.0] - estimated 2024-07

- [common] Add option field to CRDs and increase version to `Session.v1beta8`, `Workspace.v1beta5` and `AppDefinition.v1beta10` [#293](https://github.com/eclipsesource/theia-cloud/pull/293) | [#55](https://github.com/eclipsesource/theia-cloud-helm/pull/55)
- [java] Separate operator default implementation from library to allow for easier customization [#303](https://github.com/eclipsesource/theia-cloud/pull/303)
- [node] Unify the existing landing and try now pages to a new ViteJS based landing page [#304](https://github.com/eclipsesource/theia-cloud/pull/304) | [#58](https://github.com/eclipsesource/theia-cloud-helm/pull/58) - contributed on behalf of STMicroelectronics
  - The new page is based on the old try now page but uses ViteJS instead of the deprecated Create React App
  - Extend configuration options for the new landing page for texts and logo file type
  - Removed terms and conditions
  - Build the common package as ESM and CJS bundles for extended compatibility

## [0.10.0] - 2024-04-02

- [.github/workflows] Improve version detection in workflows (do not build release commits, auto-detect version for demo publishing) [#280](https://github.com/eclipsesource/theia-cloud/pull/280) - contributed on behalf of STMicroelectronics
- [node] Separate `monitor` package from other workspaces to fix bundling the extension [#280](https://github.com/eclipsesource/theia-cloud/pull/280) - contributed on behalf of STMicroelectronics
- [conversion] Provide java conversion webhook for CRD updates [#283](https://github.com/eclipsesource/theia-cloud/pull/283) | [#49](https://github.com/eclipsesource/theia-cloud-helm/pull/49) - contributed on behalf of STMicroelectronics
- [.github/workflows] Add ci for `conversion-webhook` and fix typo to build on version bumps [#283](https://github.com/eclipsesource/theia-cloud/pull/283) | [#49](https://github.com/eclipsesource/theia-cloud-helm/pull/49) - contributed on behalf of STMicroelectronics
- [common] Update CRs, keep previous version and offer Hub (used by conversion-webhook) [#283](https://github.com/eclipsesource/theia-cloud/pull/283) | [#49](https://github.com/eclipsesource/theia-cloud-helm/pull/49) - contributed on behalf of STMicroelectronics
  - Move status like fields to status
    - `Session.v1beta7`: Move `url`, `lastActivity` and `error` fields from the spec to the status.
    - `Workspace.v1beta4`: Move the `error` field from the spec to the status. Also add the `error` field to `Workspace.v1beta3` as it was missing
  - Remove `timeout.strategy` from AppDefinition
    - `AppDefinition.v1beta9`: Removed `timeout.strategy` and `timeout.limit` is now just `timeout`. This was done, as there is only one Strategy left.
- [java] Update io.fabric8.kubernetes-client to version 6.10.0. Also update Quarkus platform to 3.8.1. This provides kubernetes 1.29 support [#287](https://github.com/eclipsesource/theia-cloud/pull/287)
- [terraform] Change terraform values to conform to helm chart changes [#289](https://github.com/eclipsesource/theia-cloud/pull/289) | [#52](https://github.com/eclipsesource/theia-cloud-helm/pull/52) - contributed on behalf of STMicroelectronics

## [0.9.0] - 2024-01-23

- [node/landing-page] Make npm workspace and remove yarn references from repo [#258](https://github.com/eclipsesource/theia-cloud/pull/258) | [#45](https://github.com/eclipsesource/theia-cloud-helm/pull/45) - contributed on behalf of STMicroelectronics
- [All components] Align [versioning](https://github.com/eclipsesource/theia-cloud#versioning) between all components and introduce Changelog [#258](https://github.com/eclipsesource/theia-cloud/pull/258) | [#45](https://github.com/eclipsesource/theia-cloud-helm/pull/45) - contributed on behalf of STMicroelectronics
- [All components] Update CRD version scheme for all CRDs from `theia.cloud/vXbeta` to `theia.cloud/v1betaX` [#266](https://github.com/eclipsesource/theia-cloud/pull/266) | [#46](https://github.com/eclipsesource/theia-cloud-helm/pull/46) - contributed on behalf of STMicroelectronics
- [java/service] Update Quarkus from 2.12.0 to 3.3.1 [#266](https://github.com/eclipsesource/theia-cloud/pull/266) - contributed on behalf of STMicroelectronics
- [java] Update fabric8 Kubernetes client from 5.12.2 to 6.7.2 [#266](https://github.com/eclipsesource/theia-cloud/pull/266) - contributed on behalf of STMicroelectronics
- [java/common] Introduce hub-objects for all custom resources [#266](https://github.com/eclipsesource/theia-cloud/pull/266) - contributed on behalf of STMicroelectronics
- [demo] Fix demo applications to Theia's latest Community Release (1.43.1) [#267](https://github.com/eclipsesource/theia-cloud/pull/267) - contributed on behalf of STMicroelectronics
- [node/monitor-theia] Make `@theia` dependencies peerDependencies [#267](https://github.com/eclipsesource/theia-cloud/pull/267) - contributed on behalf of STMicroelectronics
- [node] Update to node 20 [#269](https://github.com/eclipsesource/theia-cloud/pull/269)
- [All components] Clean up repository [#275](https://github.com/eclipsesource/theia-cloud/pull/275) - contributed on behalf of STMicroelectronics

## [0.8.1] - 2023-10-01

- Last Milestone based version. No changelog available due to alpha-phase.
