import axios from 'axios';
import { v4 as uuidv4 } from 'uuid';

import { ServiceRequest, ServiceResponse } from './service';

export interface UserSession {
  name: string;
  appDefinition: string;
  user: string;
  url: string;
  workspace: string;
  lastActivity: number;
}

export interface SessionStartRequest extends ServiceRequest {
  appDefinition: string;
  user: string;
  workspaceName?: string; /* no existing workspace means ephemeral storage */
  timeout?: number;
}
export namespace SessionStartRequest {
  export const KIND = 'sessionStartRequest';
}

export interface SessionLaunchResponse extends ServiceResponse {
  url: string;
}

export interface SessionListRequest extends ServiceRequest {
  user?: string;
}
export namespace SessionListRequest {
  export const KIND = 'sessionListRequest';
}

export interface SessionStopRequest extends ServiceRequest {
  user?: string;
  sessionName: string;
}
export namespace SessionStopRequest {
  export const KIND = 'sessionStopRequest';
}

export interface SessionActivityRequest extends ServiceRequest {
  sessionName: string;
}
export namespace SessionActivityRequest {
  export const KIND = 'sessionActivityRequest';
}

function toSessionServiceUrl(serviceUrl: string): string {
  return serviceUrl + '/session';
}

export async function listSessions(options: SessionListRequest): Promise<UserSession[]> {
  const { appId, serviceUrl, user = uuidv4() + '@theia.cloud', kind = SessionListRequest.KIND } = options;
  const sessionServiceUrl = toSessionServiceUrl(serviceUrl);
  console.log('Calling to ' + sessionServiceUrl);
  try {
    const response = await axios.get(
      sessionServiceUrl,
      {
        data: { user, appId, kind },
        timeout: 300000
      }
    );
    return response.data as UserSession[];
  } catch (error) {
    console.error((error as any).message);
    throw error;
  }
}

export async function startSession(options: SessionStartRequest): Promise<SessionLaunchResponse> {
  const { appId, serviceUrl, workspaceName, user = uuidv4() + '@theia.cloud', kind = SessionStartRequest.KIND, timeout } = options;
  const sessionServiceUrl = toSessionServiceUrl(serviceUrl);
  console.log('Calling to ' + sessionServiceUrl);
  try {
    const response = await axios.post(
      sessionServiceUrl,
      {
        data: { workspaceName, user, appId, kind, timeout },
        timeout: 300000
      }
    );
    return response.data;
  } catch (error) {
    console.error((error as any).message);
    throw error;
  }
}

export async function stopSession(options: SessionStopRequest): Promise<boolean> {
  const { appId, serviceUrl, sessionName, user = uuidv4() + '@theia.cloud', kind = SessionStopRequest.KIND } = options;
  const sessionServiceUrl = toSessionServiceUrl(serviceUrl);
  console.log('Calling to ' + sessionServiceUrl);
  try {
    const response = await axios.delete(
      sessionServiceUrl,
      {
        data: { sessionName, user, appId, kind },
        timeout: 300000
      }
    );
    return !!response.data;
  } catch (error) {
    console.error((error as any).message);
    throw error;
  }
}

export async function reportSessionActivity(options: SessionActivityRequest): Promise<boolean> {
  const { appId, serviceUrl, sessionName, kind = SessionActivityRequest.KIND } = options;
  const sessionServiceUrl = toSessionServiceUrl(serviceUrl);
  console.log('Calling to ' + sessionServiceUrl);
  try {
    const response = await axios.patch(
      sessionServiceUrl,
      {
        data: { sessionName, appId, kind },
        timeout: 300000
      }
    );
    return !!response.data;
  } catch (error) {
    console.error((error as any).message);
    throw error;
  }
}
