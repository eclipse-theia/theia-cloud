export namespace TheiaCloudConfig {
  export function is(thing: any): thing is TheiaCloudConfig {
    return (
      !!thing &&
      typeof thing === 'object' &&
      typeof thing.appId === 'string' &&
      typeof thing.workspaceServiceUrl === 'string' &&
      typeof thing.workspaceTemplate === 'string'
    );
  }
}
export interface TheiaCloudConfig {
  appId: string;
  useKeycloak: boolean;
  keycloakAuthUrl?: string;
  keycloakRealm?: string;
  keycloakClientId?: string;
  workspaceServiceUrl: string;
  workspaceTemplate: string;
}

declare global {
  interface Window {
    theiaCloudConfig: TheiaCloudConfig;
  }
}

export function getTheiaCloudConfig(): TheiaCloudConfig | undefined {
  const config = window.theiaCloudConfig;
  if (TheiaCloudConfig.is(config)) {
    return Object.freeze({ ...config });
  }
  return undefined;
}
