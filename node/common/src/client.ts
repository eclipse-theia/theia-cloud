import { v4 as uuidv4 } from 'uuid';

import { RootResourceApi, SessionResourceApi, WorkspaceResourceApi } from './client/apis';
import {
  LaunchRequest as ClientLaunchRequest,
  PingRequest as ClientPingRequest, SessionActivityRequest as ClientSessionActivityRequest, SessionLaunchResponse, SessionListRequest as ClientSessionListRequest,
  SessionSpec, SessionStartRequest as ClientSessionStartRequest, SessionStopRequest as ClientSessionStopRequest,
  UserWorkspace, WorkspaceCreationRequest as ClientWorkspaceCreationRequest, WorkspaceCreationResponse,
  WorkspaceDeletionRequest as ClientWorkspaceDeletionRequest, WorkspaceListRequest as ClientWorkspaceListRequest
} from './client/models';
import { Configuration } from './client/runtime';

export interface ServiceRequest {
  serviceUrl: string;
  kind?: string;
}

export type PingRequest = ClientPingRequest & ServiceRequest;
export namespace PingRequest {
  export const KIND = 'pingRequest';

  export function create(serviceUrl: string, appId: string): PingRequest {
    return { serviceUrl, appId };
  }
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
  export const KIND = 'sessionListRequest';
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

  export async function ping(request: PingRequest): Promise<void> {
    const fullRequest = { kind: PingRequest.KIND, ...request };
    try {
      await rootApi(request.serviceUrl).serviceAppIdGet(fullRequest, createConfig());
    } catch (error) {
      console.error((error as any).message);
      throw error;
    }
  }

  export async function launch(request: LaunchRequest, retries = 0): Promise<void> {
    const launchRequest = { kind: LaunchRequest.KIND, ...request };
    try {
      const response = await rootApi(request.serviceUrl).servicePost({ launchRequest }, createConfig());
      const sessionLaunch = response;
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
      const sessionListRequest = { kind: SessionListRequest.KIND, ...request };
      return sessionApi(request.serviceUrl).serviceSessionAppIdUserGet(sessionListRequest, createConfig());
    }

    export async function startSession(request: SessionStartRequest): Promise<SessionLaunchResponse> {
      const sessionStartRequest = { kind: SessionStartRequest.KIND, ...request };
      return sessionApi(request.serviceUrl).serviceSessionPost({ sessionStartRequest }, createConfig());
    }

    export async function stopSession(request: SessionStopRequest): Promise<boolean> {
      const sessionStopRequest = { kind: SessionStopRequest.KIND, ...request };
      return sessionApi(request.serviceUrl).serviceSessionDelete({ sessionStopRequest }, createConfig());
    }

    export async function reportSessionActivity(request: SessionActivityRequest): Promise<boolean> {
      const sessionActivityRequest = { kind: SessionActivityRequest.KIND, ...request };
      return sessionApi(request.serviceUrl).serviceSessionPatch({ sessionActivityRequest }, createConfig());
    }
  }

  export namespace Workspace {
    export async function listWorkspaces(request: WorkspaceListRequest): Promise<UserWorkspace[]> {
      const workspaceListRequest = { kind: WorkspaceListRequest.KIND, ...request };
      return workspaceApi(request.serviceUrl).serviceWorkspaceAppIdUserGet(workspaceListRequest, createConfig());
    }

    export async function createWorkspace(request: WorkspaceCreationRequest): Promise<WorkspaceCreationResponse> {
      const workspaceCreationRequest = { kind: WorkspaceCreationRequest.KIND, ...request };
      return workspaceApi(request.serviceUrl).serviceWorkspacePost({ workspaceCreationRequest }, createConfig());
    }

    export async function deleteWorkspace(request: WorkspaceDeletionRequest): Promise<boolean> {
      const workspaceDeletionRequest = { kind: WorkspaceDeletionRequest.KIND, ...request };
      return workspaceApi(request.serviceUrl).serviceWorkspaceDelete({ workspaceDeletionRequest }, createConfig());
    }
  }
}

function createUser(): string {
  return uuidv4() + '@theia.cloud';
}

function createConfig(): RequestInit {
  const controller = new AbortController();
  setTimeout(() => controller.abort(), 30000);
  return { signal: controller.signal };
}
