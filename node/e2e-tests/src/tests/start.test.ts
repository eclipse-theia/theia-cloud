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
});
