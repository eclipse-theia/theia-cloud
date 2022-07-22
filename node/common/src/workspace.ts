import axios from 'axios';
import { v4 as uuidv4 } from 'uuid';

import { ServiceRequest } from './service';

export interface UserWorkspace {
  name: string;
  label: string;
  appDefinition: string;
  user: string;
  active: boolean;
}

export interface WorkspacesListRequest extends ServiceRequest {
  user: string;
}

export interface WorkspaceCreationRequest extends ServiceRequest {
  appDefinition: string;
  user: string;
  label?: string;
}

export interface WorkspaceDeletionRequest extends ServiceRequest {
  workspaceName: string;
  user: string;
}

function toWorkspaceServiceUrl(serviceUrl: string): string {
  return serviceUrl + '/workspace';
}

export async function createWorkspace(options: WorkspaceCreationRequest): Promise<UserWorkspace> {
  const { appId, serviceUrl, appDefinition, label, user = uuidv4() + '@theia.cloud' } = options;
  const workspaceServiceUrl = toWorkspaceServiceUrl(serviceUrl);
  console.log('Calling to ' + workspaceServiceUrl);
  try {
    const response = await axios.post(
      workspaceServiceUrl,
      {
        data: { appDefinition, user, appId, label },
        timeout: 300000
      }
    );
    if (response.data.error) {
      throw new Error(response.data.error);
    }
    return response.data.workspace as UserWorkspace;
  } catch (error) {
    console.error((error as any).message);
    throw error;
  }
}

export async function listWorkspaces(options: WorkspacesListRequest): Promise<UserWorkspace[]> {
  const { appId, serviceUrl, user = uuidv4() + '@theia.cloud' } = options;
  const workspaceServiceUrl = toWorkspaceServiceUrl(serviceUrl);
  console.log('Calling to ' + workspaceServiceUrl);
  try {
    const response = await axios.get(
      workspaceServiceUrl,
      {
        data: { user, appId },
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
  const { appId, serviceUrl, workspaceName, user = uuidv4() + '@theia.cloud' } = options;
  const workspaceServiceUrl = toWorkspaceServiceUrl(serviceUrl);
  console.log('Calling to ' + workspaceServiceUrl);
  try {
    const response = await axios.delete(
      workspaceServiceUrl,
      {
        data: { workspaceName, user, appId },
        timeout: 300000
      }
    );
    return !!response.data;
  } catch (error) {
    console.error((error as any).message);
    throw error;
  }
}
