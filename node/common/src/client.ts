import { AxiosRequestConfig, AxiosResponse } from 'axios';
import { v4 as uuidv4 } from 'uuid';

import {
  LaunchRequest as ClientLaunchRequest, RootResourceApi,
  SessionActivityRequest as ClientSessionActivityRequest, SessionLaunchResponse, SessionListRequest as ClientSessionListRequest,
  SessionResourceApi, SessionSpec, SessionStartRequest as ClientSessionStartRequest, SessionStopRequest as ClientSessionStopRequest,
  UserWorkspace, WorkspaceCreationRequest as ClientWorkspaceCreationRequest, WorkspaceCreationResponse,
  WorkspaceDeletionRequest as ClientWorkspaceDeletionRequest, WorkspaceListRequest as ClientWorkspaceListRequest, WorkspaceResourceApi
} from './client/api';
import { Configuration } from './client/configuration';

export interface ServiceRequest {
  serviceUrl: string;
  kind?: string;
}

export type LaunchRequest = ClientLaunchRequest & ServiceRequest;
export namespace LaunchRequest {
  export const KIND = 'launchRequest';

  export function ephemeral(serviceUrl: string, appId: string, appDefinition: string, timeout?: number, user: string = createUser()): LaunchRequest {
    return { serviceUrl, appId, appDefinition, user, ephemeral: true, timeout };
  }

  export function createWorkspace(serviceUrl: string, appId: string, appDefinition: string, timeout?: number, user: string = createUser(),
    workspaceName?: string, label?: string): LaunchRequest {
    return { serviceUrl, appId, appDefinition, user, label, workspaceName, ephemeral: false, timeout };
  }

  // eslint-disable-next-line max-len
  export function existingWorkspace(serviceUrl: string, appId: string, workspaceName: string, timeout?: number, appDefinition?: string, user: string = createUser()): LaunchRequest {
    return { serviceUrl, appId, workspaceName, appDefinition, user, timeout };
  }
}

export type SessionListRequest = ClientSessionListRequest & ServiceRequest;
export namespace SessionListRequest {
  export const KIND = 'launchRequest';
}

export type SessionStartRequest = ClientSessionStartRequest & ServiceRequest;
export namespace SessionStartRequest {
  export const KIND = 'sessionStartRequest';
}

export type SessionStopRequest = ClientSessionStopRequest & ServiceRequest;
export namespace SessionStopRequest {
  export const KIND = 'sessionStopRequest';
}

export type SessionActivityRequest = ClientSessionActivityRequest & ServiceRequest;
export namespace SessionActivityRequest {
  export const KIND = 'sessionActivityRequest';
}

export type WorkspaceListRequest = ClientWorkspaceListRequest & ServiceRequest;
export namespace WorkspaceListRequest {
  export const KIND = 'workspaceListRequest';
}

export type WorkspaceCreationRequest = ClientWorkspaceCreationRequest & ServiceRequest;
export namespace WorkspaceCreationRequest {
  export const KIND = 'workspaceCreationRequest';
}

export type WorkspaceDeletionRequest = ClientWorkspaceDeletionRequest & ServiceRequest;
export namespace WorkspaceDeletionRequest {
  export const KIND = 'workspaceDeletionRequest';
}

export namespace TheiaCloud {
  function basePath(url: string): string {
    // remove any path names as they are provided by the APIs
    const pathName = new URL(url).pathname;
    return url.endsWith(pathName) ? url.substring(0, url.length - new URL(url).pathname.length) : url;
  }

  function rootApi(url: string): RootResourceApi {
    return new RootResourceApi(new Configuration({ basePath: basePath(url) }));
  }

  function sessionApi(url: string): SessionResourceApi {
    return new SessionResourceApi(new Configuration({ basePath: basePath(url) }));
  }

  function workspaceApi(url: string): WorkspaceResourceApi {
    return new WorkspaceResourceApi(new Configuration({ basePath: basePath(url) }));
  }

  export async function launch(request: LaunchRequest, retries = 0): Promise<void> {
    const fullRequest = { kind: LaunchRequest.KIND, ...request };
    console.log('Calling to ' + fullRequest.serviceUrl);
    try {
      const response = await rootApi(request.serviceUrl).servicePost(fullRequest, createConfig());
      const sessionLaunch = response.data;
      if (sessionLaunch.success) {
        console.log(`Redirect to: https://${sessionLaunch.url}`);
        location.replace(`https://${sessionLaunch.url}`);
      } else {
        console.error(sessionLaunch.error);
        throw new Error(`Could not launch session: ${sessionLaunch.error}`);
      }
    } catch (error) {
      // Request timed out or returned an error with an error HTTP code.
      console.error((error as any).message);
      if (retries > 0) {
        launch(request, retries - 1);
      } else {
        throw error;
      }
    }
  }

  export namespace Session {
    export async function listSessions(request: SessionListRequest): Promise<SessionSpec[]> {
      const fullRequest = { kind: SessionListRequest.KIND, ...request };
      return getData(request.serviceUrl, () => sessionApi(request.serviceUrl).serviceSessionGet(fullRequest, createConfig()));
    }

    export async function startSession(request: SessionStartRequest): Promise<SessionLaunchResponse> {
      const fullRequest = { kind: SessionStartRequest.KIND, ...request };
      return getData(request.serviceUrl, () => sessionApi(request.serviceUrl).serviceSessionPost(fullRequest, createConfig()));
    }

    export async function stopSession(request: SessionStopRequest): Promise<boolean> {
      const fullRequest = { kind: SessionStopRequest.KIND, ...request };
      return getData(request.serviceUrl, () => sessionApi(request.serviceUrl).serviceSessionDelete(fullRequest, createConfig()));
    }

    export async function reportSessionActivity(request: SessionActivityRequest): Promise<boolean> {
      const fullRequest = { kind: SessionActivityRequest.KIND, ...request };
      return getData(request.serviceUrl, () => sessionApi(request.serviceUrl).serviceSessionPatch(fullRequest, createConfig()));
    }
  }

  export namespace Workspace {
    export async function listWorkspaces(request: WorkspaceListRequest): Promise<UserWorkspace[]> {
      const fullRequest = { kind: WorkspaceListRequest.KIND, ...request };
      return getData(request.serviceUrl, () => workspaceApi(request.serviceUrl).serviceWorkspaceGet(fullRequest, createConfig()));
    }

    export async function createWorkspace(request: WorkspaceCreationRequest): Promise<WorkspaceCreationResponse> {
      const fullRequest = { kind: WorkspaceCreationRequest.KIND, ...request };
      return getData(request.serviceUrl, () => workspaceApi(request.serviceUrl).serviceWorkspacePost(fullRequest, createConfig()));
    }

    export async function deleteWorkspace(request: WorkspaceDeletionRequest): Promise<boolean> {
      const fullRequest = { kind: WorkspaceDeletionRequest.KIND, ...request };
      return getData(request.serviceUrl, () => workspaceApi(request.serviceUrl).serviceWorkspaceDelete(fullRequest, createConfig()));
    }
  }
}

function createUser(): string {
  return uuidv4() + '@theia.cloud';
}

function createConfig(): AxiosRequestConfig {
  return { timeout: 30000 };
}

async function getData<T = any>(url: string, call: () => Promise<AxiosResponse<T>>): Promise<T> {
  console.log('Calling to ' + url);
  try {
    const response = await call();
    return response.data;
  } catch (error) {
    console.error((error as any).message);
    throw error;
  }
}
