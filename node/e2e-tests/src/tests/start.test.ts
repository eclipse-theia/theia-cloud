import { expect, test } from '@playwright/test';
import { KubeConfig, CustomObjectsApi } from '@kubernetes/client-node';
import { namespace, resourceGroup, resourcePlural, resourceVersion } from '../constats';

const kc = new KubeConfig();
kc.loadFromDefault();
const k8sApi = kc.makeApiClient(CustomObjectsApi);

test.describe('Start Session', () => {
  test.beforeEach(async () => {
    /* delete all sessions */
    const resources: any = await k8sApi.listNamespacedCustomObject(
      resourceGroup,
      resourceVersion,
      namespace,
      resourcePlural
    );

    for (const resource of resources.body.items) {
      await k8sApi.deleteNamespacedCustomObject(
        resourceGroup,
        resourceVersion,
        namespace,
        resourcePlural,
        resource.metadata.name
      );
    }
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
    expect(page.url()).toContain(baseURL!.replace('trynow', 'instances'));

    /* check created session */
    const resources: any = await k8sApi.listNamespacedCustomObject(
      resourceGroup,
      resourceVersion,
      namespace,
      resourcePlural
    );
    expect(resources.body.items).toHaveLength(1);
  });
});
