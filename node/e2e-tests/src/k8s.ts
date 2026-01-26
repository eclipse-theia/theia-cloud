import { CoreV1Api, CustomObjectsApi, KubeConfig } from '@kubernetes/client-node';

import {
  appDefinitionPlural,
  appDefinitionVersion,
  namespace,
  resourceGroup,
  sessionPlural,
  sessionVersion,
  workspacePlural,
  workspaceVersion
} from './constants';

const kc = new KubeConfig();
kc.loadFromDefault();
export const k8sApi = kc.makeApiClient(CustomObjectsApi);
export const coreV1Api = kc.makeApiClient(CoreV1Api);
const SESSION_LABEL_KEY = 'theia-cloud.io/session';

export interface ContainerLog {
  containerName: string;
  logs: string;
}

export interface PodLogs {
  podName: string;
  containers: ContainerLog[];
}

export interface SessionPodLogs {
  sessionName: string;
  pods: PodLogs[];
}

export async function getPodsForSession(sessionName: string): Promise<any[]> {
  try {
    const response = await coreV1Api.listNamespacedPod({
      namespace,
      labelSelector: `${SESSION_LABEL_KEY}=${sessionName}`
    });
    return response.items || [];
  } catch (error: any) {
    console.error(`Failed to list pods for session '${sessionName}':`, error.message || error);
    return [];
  }
}

export async function getPodLogs(podName: string, containerName: string): Promise<string> {
  try {
    const response = await coreV1Api.readNamespacedPodLog({
      name: podName,
      namespace,
      container: containerName,
      tailLines: 500 // Limit to last 500 lines to avoid huge outputs
    });
    return response || '';
  } catch (error: any) {
    return `[Error fetching logs: ${error.message || error}]`;
  }
}

export async function collectSessionPodLogs(sessionName: string): Promise<SessionPodLogs> {
  const result: SessionPodLogs = {
    sessionName,
    pods: []
  };

  const pods = await getPodsForSession(sessionName);
  if (pods.length === 0) {
    console.log(`No pods found for session '${sessionName}'`);
    return result;
  }

  console.log(`Found ${pods.length} pod(s) for session '${sessionName}'`);

  for (const pod of pods) {
    const podName = pod.metadata?.name;
    if (!podName) continue;

    const podLogs: PodLogs = {
      podName,
      containers: []
    };

    // Get all container names from the pod spec
    const containers = pod.spec?.containers || [];
    const initContainers = pod.spec?.initContainers || [];
    const allContainers = [...initContainers, ...containers];

    for (const container of allContainers) {
      const containerName = container.name;
      if (!containerName) continue;

      const logs = await getPodLogs(podName, containerName);
      podLogs.containers.push({
        containerName,
        logs
      });
    }

    result.pods.push(podLogs);
  }

  return result;
}

export function printSessionPodLogs(sessionLogs: SessionPodLogs): void {
  console.log('\n' + '='.repeat(80));
  console.log(`POD LOGS FOR SESSION: ${sessionLogs.sessionName}`);
  console.log('='.repeat(80));

  if (sessionLogs.pods.length === 0) {
    console.log('No pods found for this session.');
    return;
  }

  for (const pod of sessionLogs.pods) {
    console.log(`\n${'─'.repeat(60)}`);
    console.log(`POD: ${pod.podName}`);
    console.log('─'.repeat(60));

    for (const container of pod.containers) {
      console.log(`\n>>> Container: ${container.containerName}`);
      console.log(container.logs || '[No logs available]');
    }
  }

  console.log('\n' + '='.repeat(80));
}

export async function deleteAllSessions(): Promise<void> {
  const sessions: any = await k8sApi.listNamespacedCustomObject({
    group: resourceGroup,
    version: sessionVersion,
    namespace,
    plural: sessionPlural
  });

  for (const resource of sessions.items) {
    await k8sApi.deleteNamespacedCustomObject({
      group: resourceGroup,
      version: sessionVersion,
      namespace,
      plural: sessionPlural,
      name: resource.metadata.name
    });
  }
}

export async function deleteAllWorkspaces(): Promise<void> {
  const sessions: any = await k8sApi.listNamespacedCustomObject({
    group: resourceGroup,
    version: workspaceVersion,
    namespace,
    plural: workspacePlural
  });

  for (const resource of sessions.items) {
    await k8sApi.deleteNamespacedCustomObject({
      group: resourceGroup,
      version: workspaceVersion,
      namespace,
      plural: workspacePlural,
      name: resource.metadata.name
    });
  }
}

export async function getAppDefinition(name: string): Promise<any> {
  return k8sApi.getNamespacedCustomObject({
    group: resourceGroup,
    version: appDefinitionVersion,
    namespace,
    plural: appDefinitionPlural,
    name
  });
}

export async function listSessions(): Promise<any> {
  return k8sApi.listNamespacedCustomObject({
    group: resourceGroup,
    version: sessionVersion,
    namespace,
    plural: sessionPlural
  });
}

export async function isSessionDeleted(sessionName: string): Promise<boolean> {
  const sessions: any = await listSessions();
  return !sessions.items.some((s: any) => s.metadata.name === sessionName);
}

export async function waitForSessionDeletion(
  sessionName: string,
  startAfterMs: number,
  deadlineMs: number,
  intervalMs: number
): Promise<void> {
  const startTime = Date.now();

  console.log(`waitForSessionDeletion: waiting ${startAfterMs / 1000}s before first poll...`);
  await new Promise(resolve => setTimeout(resolve, startAfterMs));

  let elapsed = Date.now() - startTime;
  while (elapsed < deadlineMs) {
    const deleted = await isSessionDeleted(sessionName);
    console.log(`waitForSessionDeletion: poll at ${Math.round(elapsed / 1000)}s — deleted=${deleted}`);

    if (deleted) {
      console.log(`waitForSessionDeletion: session '${sessionName}' confirmed deleted after ${Math.round(elapsed / 1000)}s`);
      return;
    }

    await new Promise(resolve => setTimeout(resolve, intervalMs));
    elapsed = Date.now() - startTime;
  }

  /* Final check after deadline */
  if (await isSessionDeleted(sessionName)) {
    console.log(`waitForSessionDeletion: session '${sessionName}' confirmed deleted after ${Math.round((Date.now() - startTime) / 1000)}s (final check)`);
    return;
  }

  throw new Error(
    `waitForSessionDeletion: session '${sessionName}' still exists after ${Math.round(elapsed / 1000)}s (deadline: ${deadlineMs / 1000}s)`
  );
}

export async function getSession(name: string): Promise<any | undefined> {
  try {
    return await k8sApi.getNamespacedCustomObject({
      group: resourceGroup,
      version: sessionVersion,
      namespace,
      plural: sessionPlural,
      name
    });
  } catch (error: any) {
    if (error.code === 404 || error.response?.statusCode === 404 || error.statusCode === 404) {
      return undefined;
    }
    throw error;
  }
}
