import { expect, test } from '@playwright/test';

import { deleteAllSessions, deleteAllWorkspaces } from '../k8s';

test.describe('Login', () => {
  test.beforeEach(async () => {
    deleteAllSessions();
    deleteAllWorkspaces();
  });

  test('should work', async ({ page, baseURL }) => {
    test.skip(process.env.MATRIX_KEYCLOAK !== 'true', 'Skipping test because keycloak not enabled');

    expect(baseURL).toBeDefined();

    /* Click on Login button */
    await page.goto(baseURL!);
    const loginButton = await page.locator('.App__try-now-button');
    await expect(loginButton).toHaveText('Login');
    await loginButton.click();
    const signInHeading = await page.locator('#kc-page-title');
    await expect(signInHeading).toHaveText('Sign in to your account');

    /* Enter user data */
    await page.fill('#username', 'foo');
    await page.fill('#password', 'foo');
    await page.click('#kc-login');
    await page.waitForLoadState('networkidle');
    const launchButton = await page.locator('.App__try-now-button');
    await expect(launchButton).toContainText('Launch Theia with Theia Extension Monitor');
  });

  test('not required when already logged in', async ({ page, baseURL }) => {
    test.skip(process.env.MATRIX_KEYCLOAK !== 'true', 'Skipping test because keycloak not enabled');

    expect(baseURL).toBeDefined();

    /* Click on Login button */
    await page.goto(baseURL!);
    const loginButton = await page.locator('.App__try-now-button');
    await expect(loginButton).toHaveText('Login');
    await loginButton.click();
    const signInHeading = await page.locator('#kc-page-title');
    await expect(signInHeading).toHaveText('Sign in to your account');

    /* Enter user data */
    await page.fill('#username', 'foo');
    await page.fill('#password', 'foo');
    await page.click('#kc-login');
    await page.waitForLoadState('networkidle');

    /* reload page */
    await page.reload();
    await page.waitForLoadState('networkidle');

    /* check button and email */
    const launchButton = await page.locator('.App__try-now-button');
    await expect(launchButton).toContainText('Launch Theia with Theia Extension Monitor');
    const email = await page.locator('#root .App .header p:nth-child(1)');
    await expect(email).toHaveText('foo@theia-cloud.io');
  });

  test('not required when not using Keycloak', async ({ page, baseURL }) => {
    test.skip(process.env.MATRIX_KEYCLOAK === 'true', 'Skipping test because keycloak is enabled');

    expect(baseURL).toBeDefined();

    /* Check no Login Button */
    await page.goto(baseURL!);
    const launchButton = await page.locator('.App__try-now-button');
    await expect(launchButton).toContainText('Launch Theia with Theia Extension Monitor');
  });
});
