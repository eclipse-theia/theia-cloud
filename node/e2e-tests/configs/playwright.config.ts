import { PlaywrightTestConfig } from '@playwright/test';

const config: PlaywrightTestConfig = {
  testDir: '../lib/tests',
  testMatch: ['**/*.js'],
  workers: 1,
  fullyParallel: false,
  // Timeout for each test in milliseconds.
  timeout: 60 * 1000,
  use: {
    baseURL:
      process.env.MATRIX_PATHS !== 'true'
        ? `https://trynow.${process.env.INGRESS_HOST}.nip.io`
        : `https://${process.env.INGRESS_HOST}.nip.io/trynow`,
    browserName: 'chromium',
    permissions: ['clipboard-read'],
    screenshot: 'only-on-failure',
    ignoreHTTPSErrors: true
  },
  preserveOutput: 'failures-only',
  reporter: [['list']]
};

export default config;
