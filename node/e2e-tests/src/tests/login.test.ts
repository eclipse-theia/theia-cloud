import { expect, test } from '@playwright/test';

test.describe('Login', () => {
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
    const launchButton = await page.locator('.App__try-now-button');
    await expect(launchButton).toContainText('Launch Theia with Theia Extension Monitor');
  });
});
