import axios from 'axios';
import { v4 as uuidv4 } from 'uuid';

import { ServiceRequest } from './service';

export interface UserSession {
  name: string;
  appDefinition: string;
  user: string;
  url: string;
  workspace: string;
  lastActivity: number;
}

export interface SessionLaunchRequest extends ServiceRequest {
  appDefinition: string;
  user?: string;
  label?: string;
}

export interface SessionLaunchResponse {
  success: boolean;
  error: string;
  url: string;
}

export interface SessionsListRequest extends ServiceRequest {
  user?: string;
}

export interface SessionStopRequest extends ServiceRequest {
  user?: string;
  sessionName: string;
}

export interface SessionStartRequest extends ServiceRequest {
  user?: string;
  workspaceName: string;
}

export interface SessionActivityRequest extends ServiceRequest {
  sessionName: string;
}

function toSessionServiceUrl(serviceUrl: string): string {
  return serviceUrl + '/session';
}

export async function createAndLaunchSession(options: SessionLaunchRequest, retries = 0): Promise<void> {
  const { appId, serviceUrl, appDefinition, user = uuidv4() + '@theia.cloud' } = options;
  console.log('Calling to ' + serviceUrl);
  try {
    const response = await axios.post(
      serviceUrl,
      { appDefinition, user, appId },
      { timeout: 300000 }
    );
    const sessionLaunch = response.data as SessionLaunchResponse;
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
      createAndLaunchSession(options, retries - 1);
    } else {
      throw error;
    }
  }
}

export async function listSessions(options: SessionsListRequest): Promise<UserSession[]> {
  const { appId, serviceUrl, user = uuidv4() + '@theia.cloud' } = options;
  const sessionServiceUrl = toSessionServiceUrl(serviceUrl);
  console.log('Calling to ' + sessionServiceUrl);
  try {
    const response = await axios.get(
      sessionServiceUrl,
      {
        data: { user, appId },
        timeout: 300000
      }
    );
    return response.data as UserSession[];
  } catch (error) {
    console.error((error as any).message);
    throw error;
  }
}

export async function startSession(options: SessionStartRequest): Promise<boolean> {
  const { appId, serviceUrl, workspaceName, user = uuidv4() + '@theia.cloud' } = options;
  const sessionServiceUrl = toSessionServiceUrl(serviceUrl);
  console.log('Calling to ' + sessionServiceUrl);
  try {
    const response = await axios.post(
      sessionServiceUrl,
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

export async function stopSession(options: SessionStopRequest): Promise<boolean> {
  const { appId, serviceUrl, sessionName, user = uuidv4() + '@theia.cloud' } = options;
  const sessionServiceUrl = toSessionServiceUrl(serviceUrl);
  console.log('Calling to ' + sessionServiceUrl);
  try {
    const response = await axios.delete(
      sessionServiceUrl,
      {
        data: { sessionName, user, appId },
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
  const { appId, serviceUrl, sessionName } = options;
  const sessionServiceUrl = toSessionServiceUrl(serviceUrl);
  console.log('Calling to ' + sessionServiceUrl);
  try {
    const response = await axios.patch(
      sessionServiceUrl,
      {
        data: { sessionName, appId },
        timeout: 300000
      }
    );
    return !!response.data;
  } catch (error) {
    console.error((error as any).message);
    throw error;
  }
}
