import { expect, test } from '@playwright/test';

import { namespace, resourceGroup, sessionPlural, sessionVersion } from '../constants';
import { deleteAllSessions, deleteAllWorkspaces, k8sApi } from '../k8s';

test.describe('Start Session', () => {
  test.beforeEach(async () => {
    deleteAllSessions();
    deleteAllWorkspaces();
  });

  test('should work', async ({ page, baseURL }) => {
    expect(baseURL).toBeDefined();
    await page.goto(baseURL!);

    if (process.env.MATRIX_KEYCLOAK === 'true') {
      /* Click on Login button */
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
    }

    await page.click('.App__try-now-button');

    /* Check loading animation */
    const loadingAnimation = page.locator('.Loading__description');
    await expect(loadingAnimation).toBeVisible();

    /* wait for loading to be done */
    await expect(loadingAnimation).toBeHidden({ timeout: 180000 });

    /* check redirect url */
    const browserUrl = page.url();
    expect(browserUrl).toContain(baseURL!.replace('trynow', 'instances'));

    /* check created session */
    const resources: any = await k8sApi.listNamespacedCustomObject({
      group: resourceGroup,
      version: sessionVersion,
      namespace,
      plural: sessionPlural
    });
    expect(resources.items).toHaveLength(1);

    const sessionUrl = resources.items[0].status.url;
    expect(sessionUrl).toBeDefined();
    const normalizedBrowserUrl = new URL(browserUrl);
    const normalizedSessionUrl = new URL(sessionUrl.startsWith('http') ? sessionUrl : `https://${sessionUrl}`);
    expect(normalizedBrowserUrl.hostname).toBe(normalizedSessionUrl.hostname);
    expect(normalizedBrowserUrl.pathname).toBe(normalizedSessionUrl.pathname);
  });

  test('should require login when accessing session URL in new browser context', async ({ page, baseURL, browser }) => {
    test.skip(process.env.MATRIX_KEYCLOAK !== 'true', 'Skipping test because keycloak not enabled');

    expect(baseURL).toBeDefined();

    /* Navigate to baseURL */
    await page.goto(baseURL!);

    /* Click on Login button */
    const loginButton = await page.locator('.App__try-now-button');
    await expect(loginButton).toHaveText('Login');
    await loginButton.click();

    /* Enter user data */
    await page.fill('#username', 'foo');
    await page.fill('#password', 'foo');
    await page.click('#kc-login');
    await page.waitForLoadState('networkidle');

    /* Start session */
    await page.click('.App__try-now-button');

    /* Wait for loading animation to appear then disappear */
    const loadingAnimation = page.locator('.Loading__description');
    await expect(loadingAnimation).toBeVisible();
    await expect(loadingAnimation).toBeHidden({ timeout: 180000 });

    /* Save the session URL */
    const sessionUrl = page.url();

    /* Create fresh browser context (no cookies) */
    const newContext = await browser.newContext();
    const newPage = await newContext.newPage();

    /* Navigate to session URL in fresh context */
    await newPage.goto(sessionUrl);
    await newPage.waitForLoadState('networkidle');

    /* Handle OAuth2 Proxy redirect to Keycloak */
    await newPage.waitForSelector('#kc-page-title', { timeout: 30000 });

    /* Verify the sign-in heading */
    const signInHeading = newPage.locator('#kc-page-title');
    await expect(signInHeading).toHaveText('Sign in to your account');

    /* Login as foo in fresh context */
    await newPage.fill('#username', 'foo');
    await newPage.fill('#password', 'foo');
    await newPage.click('#kc-login');
    await newPage.waitForLoadState('networkidle');

    /* Verify access to Theia application (not Access Forbidden page) */
    const pageTitle = await newPage.title();
    expect(pageTitle).toBe('Eclipse Theia');

    /* Cleanup */
    await newContext.close();
  });

  test('should deny access when different user tries to access session URL', async ({ page, baseURL, browser }) => {
    test.skip(process.env.MATRIX_KEYCLOAK !== 'true', 'Skipping test because keycloak not enabled');

    expect(baseURL).toBeDefined();

    /* Navigate to baseURL */
    await page.goto(baseURL!);

    /* Click on Login button */
    const loginButton = await page.locator('.App__try-now-button');
    await expect(loginButton).toHaveText('Login');
    await loginButton.click();

    /* Enter user data */
    await page.fill('#username', 'foo');
    await page.fill('#password', 'foo');
    await page.click('#kc-login');
    await page.waitForLoadState('networkidle');

    /* Start session */
    await page.click('.App__try-now-button');

    /* Wait for loading animation to appear then disappear */
    const loadingAnimation = page.locator('.Loading__description');
    await expect(loadingAnimation).toBeVisible();
    await expect(loadingAnimation).toBeHidden({ timeout: 180000 });

    /* Save the session URL */
    const sessionUrl = page.url();

    /* Create fresh browser context (no cookies) */
    const newContext = await browser.newContext();
    const newPage = await newContext.newPage();

    /* Navigate to session URL in fresh context */
    await newPage.goto(sessionUrl);
    await newPage.waitForLoadState('networkidle');

    /* Wait for Keycloak Login Page */
    await newPage.waitForSelector('#kc-page-title', { timeout: 30000 });

    /* Verify the sign-in heading */
    const signInHeading = newPage.locator('#kc-page-title');
    await expect(signInHeading).toHaveText('Sign in to your account');

    /* Login as bar (different user) */
    await newPage.fill('#username', 'bar');
    await newPage.fill('#password', 'bar');
    await newPage.click('#kc-login');
    await newPage.waitForLoadState('networkidle');

    /* Verify access is forbidden */
    const pageTitle = await newPage.title();
    expect(pageTitle).toBe('Access Forbidden');

    /* Cleanup */
    await newContext.close();
  });
});
