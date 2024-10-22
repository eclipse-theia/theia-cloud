/* eslint-disable indent */
import { AxiosError, AxiosRequestConfig, AxiosResponse } from 'axios';
import { v4 as uuidv4 } from 'uuid';

import {
  AppDefinitionListRequest as ClientAppDefinitionListRequest,
  AppDefinitionResourceApi,
  AppDefinitionSpec,
  LaunchRequest as ClientLaunchRequest,
  PingRequest as ClientPingRequest,
  RootResourceApi,
  SessionActivityRequest as ClientSessionActivityRequest,
  SessionListRequest as ClientSessionListRequest,
  SessionPerformance,
  SessionPerformanceRequest as ClientSessionPerformanceRequest,
  SessionResourceApi,
  SessionSpec,
  SessionStartRequest as ClientSessionStartRequest,
  SessionStopRequest as ClientSessionStopRequest,
  UserWorkspace,
  WorkspaceCreationRequest as ClientWorkspaceCreationRequest,
  WorkspaceDeletionRequest as ClientWorkspaceDeletionRequest,
  WorkspaceListRequest as ClientWorkspaceListRequest,
  WorkspaceResourceApi
} from './client/api';
import { Configuration } from './client/configuration';

export const DEFAULT_CALL_TIMEOUT = 30000;
export const DEFAULT_CALL_RETRIES = 0;

export interface RequestOptions {
  accessToken?: string;
  retries?: number;
  timeout?: number;
}

export interface ServiceRequest {
  /** The root URL of the service. */
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

  export function ephemeral(
    serviceUrl: string,
    appId: string,
    appDefinition: string,
    timeout?: number,
    user: string = createUser()
  ): LaunchRequest {
    return { serviceUrl, appId, appDefinition, user, ephemeral: true, timeout };
  }

  export function createWorkspace(
    serviceUrl: string,
    appId: string,
    appDefinition: string,
    timeout?: number,
    user: string = createUser(),
    workspaceName?: string,
    label?: string
  ): LaunchRequest {
    return { serviceUrl, appId, appDefinition, user, label, workspaceName, ephemeral: false, timeout };
  }

  // eslint-disable-next-line max-len
  export function existingWorkspace(
    serviceUrl: string,
    appId: string,
    workspaceName: string,
    timeout?: number,
    appDefinition?: string,
    user: string = createUser()
  ): LaunchRequest {
    return { serviceUrl, appId, workspaceName, appDefinition, user, timeout };
  }
}

export type AppDefinitionListRequest = ClientAppDefinitionListRequest & ServiceRequest;
export namespace AppDefinitionListRequest {
  export const KIND = 'appDefinitionListRequest';
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

export type SessionPerformanceRequest = ClientSessionPerformanceRequest & ServiceRequest;
export namespace SessionPerformanceRequest {
  export const KIND = 'sessionPerformanceRequest';
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
  function rootApi(serviceUrl: string, accessToken: string | undefined): RootResourceApi {
    return new RootResourceApi(new Configuration({ basePath: serviceUrl, accessToken }));
  }

  function appDefinitionApi(serviceUrl: string, accessToken: string | undefined): AppDefinitionResourceApi {
    return new AppDefinitionResourceApi(new Configuration({ basePath: serviceUrl, accessToken }));
  }

  function sessionApi(serviceUrl: string, accessToken: string | undefined): SessionResourceApi {
    return new SessionResourceApi(new Configuration({ basePath: serviceUrl, accessToken }));
  }

  function workspaceApi(serviceUrl: string, accessToken: string | undefined): WorkspaceResourceApi {
    return new WorkspaceResourceApi(new Configuration({ basePath: serviceUrl, accessToken }));
  }

  export async function ping(request: PingRequest, options: RequestOptions = {}): Promise<boolean> {
    const { accessToken, retries, timeout } = options;
    return call(
      () => rootApi(request.serviceUrl, accessToken).serviceAppIdGet(request.appId, createConfig(timeout)),
      retries
    );
  }

  export async function launch(request: LaunchRequest, options: RequestOptions = {}): Promise<string> {
    const { accessToken, retries, timeout } = options;
    const launchRequest = { kind: LaunchRequest.KIND, ...request };
    return call(
      () => rootApi(request.serviceUrl, accessToken).servicePost(launchRequest, createConfig(timeout)),
      retries
    );
  }

  export async function launchAndRedirect(request: LaunchRequest, options: RequestOptions = {}): Promise<string> {
    const url = await launch(request, options);
    console.log(`Redirect to: https://${url}`);
    location.replace(`https://${url}`);
    return url;
  }

  export namespace AppDefinition {
    export async function listAppDefinitions(
      request: AppDefinitionListRequest,
      options: RequestOptions = {}
    ): Promise<AppDefinitionSpec[]> {
      const { accessToken, retries, timeout } = options;
      return call(
        () =>
          appDefinitionApi(request.serviceUrl, accessToken).serviceAppdefinitionAppIdGet(
            request.appId,
            createConfig(timeout)
          ),
        retries
      );
    }
  }

