import axios from 'axios';
import { v4 as uuidv4 } from 'uuid';

export interface SessionOptions {
  serviceUrl: string;
  appDefinition: string;
  appId: string;
  user?: string;
}

export function startSession(options: SessionOptions, retries = 0): Promise<void> {
  const { appId, serviceUrl, appDefinition, user = uuidv4() + '@theia.cloud' } = options;
  console.log('Calling to ' + serviceUrl);
  return axios
    .post(
      serviceUrl,
      {
        appDefinition: appDefinition,
        user: user,
        appId: appId,
        startupTimeout: 3
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
          throw new Error(`Could not launch session: ${response.data.error}`);
        }
      },
      error => {
        // Request timed out or returned an error with an error HTTP code.
        console.error(error.message);
        if (retries > 0) {
          startSession(options, retries - 1);
        } else {
          throw error;
        }
      }
    );
}
