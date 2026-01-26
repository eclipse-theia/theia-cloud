import { expect, Page, test, TestInfo } from '@playwright/test';

import {
  collectSessionPodLogs,
  deleteAllSessions,
  deleteAllWorkspaces,
  getSession,
  listSessions,
  printSessionPodLogs,
  waitForSessionDeletion
} from '../k8s';

let currentSessionName: string | undefined;

async function loginAndStartSession(page: Page, baseURL: string, appDefinition?: string): Promise<string> {
  const url = new URL(baseURL);
  if (appDefinition) {
    url.searchParams.set('appDef', appDefinition);
  }
  await page.goto(url.toString());

  /* Login if Keycloak is enabled */
  if (process.env.MATRIX_KEYCLOAK === 'true') {
    const loginButton = await page.locator('.App__try-now-button');
    await expect(loginButton).toHaveText('Login');
    await loginButton.click();
    const signInHeading = await page.locator('#kc-page-title');
    await expect(signInHeading).toHaveText('Sign in to your account');

    await page.fill('#username', 'foo');
    await page.fill('#password', 'foo');
    await page.click('#kc-login');
    await page.waitForLoadState('networkidle');
  }

  /* Start session */
  await page.click('.App__try-now-button');

  /* Wait for loading animation to appear then disappear */
  const loadingAnimation = page.locator('.Loading__description');
  await expect(loadingAnimation).toBeVisible();
  await expect(loadingAnimation).toBeHidden({ timeout: 180000 });

  /* Get session info */
  const sessions: any = await listSessions();
  expect(sessions.items).toHaveLength(1);
  const sessionName = sessions.items[0].metadata.name;
  const sessionAppDef = sessions.items[0].spec?.appDefinition;
  console.log('DEBUG: Session created:', sessionName);
  console.log('DEBUG: Session appDefinition:', sessionAppDef);

  /* Verify session exists */
  const session = await getSession(sessionName);
  expect(session).toBeDefined();

  currentSessionName = sessionName;

  return sessionName;
}

test.describe('Session Timeout', () => {
  test.beforeEach(async () => {
    /* Reset session tracking and clean up any leftover sessions */
    currentSessionName = undefined;
    await deleteAllSessions();
    await deleteAllWorkspaces();
  });

  test.afterEach(async ({}, testInfo: TestInfo) => {
    /* Collect pod logs if test failed */
    if (testInfo.status !== 'passed' && currentSessionName) {
      console.log(`\nTest '${testInfo.title}' failed. Collecting pod logs for session '${currentSessionName}'...`);
      try {
        const sessionLogs = await collectSessionPodLogs(currentSessionName);
        printSessionPodLogs(sessionLogs);
      } catch (error: any) {
        console.error('Failed to collect session pod logs:', error.message || error);
      }
    }

    /* Clean up sessions and workspaces */
    await deleteAllSessions();
    await deleteAllWorkspaces();
  });

  test('session should be terminated after fixed time timeout', async ({ page, baseURL }) => {
    test.setTimeout(360_000);
    expect(baseURL).toBeDefined();
    const sessionName = await loginAndStartSession(page, baseURL!, 'theia-cloud-demo');
    /* Poll for session deletion: start at 1 min, deadline at 4.5 min, poll every 30s */
    await waitForSessionDeletion(sessionName, 60_000, 270_000, 30_000);
  });

  test('should show inactivity warning popup (Theia monitor)', async ({ page, baseURL }) => {
    test.setTimeout(360_000);
    expect(baseURL).toBeDefined();
    await loginAndStartSession(page, baseURL!, 'theia-cloud-monitor-theia-popup');
    /* Wait 3.5 min for popup to appear (notifyAfter=2, monitorInterval=1) */
    await page.waitForTimeout(210_000);
    await expect(page.getByText('Are you still here?')).toBeVisible();
    console.log('DEBUG: Theia monitor popup is visible');
  });

  test('should show inactivity warning popup (VSCode monitor)', async ({ page, baseURL }) => {
    test.setTimeout(360_000);
    expect(baseURL).toBeDefined();
    await loginAndStartSession(page, baseURL!, 'theia-cloud-monitor-vscode-popup');
    /* Wait 3.5 min for popup to appear (notifyAfter=2, monitorInterval=1) */
    await page.waitForTimeout(210_000);
    await expect(page.getByText('Are you still here?')).toBeVisible({ timeout: 60_000 });
    console.log('DEBUG: VSCode monitor popup is visible');
  });

  test('session should be terminated after inactivity timeout (Theia monitor)', async ({ page, baseURL }) => {
  test.setTimeout(600_000);
    expect(baseURL).toBeDefined();
    const sessionName = await loginAndStartSession(page, baseURL!, 'theia-cloud-monitor-theia-timeout');
    /* timeoutAfter=4, monitorInterval=1 → session killed ~4-5 min after creation */
    /* No popup will appear (notifyAfter=15 > timeoutAfter=4) */
	await waitForSessionDeletion(sessionName, 180_000, 480_000, 30_000);
    console.log('DEBUG: Theia monitor session deleted by inactivity');
  });

  test('session should be terminated after inactivity timeout (VSCode monitor)', async ({ page, baseURL }) => {
    test.setTimeout(600_000);
    expect(baseURL).toBeDefined();
    const sessionName = await loginAndStartSession(page, baseURL!, 'theia-cloud-monitor-vscode-timeout');
    /* timeoutAfter=4, monitorInterval=1 → session killed ~4-5 min after creation */
    /* No popup will appear (notifyAfter=15 > timeoutAfter=4) */
    await waitForSessionDeletion(sessionName, 180_000, 480_000, 30_000);
    console.log('DEBUG: VSCode monitor session deleted by inactivity');
  });
});
