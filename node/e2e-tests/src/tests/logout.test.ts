import { expect, test } from '@playwright/test';

import { deleteAllSessions, deleteAllWorkspaces } from '../k8s';

test.describe('Logout', () => {
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

    /* Log out */
    const logoutImageTitle = await page.locator('#root .App .header p:nth-child(2) svg > title');
    await expect(logoutImageTitle).toHaveText('logout');
    await page.click('#root .App .header p:nth-child(2) a');
    await page.waitForLoadState('networkidle');
    const loginButtonAgain = await page.locator('.App__try-now-button');
    await expect(loginButtonAgain).toHaveText('Login');
  });

  test('not available when not logged in,', async ({ page, baseURL }) => {
    test.skip(process.env.MATRIX_KEYCLOAK !== 'true', 'Skipping test because keycloak not enabled');

    expect(baseURL).toBeDefined();

    /* Check that logout ui is not there but login ui*/
    // Assert that the logout UI elements are not present
    await page.goto(baseURL!);
    const logoutImageTitle = page.locator('#root .App .header p:nth-child(2) svg > title');
    await expect(logoutImageTitle).toHaveCount(0);

    const logoutLink = page.locator('#root .App .header p:nth-child(2) a');
    await expect(logoutLink).toHaveCount(0);

    const loginButton = await page.locator('.App__try-now-button');
    await expect(loginButton).toHaveText('Login');
  });

  test('not available when not using Keycloak', async ({ page, baseURL }) => {
    test.skip(process.env.MATRIX_KEYCLOAK === 'true', 'Skipping test because keycloak is enabled');

    expect(baseURL).toBeDefined();

    /* Check that logout ui is not there but login ui*/
    // Assert that the logout UI elements are not present
    await page.goto(baseURL!);
    const logoutImageTitle = page.locator('#root .App .header p:nth-child(2) svg > title');
    await expect(logoutImageTitle).toHaveCount(0);

    const logoutLink = page.locator('#root .App .header p:nth-child(2) a');
    await expect(logoutLink).toHaveCount(0);

    const loginButton = await page.locator('.App__try-now-button');
    await expect(loginButton).toContainText('Launch Theia with Theia Extension Monitor');
  });
});
