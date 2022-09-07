import { AxiosError, AxiosRequestConfig, AxiosResponse } from 'axios';
import { v4 as uuidv4 } from 'uuid';

import {
  LaunchRequest as ClientLaunchRequest,
  PingRequest as ClientPingRequest, RootResourceApi, SessionActivityRequest as ClientSessionActivityRequest, SessionListRequest as ClientSessionListRequest,
  SessionResourceApi, SessionSpec, SessionStartRequest as ClientSessionStartRequest, SessionStopRequest as ClientSessionStopRequest,
  UserWorkspace, WorkspaceCreationRequest as ClientWorkspaceCreationRequest,
  WorkspaceDeletionRequest as ClientWorkspaceDeletionRequest, WorkspaceListRequest as ClientWorkspaceListRequest,
  WorkspaceResourceApi
} from './client/api';
import { Configuration } from './client/configuration';

export const DEFAULT_CALL_TIMEOUT = 30000;
export const DEFAULT_CALL_RETRIES = 0;

export interface ServiceRequest {
  serviceUrl: string;
  kind?: string;
}
export namespace ServiceRequest {
  export function is(obj?: any): obj is ServiceRequest {
    return !!obj && typeof obj.serviceUrl === 'string';
  }
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

  export async function ping(request: PingRequest): Promise<boolean> {
    return call(() => rootApi(request.serviceUrl).serviceAppIdGet(request.appId, createConfig()));
  }

  export async function launch(request: LaunchRequest, timeout?: number, retries?: number): Promise<string> {
    const launchRequest = { kind: LaunchRequest.KIND, ...request };
    return call(() => rootApi(request.serviceUrl).servicePost(launchRequest, createConfig(timeout)), retries);
  }

  export async function launchAndRedirect(request: LaunchRequest, timeout?: number, retries?: number): Promise<string> {
    const url = await launch(request, timeout, retries);
    console.log(`Redirect to: https://${url}`);
    location.replace(`https://${url}`);
    return url;
  }

  export namespace Session {
    export async function listSessions(request: SessionListRequest, timeout?: number, retries?: number): Promise<SessionSpec[]> {
      return call(() => sessionApi(request.serviceUrl).serviceSessionAppIdUserGet(request.appId, request.user, createConfig(timeout)), retries);
    }

    export async function startSession(request: SessionStartRequest, timeout?: number, retries?: number): Promise<string> {
      const sessionStartRequest = { kind: SessionStartRequest.KIND, ...request };
      return call(() => sessionApi(request.serviceUrl).serviceSessionPost(sessionStartRequest, createConfig(timeout)), retries);
    }

    export async function stopSession(request: SessionStopRequest, timeout?: number, retries?: number): Promise<boolean> {
      const sessionStopRequest = { kind: SessionStopRequest.KIND, ...request };
      return call(() => sessionApi(request.serviceUrl).serviceSessionDelete(sessionStopRequest, createConfig(timeout)), retries);
    }

    export async function reportSessionActivity(request: SessionActivityRequest, timeout?: number, retries?: number): Promise<boolean> {
      const sessionActivityRequest = { kind: SessionActivityRequest.KIND, ...request };
      return call(() => sessionApi(request.serviceUrl).serviceSessionPatch(sessionActivityRequest, createConfig(timeout)), retries);
    }
  }

  export namespace Workspace {
    export async function listWorkspaces(request: WorkspaceListRequest, timeout?: number, retries?: number): Promise<UserWorkspace[]> {
      return call(() => workspaceApi(request.serviceUrl).serviceWorkspaceAppIdUserGet(request.appId, request.user, createConfig(timeout)), retries);
    }

    export async function createWorkspace(request: WorkspaceCreationRequest, timeout?: number, retries?: number): Promise<UserWorkspace> {
      const workspaceCreationRequest = { kind: WorkspaceCreationRequest.KIND, ...request };
      return call(() => workspaceApi(request.serviceUrl).serviceWorkspacePost(workspaceCreationRequest, createConfig(timeout)), retries);
    }

    export async function deleteWorkspace(request: WorkspaceDeletionRequest, timeout?: number, retries?: number): Promise<boolean> {
      const workspaceDeletionRequest = { kind: WorkspaceDeletionRequest.KIND, ...request };
      return call(() => workspaceApi(request.serviceUrl).serviceWorkspaceDelete(workspaceDeletionRequest, createConfig(timeout)), retries);
    }
  }
}

function createUser(): string {
  return uuidv4() + '@theia.cloud';
}

function createConfig(timeout = DEFAULT_CALL_TIMEOUT): AxiosRequestConfig {
  return { timeout };
}

async function call<T = any>(fn: () => Promise<AxiosResponse<T>>, retries = DEFAULT_CALL_RETRIES): Promise<T> {
  try {
    const response = await fn();
    return response.data;
  } catch (err) {
    const error = err as Error;
    console.error(error.message);
    if (retries > 0) {
      return call(fn, retries - 1);
    }
    throw TheiaCloudError.from(error);
  }
}

export interface TheiaCloudErrorResponse {
  code: number;
  reason: string;
}
export namespace TheiaCloudErrorResponse {
  export function is(obj?: any): obj is TheiaCloudErrorResponse {
    return !!obj && typeof obj.code === 'number' && typeof obj.reason === 'string';
  }
}

export class TheiaCloudError extends Error {
  static INTERNAL_ERROR = 500;

  constructor(message: string, public status: number, public originalError: Error, public serverError?: TheiaCloudErrorResponse, public request?: ServiceRequest) {
    super(message);
  }

  static from(error: Error): TheiaCloudError {
    if (error instanceof AxiosError) {
      const responseData = error.response ? error.response.data : undefined;
      const errorResponse = TheiaCloudErrorResponse.is(responseData) ? responseData : undefined;
      const message = errorResponse ? errorResponse.reason : error.message;

      const requestData = typeof error.config.data === 'string' ? JSON.parse(error.config.data) : error.config.data;
      const serviceRequest = ServiceRequest.is(requestData) ? requestData : undefined;

      const status = error.status ? parseInt(error.status, 10)
        : error.response ? error.response.status
          : errorResponse ? errorResponse.code
            : TheiaCloudError.INTERNAL_ERROR;
      return new TheiaCloudError(message, status, error, errorResponse, serviceRequest);
    }
    return new TheiaCloudError(error.message, TheiaCloudError.INTERNAL_ERROR, error);
  }
}
