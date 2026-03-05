## Summary
Fixes eager session re-authentication failures caused by pool reconciliation recreating eager resources (especially email allowlist ConfigMaps) even when they were not outdated.

## Problem
Eager sessions could fail authentication after some time although the session remained valid.

Observed behavior:
- oauth2-proxy reported invalid email / auth failures
- eager email ConfigMap `authenticated-emails-list` was reset to empty
- issue did not reproduce for lazy sessions

## Root Cause
`PrewarmedResourcePool.reconcile()` used `OwnershipManager.isOwnedSolelyBy(...)` as recreation criterion.

That allowed reconcile to recreate eager resources that were solely AppDefinition-owned, including email ConfigMaps, which reset user-specific allowlists.

## Solution
Restrict resource recreation during reconcile to resources that are both:
- solely owned by the AppDefinition, and
- outdated by `theia-cloud.io/appdefinition-generation`

This preserves active eager session bindings unless a real generation update requires recreation.

## Files Changed
- `java/operator/org.eclipse.theia.cloud.operator/src/main/java/org/eclipse/theia/cloud/operator/pool/PrewarmedResourcePool.java`

## Verification
- `mvn -q -DskipTests compile` in `java/operator/org.eclipse.theia.cloud.operator`
- manual cluster investigation in `theia-staging` confirmed previous reset pattern and oauth2-proxy invalid-email failures

## Risk Assessment
Low to medium.

Behavior change is scoped to reconcile recreation criteria. Resource creation/deletion paths remain unchanged.

## Rollout Notes
After deploying the operator update:
1. Validate that eager session reopen after idle time still authenticates successfully.
2. Confirm eager email ConfigMaps are not recreated/reset during normal reconcile/restart.
3. Verify recreation still happens when AppDefinition generation changes.
