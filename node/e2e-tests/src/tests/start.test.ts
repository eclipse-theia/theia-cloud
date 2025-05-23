import { expect, Page, test } from '@playwright/test';

import { namespace, resourceGroup, sessionPlural, sessionVersion } from '../constants';
import { deleteAllSessions, deleteAllWorkspaces, k8sApi } from '../k8s';

async function loginIfNeeded(page: Page): Promise<void> {
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
}

async function startSession(page: Page): Promise<void> {
  await page.click('.App__try-now-button');

  /* Check loading animation */
  const loadingAnimation = page.locator('.Loading__description');
  await expect(loadingAnimation).toBeVisible();

  /* wait for loading to be done */
  await expect(loadingAnimation).toBeHidden({ timeout: 180000 });
}

async function startSessionAndVerify(
  page: Page,
  baseURL: string,
  expectedSessionCount: number,
  expectedHeaderText: string,
  sessionIndex = expectedSessionCount - 1
): Promise<string> {
  await page.click('.App__try-now-button');

  /* Check loading animation */
  const loadingAnimation = page.locator('.Loading__description');
  await expect(loadingAnimation).toBeVisible();

  /* wait for loading to be done */
  await expect(loadingAnimation).toBeHidden({ timeout: 180000 });

  /* check redirect url */
  const browserUrl = page.url();
  expect(browserUrl).toContain(baseURL.replace('trynow', 'instances'));

  /* check created session */
  const resources: any = await k8sApi.listNamespacedCustomObject(
    resourceGroup,
    sessionVersion,
    namespace,
    sessionPlural
  );
  expect(resources.body.items).toHaveLength(expectedSessionCount);

  const sessionUrl = resources.body.items[sessionIndex].status.url;
  expect(sessionUrl).toBeDefined();
  const normalizedBrowserUrl = new URL(browserUrl);
  const normalizedSessionUrl = new URL(sessionUrl.startsWith('http') ? sessionUrl : `https://${sessionUrl}`);
  expect(normalizedBrowserUrl.hostname).toBe(normalizedSessionUrl.hostname);
  expect(normalizedBrowserUrl.pathname).toBe(normalizedSessionUrl.pathname);

  const gsHeader = page.locator('.gs-header h1');
  await expect(gsHeader).toBeVisible();
  await expect(gsHeader).toContainText(expectedHeaderText);

  return sessionUrl;
}

test.describe('Start', () => {
  test.beforeEach(async () => {
    deleteAllSessions();
    deleteAllWorkspaces();
  });

  test('Session should work (default:theia-monitor)', async ({ page, baseURL }) => {
    expect(baseURL).toBeDefined();
    await page.goto(baseURL!);
    await loginIfNeeded(page);
    await startSessionAndVerify(page, baseURL!, 1, 'Theia Cloud Example');
  });

  test('Session should work (switched:vscode-monitor)', async ({ page, baseURL }) => {
    expect(baseURL).toBeDefined();
    await page.goto(baseURL!);
    await loginIfNeeded(page);

    /* Switch to VSCode Monitor app */
    const appSelect = page.locator('#selectapp');
    await appSelect.selectOption('theia-cloud-monitor-vscode');

    await startSessionAndVerify(page, baseURL!, 1, 'Eclipse Theia IDE');
  });

  test('multiple Sessions', async ({ page, baseURL }) => {
    expect(baseURL).toBeDefined();

    // we allow 3 sessions per user
    // theia-monitor allows 10 instances
    // vscode-monitor allows 1 instance

    // First session: theia-monitor
    await page.goto(baseURL!);
    await loginIfNeeded(page);
    const firstSessionURL = await startSessionAndVerify(page, baseURL!, 1, 'Theia Cloud Example');

    // Second session: vscode-monitor
    await page.goto(baseURL!);
    await page.waitForLoadState('networkidle');
    let appSelect = page.locator('#selectapp');
    await appSelect.selectOption('theia-cloud-monitor-vscode');
    await startSessionAndVerify(page, baseURL!, 2, 'Eclipse Theia IDE');

    // Third session attempt: vscode-monitor, max instances reached
    await page.goto(baseURL!);
    await page.waitForLoadState('networkidle');
    appSelect = page.locator('#selectapp');
    await appSelect.selectOption('theia-cloud-monitor-vscode');
    await startSession(page);
    const errorMessage = page.locator('.App__error-message');
    await expect(errorMessage).toBeVisible();
    const errorText = await errorMessage.textContent();
    expect(errorText).toContain('ERROR: Max instances reached. Could not create session.');

    // Start sessions again
    // if (process.env.MATRIX_EPHEMERAL === 'true') {
    // with ephemeral we should get redirected to the existing sessions
    await page.goto(baseURL!);
    await page.waitForLoadState('networkidle');
    const thirdSessionURL = await startSessionAndVerify(page, baseURL!, 2, 'Theia Cloud Example', 0);
    expect(thirdSessionURL).toEqual(firstSessionURL);
    // }
  });
});
