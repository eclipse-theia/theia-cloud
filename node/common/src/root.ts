import axios from 'axios';
import { v4 as uuidv4 } from 'uuid';
import { ServiceRequest } from './service';
import { SessionLaunchResponse } from './session';

export interface LaunchExistingWorkspaceSessionRequest extends ServiceRequest {
    appDefinition?: string;
    user?: string;
    workspaceName: string;
}

export interface LaunchEphemeralSessionRequest extends ServiceRequest {
    appDefinition: string;
    user?: string;
    ephemeral: boolean;
}

export interface LaunchSessionWithNewWorkspaceRequest extends ServiceRequest {
  appDefinition: string;
  user?: string;
  label?: string;
  ephemeral: boolean;
}

export type LaunchRequest = LaunchExistingWorkspaceSessionRequest | LaunchEphemeralSessionRequest | LaunchSessionWithNewWorkspaceRequest;
export namespace LaunchRequest {
    export const KIND = 'launchRequest';

    export function ephemeral(serviceUrl: string, appId: string, appDefinition: string, user?: string): LaunchEphemeralSessionRequest {
      return { serviceUrl, appId, appDefinition, user, ephemeral: true };
    }

    export function createWorkspace(serviceUrl: string, appId: string, appDefinition: string, user?: string, label?: string): LaunchSessionWithNewWorkspaceRequest {
      return { serviceUrl, appId, appDefinition, user, label, ephemeral: false };
    }

    export function existingWorkspace(serviceUrl: string, appId: string, workspaceName: string, appDefinition?: string, user?: string): LaunchExistingWorkspaceSessionRequest {
      return { serviceUrl, appId, workspaceName, appDefinition, user };
    }
}

export async function launch(options: LaunchRequest, retries = 0): Promise<void> {
  const request = { ...options, kind: options.kind ?? LaunchRequest.KIND, user: options.user ?? uuidv4() + '@theia.cloud' };
  console.log('Calling to ' + options.serviceUrl);
  try {
    const response = await axios.post(
      options.serviceUrl, request,
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
      launch(options, retries - 1);
    } else {
      throw error;
    }
  }
}
