import React, { useEffect, useState } from 'react';
import './App.css';
import { KeycloakConfig } from 'keycloak-js';
import Keycloak from 'keycloak-js';
import {
  TheiaCloud,
  RequestOptions,
  SessionListRequest,
  SessionStartRequest,
  SessionStopRequest,
  WorkspaceCreationRequest,
  WorkspaceDeletionRequest,
  WorkspaceListRequest,
  PingRequest,
  LaunchRequest,
  SessionPerformanceRequest,
  SessionSetConfigValueRequest
} from '@eclipse-theiacloud/common';

const KEYCLOAK_CONFIG: KeycloakConfig = {
  url: 'https://keycloak.localdemo.io/',
  realm: 'TheiaCloud',
  clientId: 'theia-cloud'
};
// The base URL of the service
const SERVICE_URL = 'https://service.localdemo.io';

const APP_DEFINITION = 'theia-cloud-demo';
const SERVICE_AUTH_TOKEN = 'asdfghjkl';

function App() {
  const [token, setToken] = useState<string>();
  const [logoutUrl, setLogoutUrl] = useState<string>();
  const [email, setEmail] = useState<string>();
  const [user, setUser] = useState('');
  const [resourceName, setResourceName] = useState('');
  const [configKey, setConfigKey] = useState('');
  const [configValue, setConfigValue] = useState('');

  useEffect(() => {
    const keycloak = Keycloak(KEYCLOAK_CONFIG);
    keycloak
      .init({
        onLoad: 'check-sso',
        redirectUri: window.location.href,
        checkLoginIframe: false
      })
      .then(auth => {
        if (auth) {
          const parsedToken = keycloak.idTokenParsed;
          if (parsedToken) {
            const userMail = parsedToken.email;
            setToken(keycloak.idToken);
            setEmail(userMail);
            setLogoutUrl(keycloak.createLogoutUrl());
          }
        }
      })
      .catch(() => {
        console.error('Authentication Failed');
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const login = () => {
    const keycloak = Keycloak(KEYCLOAK_CONFIG);
    keycloak
      .init({
        onLoad: 'login-required',
        redirectUri: window.location.href,
        checkLoginIframe: false
      })
      .catch(() => {
        console.error('Authentication Failed');
      });
  };

  const executeRequest = async (requestFn: RequestFunction): Promise<any> => {
    if (!token) {
      console.warn('No token. Request is anonymous.');
    }
    return requestFn(user ? user : email!, token!)
      .then(val => {
        console.log('Request successful with result:', val);
      })
      .catch(err => {
        console.error('Request failed:', err);
      });
  };

  // Root requests
  const ping = (_user: string, accessToken?: string) => {
    const request: PingRequest = {
      appId: SERVICE_AUTH_TOKEN,
      serviceUrl: SERVICE_URL
    };
    return TheiaCloud.ping(request, generateRequestOptions(accessToken));
  };
  const launch = (user: string, accessToken?: string) => {
    const request: LaunchRequest = {
      appId: SERVICE_AUTH_TOKEN,
      appDefinition: APP_DEFINITION,
      user,
      serviceUrl: SERVICE_URL,
      timeout: 3
    };
    return TheiaCloud.launch(request, generateRequestOptions(accessToken));
  };

  // Workspace requests
  const listWorkspaces = (user: string, accessToken?: string) => {
    const request: WorkspaceListRequest = {
      appId: SERVICE_AUTH_TOKEN,
      user,
      serviceUrl: SERVICE_URL
    };
    return TheiaCloud.Workspace.listWorkspaces(request, generateRequestOptions(accessToken));
  };
  const createWorkspace = (user: string, accessToken?: string) => {
    const request: WorkspaceCreationRequest = {
      appId: SERVICE_AUTH_TOKEN,
      appDefinition: APP_DEFINITION,
      user,
      serviceUrl: SERVICE_URL
    };
    return TheiaCloud.Workspace.createWorkspace(request, generateRequestOptions(accessToken));
  };
  const deleteWorkspace = (user: string, accessToken?: string) => {
    const request: WorkspaceDeletionRequest = {
      appId: SERVICE_AUTH_TOKEN,
      user,
      workspaceName: resourceName,
      serviceUrl: SERVICE_URL
    };
    return TheiaCloud.Workspace.deleteWorkspace(request, generateRequestOptions(accessToken));
  };

  // Session requests
  const listSessions = (user: string, accessToken?: string) => {
    const request: SessionListRequest = {
      appId: SERVICE_AUTH_TOKEN,
      user,
      serviceUrl: SERVICE_URL
    };
    return TheiaCloud.Session.listSessions(request, generateRequestOptions(accessToken));
  };
  const startSession = (user: string, accessToken?: string) => {
    const request: SessionStartRequest = {
      appId: SERVICE_AUTH_TOKEN,
      appDefinition: APP_DEFINITION,
      user,
      workspaceName: resourceName ? resourceName : undefined,
      serviceUrl: SERVICE_URL
    };
    return TheiaCloud.Session.startSession(request, generateRequestOptions(accessToken));
  };
  const stopSession = (user: string, accessToken?: string) => {
    const request: SessionStopRequest = {
      appId: SERVICE_AUTH_TOKEN,
      user,
      sessionName: resourceName,
      serviceUrl: SERVICE_URL
    };
    return TheiaCloud.Session.stopSession(request, generateRequestOptions(accessToken));
  };
  const reportSessionPerformance = (user: string, accessToken?: string) => {
    const request: SessionPerformanceRequest = {
      appId: SERVICE_AUTH_TOKEN,
      sessionName: resourceName,
      serviceUrl: SERVICE_URL
    };
    return TheiaCloud.Session.getSessionPerformance(request, generateRequestOptions(accessToken));
  };
  const setSessionConfigValue = (user: string, accessToken?: string) => {
    const request: SessionSetConfigValueRequest = {
      appId: SERVICE_AUTH_TOKEN,
      key: configKey,
      value: configValue,
      serviceUrl: SERVICE_URL
    };
    return TheiaCloud.Session.setConfigValue(resourceName, request, generateRequestOptions(accessToken));
  };

  // App definition requests
  const listAppDefinitions = (user: string, accessToken?: string) => {
    const request = {
      appId: SERVICE_AUTH_TOKEN,
      user,
      serviceUrl: SERVICE_URL
    };
    return TheiaCloud.AppDefinition.listAppDefinitions(request, generateRequestOptions(accessToken));
  };

  return (
    <div className='App'>
      <h1>TheiaCloud Service Test Page</h1>
      <p>
        <strong>This page is meant for internal testing only!</strong>
      </p>
      <p>Open your browser's dev tools (F12) to see outgoing requests. Results are logged to the console as well.</p>
      {email ? <p>Logged in as: {email}</p> : <button onClick={login}>Login via Keycloak</button>}
      {logoutUrl && <a href={logoutUrl}>Logout</a>}
      <p>Service Auth Token: {SERVICE_AUTH_TOKEN}</p>
      <p>Service URL: {SERVICE_URL}</p>
      <p>
        <span>User:</span>
        <input type='text' value={user} onChange={ev => setUser(ev.target.value)} />
      </p>
      <p>
        <span>Session/Workspace:</span>
        <input type='text' value={resourceName} onChange={ev => setResourceName(ev.target.value)} />
      </p>
      <p>
        <button onClick={() => executeRequest(ping)}>Ping</button>
        <button onClick={() => executeRequest(launch)}>Launch</button>
      </p>
      <p>
        <button onClick={() => executeRequest(listSessions)}>List Sessions</button>
        <button onClick={() => executeRequest(startSession)}>Start Session</button>
        <button onClick={() => executeRequest(stopSession)}>Stop Session</button>
        <button onClick={() => executeRequest(reportSessionPerformance)}>Report Session Performance</button>
      </p>
      <div>
        <p>Session Key Value Config:</p>
        <p>
          <span>Key:</span>
          <input type='text' value={configKey} onChange={ev => setConfigKey(ev.target.value)} />
        </p>
        <p>
          <span>Value:</span>
          <input type='text' value={configValue} onChange={ev => setConfigValue(ev.target.value)} />
        </p>
        <button onClick={() => executeRequest(setSessionConfigValue)}>Set Config Value</button>
      </div>
      <p>
        <button onClick={() => executeRequest(listWorkspaces)}>List Workspaces</button>
        <button onClick={() => executeRequest(createWorkspace)}>Create Workspace</button>
        <button onClick={() => executeRequest(deleteWorkspace)}>Delete Workspace</button>
      </p>
      <p>
        <button onClick={() => executeRequest(listAppDefinitions)}>List AppDefinitions</button>
      </p>
    </div>
  );
}

type RequestFunction = (user: string, token: string | undefined) => Promise<any>;

function generateRequestOptions(token: string | undefined): RequestOptions {
  return {
    accessToken: token
  };
}
export default App;
