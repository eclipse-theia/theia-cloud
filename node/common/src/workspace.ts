import axios from 'axios';
import { v4 as uuidv4 } from 'uuid';

import { ServiceRequest, ServiceResponse } from './service';

export interface UserWorkspace {
  name: string;
  label: string;
  appDefinition: string;
  user: string;
  active: boolean;
}

export interface WorkspaceListRequest extends ServiceRequest {
  user: string;
}
export namespace WorkspaceListRequest {
  export const KIND = 'workspaceListRequest';
}

export interface WorkspaceCreationRequest extends ServiceRequest {
  appDefinition: string;
  user: string;
  label?: string;
}
export namespace WorkspaceCreationRequest {
  export const KIND = 'workspaceCreationRequest';
}

export interface WorkspaceCreationResponse extends ServiceResponse {
  workspace: UserWorkspace;
}

export interface WorkspaceDeletionRequest extends ServiceRequest {
  workspaceName: string;
  user: string;
}
export namespace WorkspaceDeletionRequest {
  export const KIND = 'workspaceDeletionRequest';
}

function toWorkspaceServiceUrl(serviceUrl: string): string {
  return serviceUrl + '/workspace';
}

export async function createWorkspace(options: WorkspaceCreationRequest): Promise<WorkspaceCreationResponse> {
  const { appId, serviceUrl, appDefinition, label, user = uuidv4() + '@theia.cloud', kind = WorkspaceCreationRequest.KIND } = options;
  const workspaceServiceUrl = toWorkspaceServiceUrl(serviceUrl);
  console.log('Calling to ' + workspaceServiceUrl);
  try {
    const response = await axios.post(
      workspaceServiceUrl,
      {
        data: { appDefinition, user, appId, label, kind },
        timeout: 300000
      }
    );
    if (response.data.error) {
      throw new Error(response.data.error);
    }
    return response.data;
  } catch (error) {
    console.error((error as any).message);
    throw error;
  }
}

export async function listWorkspaces(options: WorkspaceListRequest): Promise<UserWorkspace[]> {
  const { appId, serviceUrl, user = uuidv4() + '@theia.cloud', kind = WorkspaceListRequest.KIND } = options;
  const workspaceServiceUrl = toWorkspaceServiceUrl(serviceUrl);
  console.log('Calling to ' + workspaceServiceUrl);
  try {
    const response = await axios.get(
      workspaceServiceUrl,
      {
        data: { user, appId, kind },
        timeout: 300000
      }
    );
    return response.data as UserWorkspace[];
  } catch (error) {
    console.error((error as any).message);
    throw error;
  }
}

export async function deleteWorkspace(options: WorkspaceDeletionRequest): Promise<boolean> {
  const { appId, serviceUrl, workspaceName, user = uuidv4() + '@theia.cloud', kind = WorkspaceDeletionRequest.KIND } = options;
  const workspaceServiceUrl = toWorkspaceServiceUrl(serviceUrl);
  console.log('Calling to ' + workspaceServiceUrl);
  try {
    const response = await axios.delete(
      workspaceServiceUrl,
      {
        data: { workspaceName, user, appId, kind },
        timeout: 300000
      }
    );
    return !!response.data;
  } catch (error) {
    console.error((error as any).message);
    throw error;
  }
}
