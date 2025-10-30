# Changelog

## [1.2.0] - estimated between 2025-11 and 2026-05

## [1.1.2] - 2025-09-26

- [java/operator] Use ingress path type ImplementationSpecific instead of Prefix [#449](https://github.com/eclipse-theia/theia-cloud/pull/449)

## [1.1.1] - 2025-08-01

- [java/operator] Create missing internal services in eager start app definition handler [#441](https://github.com/eclipse-theia/theia-cloud/pull/441)

## [1.1.0] - 2025-08-01

- [theia] Introduce new folder `theia` for all Theia extensions and an example app to test these [#389](https://github.com/eclipse-theia/theia-cloud/pull/389)
- [node/monitor-theia] Move Theia monitor extension to `theia/extensions/monitor-theia` [#389](https://github.com/eclipse-theia/theia-cloud/pull/389)
- [theia/extensions/monitor-theia] Update Theia dependencies to `^1.55.0` [#389](https://github.com/eclipse-theia/theia-cloud/pull/389)
- [ci] Add Theia CI workflow, add reusable Theia extension publish workflow [#389](https://github.com/eclipse-theia/theia-cloud/pull/388)
- [java] Fix and improve eager start handling [#393](https://github.com/eclipse-theia/theia-cloud/pull/393), [#396](https://github.com/eclipse-theia/theia-cloud/pull/396)
- [java/service] Add admin user concept and app definition scaling endpoint [#400](https://github.com/eclipse-theia/theia-cloud/pull/400)
- [docker] Provide ARM64 images [#404](https://github.com/eclipse-theia/theia-cloud/pull/404), [#408](https://github.com/eclipse-theia/theia-cloud/pull/408)
- [java/common] Truncate labels longer than 63 chars [#413](https://github.com/eclipse-theia/theia-cloud/pull/413), [#417](https://github.com/eclipse-theia/theia-cloud/pull/417)
- [java/conversion] Allow to set conversion webhook certificate reload period [#410](https://github.com/eclipse-theia/theia-cloud/pull/410)
- [all components] Introduce config store Theia extension and REST endpoint [#432](https://github.com/eclipse-theia/theia-cloud/pull/432), [#438](https://github.com/eclipse-theia/theia-cloud/pull/438)

### Breaking Changes in 1.1.0

- [java/common] Changed `LabelsUtil.createSessionLabels` to accept `AppDefinition` instead of `AppDefinitionSpec`

## [1.0.1] - 2025-03-24

- [java/common] Truncate labels longer than 63 chars [#413](https://github.com/eclipse-theia/theia-cloud/pull/413)

## [1.0.0] - 2024-11-29

- [java/operator] Add Theia Cloud Labels on Resources created by operator [#362](https://github.com/eclipse-theia/theia-cloud/pull/362)
- [node] Updated Node Dependencies [#371](https://github.com/eclipse-theia/theia-cloud/pull/371) - contributed on behalf of STMicroelectronics
- [terraform] Update `cert-manager`, `ingress-nginx`, and `keycloak` [#371](https://github.com/eclipse-theia/theia-cloud/pull/371) - contributed on behalf of STMicroelectronics

## [0.12.0] - 2024-10-30

- [All components] Documented naming conventions and updated code to follow [#368](https://github.com/eclipse-theia/theia-cloud/pull/368)
- [documentation] Add REST API markdown docs [#363](https://github.com/eclipse-theia/theia-cloud/pull/363)
- [node/common] Update Common Package to support listing app definitions [#361](https://github.com/eclipse-theia/theia-cloud/pull/361)
- [java/service] Extend service with a list app definitions endpoint [#361](https://github.com/eclipse-theia/theia-cloud/pull/361)
- [documentation] Improved debugging documentation [#354](https://github.com/eclipse-theia/theia-cloud/pull/354)
- [java] Fixed issue where monitor might have stopped session during session startup [#354](https://github.com/eclipse-theia/theia-cloud/pull/354)
- [terraform] Added Devcontainer for GKE demo [#353](https://github.com/eclipse-theia/theia-cloud/pull/353)
- [github] Moved from `eclipsesource` to `eclipse-theia` org as an official Theia subproject [#353](https://github.com/eclipse-theia/theia-cloud/pull/353) [#358](https://github.com/eclipse-theia/theia-cloud/pull/358)
- [node] Updated Node Dependencies [#345](https://github.com/eclipse-theia/theia-cloud/pull/345) - contributed on behalf of STMicroelectronics
- [java] Updated Maven Dependencies [#345](https://github.com/eclipse-theia/theia-cloud/pull/345) - contributed on behalf of STMicroelectronics

### Breaking Changes in 0.12.0

See the helm chart Changelog for [more details](https://github.com/eclipse-theia/theia-cloud-helm/blob/main/CHANGELOG.md#breaking-changes-in-0120).

We did some renaming in preparation for 1.0.0

- NOTE: default namespace renamed from `theiacloud` to `theia-cloud`
- Config Map template label key updated from `theiacloud` to `theia-cloud.io/template-purpose`
- PVC label `theia.cloud.workspace.name` renamed to `theia-cloud.io/workspace-name`
- System property `THEIA_CLOUD_APP_ID` renamed to `THEIACLOUD_APP_ID`
- System property `THEIA_CLOUD_USE_KEYCLOAK` renamed to `THEIACLOUD_USE_KEYCLOAK`
- Theia Monitor paths changed from `/services/theiacloud-` to `/services/theia-cloud-`

## [0.11.0] - 2024-07-23

- [common] Add option field to CRDs and increase version to `Session.v1beta8`, `Workspace.v1beta5` and `AppDefinition.v1beta10` [#293](https://github.com/eclipse-theia/theia-cloud/pull/293) | [#55](https://github.com/eclipse-theia/theia-cloud-helm/pull/55)
- [java] Separate operator default implementation from library to allow for easier customization [#303](https://github.com/eclipse-theia/theia-cloud/pull/303)
- [node] Unify the existing landing and try now pages to a new ViteJS based landing page [#304](https://github.com/eclipse-theia/theia-cloud/pull/304) | [#58](https://github.com/eclipse-theia/theia-cloud-helm/pull/58) - contributed on behalf of STMicroelectronics
  - The new page is based on the old try now page but uses ViteJS instead of the deprecated Create React App
  - Extend configuration options for the new landing page for texts and logo file type
  - Removed terms and conditions
  - Build the common package as ESM and CJS bundles for extended compatibility
- [common] Add `ingressHostnamePrefixes` list to `AppDefinition.v1beta10` [#298](https://github.com/eclipse-theia/theia-cloud/pull/298) | [#57](https://github.com/eclipse-theia/theia-cloud-helm/pull/57)
- [java] Improved naming for kubernetes resources [#326](https://github.com/eclipse-theia/theia-cloud/pull/326)

### Breaking Changes in 0.11.0

See the helm chart Changelog for [more details](https://github.com/eclipse-theia/theia-cloud-helm/blob/main/CHANGELOG.md).

## [0.10.0] - 2024-04-02

- [.github/workflows] Improve version detection in workflows (do not build release commits, auto-detect version for demo publishing) [#280](https://github.com/eclipse-theia/theia-cloud/pull/280) - contributed on behalf of STMicroelectronics
- [node] Separate `monitor` package from other workspaces to fix bundling the extension [#280](https://github.com/eclipse-theia/theia-cloud/pull/280) - contributed on behalf of STMicroelectronics
- [conversion] Provide java conversion webhook for CRD updates [#283](https://github.com/eclipse-theia/theia-cloud/pull/283) | [#49](https://github.com/eclipse-theia/theia-cloud-helm/pull/49) - contributed on behalf of STMicroelectronics
- [.github/workflows] Add ci for `conversion-webhook` and fix typo to build on version bumps [#283](https://github.com/eclipse-theia/theia-cloud/pull/283) | [#49](https://github.com/eclipse-theia/theia-cloud-helm/pull/49) - contributed on behalf of STMicroelectronics
- [common] Update CRs, keep previous version and offer Hub (used by conversion-webhook) [#283](https://github.com/eclipse-theia/theia-cloud/pull/283) | [#49](https://github.com/eclipse-theia/theia-cloud-helm/pull/49) - contributed on behalf of STMicroelectronics
  - Move status like fields to status
    - `Session.v1beta7`: Move `url`, `lastActivity` and `error` fields from the spec to the status.
    - `Workspace.v1beta4`: Move the `error` field from the spec to the status. Also add the `error` field to `Workspace.v1beta3` as it was missing
  - Remove `timeout.strategy` from AppDefinition
    - `AppDefinition.v1beta9`: Removed `timeout.strategy` and `timeout.limit` is now just `timeout`. This was done, as there is only one Strategy left.
- [java] Update io.fabric8.kubernetes-client to version 6.10.0. Also update Quarkus platform to 3.8.1. This provides kubernetes 1.29 support [#287](https://github.com/eclipse-theia/theia-cloud/pull/287)
- [terraform] Change terraform values to conform to helm chart changes [#289](https://github.com/eclipse-theia/theia-cloud/pull/289) | [#52](https://github.com/eclipse-theia/theia-cloud-helm/pull/52) - contributed on behalf of STMicroelectronics

## [0.9.0] - 2024-01-23

- [node/landing-page] Make npm workspace and remove yarn references from repo [#258](https://github.com/eclipse-theia/theia-cloud/pull/258) | [#45](https://github.com/eclipse-theia/theia-cloud-helm/pull/45) - contributed on behalf of STMicroelectronics
- [All components] Align [versioning](https://github.com/eclipse-theia/theia-cloud#versioning) between all components and introduce Changelog [#258](https://github.com/eclipse-theia/theia-cloud/pull/258) | [#45](https://github.com/eclipse-theia/theia-cloud-helm/pull/45) - contributed on behalf of STMicroelectronics
- [All components] Update CRD version scheme for all CRDs from `theia.cloud/vXbeta` to `theia.cloud/v1betaX` [#266](https://github.com/eclipse-theia/theia-cloud/pull/266) | [#46](https://github.com/eclipse-theia/theia-cloud-helm/pull/46) - contributed on behalf of STMicroelectronics
- [java/service] Update Quarkus from 2.12.0 to 3.3.1 [#266](https://github.com/eclipse-theia/theia-cloud/pull/266) - contributed on behalf of STMicroelectronics
- [java] Update fabric8 Kubernetes client from 5.12.2 to 6.7.2 [#266](https://github.com/eclipse-theia/theia-cloud/pull/266) - contributed on behalf of STMicroelectronics
- [java/common] Introduce hub-objects for all custom resources [#266](https://github.com/eclipse-theia/theia-cloud/pull/266) - contributed on behalf of STMicroelectronics
- [demo] Fix demo applications to Theia's latest Community Release (1.43.1) [#267](https://github.com/eclipse-theia/theia-cloud/pull/267) - contributed on behalf of STMicroelectronics
- [node/monitor-theia] Make `@theia` dependencies peerDependencies [#267](https://github.com/eclipse-theia/theia-cloud/pull/267) - contributed on behalf of STMicroelectronics
- [node] Update to node 20 [#269](https://github.com/eclipse-theia/theia-cloud/pull/269)
- [All components] Clean up repository [#275](https://github.com/eclipse-theia/theia-cloud/pull/275) - contributed on behalf of STMicroelectronics

## [0.8.1] - 2023-10-01

- Last Milestone based version. No changelog available due to alpha-phase.
