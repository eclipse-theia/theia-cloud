import axios from 'axios';
import { v4 as uuidv4 } from 'uuid';

export interface WorkspaceOptions {
  workspaceServiceUrl: string;
  workspaceTemplate: string;
  appId: string;
  user?: string;
}

export function startWorkspace(options: WorkspaceOptions, retries = 0): Promise<void> {
  const { appId, workspaceServiceUrl, workspaceTemplate, user = uuidv4() + '@theia.cloud' } = options;
  console.log('Calling to ' + workspaceServiceUrl);
  return axios
    .post(
      workspaceServiceUrl,
      {
        template: workspaceTemplate,
        user: user,
        appId: appId
      },
      {
        timeout: 300000
      }
    )
    .then(
      response => {
        if (response.data.success) {
          console.log(`Redirect to: https://${response.data.url}`);
          location.replace(`https://${response.data.url}`);
        } else {
          console.error(response.data.error);
          throw new Error(`Could not launch workspace: ${response.data.error}`);
        }
      },
      error => {
        // Request timed out or returned an error with an error HTTP code.
        console.error(error.message);
        if (retries > 0) {
          startWorkspace(options, retries - 1);
        } else {
          throw error;
        }
      }
    );
}
