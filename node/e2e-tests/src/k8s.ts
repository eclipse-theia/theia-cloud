import { CustomObjectsApi, KubeConfig } from '@kubernetes/client-node';

import {
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
