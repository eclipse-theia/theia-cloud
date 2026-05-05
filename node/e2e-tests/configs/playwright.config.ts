import { PlaywrightTestConfig } from '@playwright/test';

/**
 * Compute the landing-page baseURL for the test run.
 *
 * Two host-generation patterns are supported:
 *
 * 1. OpenShift (MicroShift e2e CI). Routes use the apps domain
 *    configured in `valuesOpenShiftMonitor.yaml` -- by default
 *    `apps-microshift.testing`. The landing page is served at
 *    `try.<apps-domain>`. There is no `paths` mode for OpenShift
 *    (the Helm chart's `_helpers.tpl` rejects `usePaths=true` when
 *    `cloudProvider=OPENSHIFT`).
 *
 * 2. Minikube (existing nip.io-based tests). The cluster IP is
 *    embedded in the hostname (e.g. `trynow.192.168.49.2.nip.io`)
 *    or in the path (`192.168.49.2.nip.io/trynow`) depending on
 *    `MATRIX_PATHS`.
 */
function getBaseURL(): string {
  if (process.env.MATRIX_CLOUD_PROVIDER === 'OPENSHIFT') {
    const appsDomain = process.env.APPS_DOMAIN || 'apps-microshift.testing';
    return `https://try.${appsDomain}`;
  }
  return process.env.MATRIX_PATHS !== 'true'
    ? `https://trynow.${process.env.INGRESS_HOST}.nip.io`
    : `https://${process.env.INGRESS_HOST}.nip.io/trynow`;
}

const config: PlaywrightTestConfig = {
  testDir: '../lib/tests',
  testMatch: ['**/*.js'],
  workers: 1,
  fullyParallel: false,
  // Timeout for each test in milliseconds.
  timeout: 60 * 1000,
  use: {
    baseURL: getBaseURL(),
    browserName: 'chromium',
    permissions: ['clipboard-read'],
    screenshot: 'only-on-failure',
    ignoreHTTPSErrors: true
  },
  preserveOutput: 'failures-only',
  reporter: [['list']]
};

export default config;