  export namespace Session {
    export async function listSessions(
      request: SessionListRequest,
      options: RequestOptions = {}
    ): Promise<SessionSpec[]> {
      const { accessToken, retries, timeout } = options;
      return call(
        () =>
          sessionApi(request.serviceUrl, accessToken).serviceSessionAppIdUserGet(
            request.appId,
            request.user,
            createConfig(timeout)
          ),
        retries
      );
    }

    export async function startSession(request: SessionStartRequest, options: RequestOptions = {}): Promise<string> {
      const { accessToken, retries, timeout } = options;
      const sessionStartRequest = { kind: SessionStartRequest.KIND, ...request };
      return call(
        () =>
          sessionApi(request.serviceUrl, accessToken).serviceSessionPost(sessionStartRequest, createConfig(timeout)),
        retries
      );
    }

    export async function stopSession(request: SessionStopRequest, options: RequestOptions = {}): Promise<boolean> {
      const { accessToken, retries, timeout } = options;
      const sessionStopRequest = { kind: SessionStopRequest.KIND, ...request };
      return call(
        () =>
          sessionApi(request.serviceUrl, accessToken).serviceSessionDelete(sessionStopRequest, createConfig(timeout)),
        retries
      );
    }

    export async function reportSessionActivity(
      request: SessionActivityRequest,
      options: RequestOptions = {}
    ): Promise<boolean> {
      const { accessToken, retries, timeout } = options;
      const sessionActivityRequest = { kind: SessionActivityRequest.KIND, ...request };
      return call(
        () =>
          sessionApi(request.serviceUrl, accessToken).serviceSessionPatch(
            sessionActivityRequest,
            createConfig(timeout)
          ),
        retries
      );
    }

    export async function getSessionPerformance(
      request: SessionPerformanceRequest,
      options: RequestOptions = {}
    ): Promise<SessionPerformance> {
      const { accessToken, retries, timeout } = options;
      return call(
        () =>
          sessionApi(request.serviceUrl, accessToken).serviceSessionPerformanceAppIdSessionNameGet(
            request.appId,
            request.sessionName,
            createConfig(timeout)
          ),
        retries
      );
    }
  }

  export namespace Workspace {
    export async function listWorkspaces(
      request: WorkspaceListRequest,
      options: RequestOptions = {}
    ): Promise<UserWorkspace[]> {
      const { accessToken, retries, timeout } = options;
      return call(
        () =>
          workspaceApi(request.serviceUrl, accessToken).serviceWorkspaceAppIdUserGet(
            request.appId,
            request.user,
            createConfig(timeout)
          ),
        retries
      );
    }

    export async function createWorkspace(
      request: WorkspaceCreationRequest,
      options: RequestOptions = {}
    ): Promise<UserWorkspace> {
      const { accessToken, retries, timeout } = options;
      const workspaceCreationRequest = { kind: WorkspaceCreationRequest.KIND, ...request };
      return call(
        () =>
          workspaceApi(request.serviceUrl, accessToken).serviceWorkspacePost(
            workspaceCreationRequest,
            createConfig(timeout)
          ),
        retries
      );
    }

    export async function deleteWorkspace(
      request: WorkspaceDeletionRequest,
      options: RequestOptions = {}
    ): Promise<boolean> {
      const { accessToken, retries, timeout } = options;
      const workspaceDeletionRequest = { kind: WorkspaceDeletionRequest.KIND, ...request };
      return call(
        () =>
          workspaceApi(request.serviceUrl, accessToken).serviceWorkspaceDelete(
            workspaceDeletionRequest,
            createConfig(timeout)
          ),
        retries
      );
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

  constructor(
    message: string,
    public status: number,
    public originalError: Error,
    public serverError?: TheiaCloudErrorResponse,
    public request?: ServiceRequest
  ) {
    super(message);
  }

  static from(error: Error): TheiaCloudError {
    if (error instanceof AxiosError) {
      const responseData = error.response ? error.response.data : undefined;
      const errorResponse = TheiaCloudErrorResponse.is(responseData) ? responseData : undefined;
      const message = errorResponse ? errorResponse.reason : error.message;

      const status = error.status
        ? error.status
        : error.response
        ? error.response.status
        : errorResponse
        ? errorResponse.code
        : TheiaCloudError.INTERNAL_ERROR;

      if (error.config === undefined) {
        return new TheiaCloudError(message, status, error, errorResponse);
      } else {
        const requestData = typeof error.config.data === 'string' ? JSON.parse(error.config.data) : error.config.data;
        const serviceRequest = ServiceRequest.is(requestData) ? requestData : undefined;
        return new TheiaCloudError(message, status, error, errorResponse, serviceRequest);
      }
    }
    return new TheiaCloudError(error.message, TheiaCloudError.INTERNAL_ERROR, error);
  }
}
