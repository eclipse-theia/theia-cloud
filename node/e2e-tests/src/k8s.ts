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
  const sessions: any = await k8sApi.listNamespacedCustomObject(
    resourceGroup,
    sessionVersion,
    namespace,
    sessionPlural
  );

  for (const resource of sessions.body.items) {
    await k8sApi.deleteNamespacedCustomObject(
      resourceGroup,
      sessionVersion,
      namespace,
      sessionPlural,
      resource.metadata.name
    );
  }
}

export async function deleteAllWorkspaces(): Promise<void> {
  const sessions: any = await k8sApi.listNamespacedCustomObject(
    resourceGroup,
    workspaceVersion,
    namespace,
    workspacePlural
  );

  for (const resource of sessions.body.items) {
    await k8sApi.deleteNamespacedCustomObject(
      resourceGroup,
      workspaceVersion,
      namespace,
      workspacePlural,
      resource.metadata.name
    );
  }
}
