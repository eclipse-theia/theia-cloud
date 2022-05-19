export namespace TheiaCloudConfig {
  export function is(thing: any): thing is TheiaCloudConfig {
    return (
      !!thing &&
      typeof thing === 'object' &&
      typeof thing.appId === 'string' &&
      typeof thing.workspaceServiceUrl === 'string' &&
      typeof thing.workspaceTemplate === 'string' &&
      typeof thing.useKeycloak === 'boolean'
    );
  }
}

export namespace KeycloakConfig {
  export function is(thing: any): thing is KeycloakConfig {
    return (
      !!thing &&
      typeof thing === 'object' &&
      typeof thing.keycloakAuthUrl === 'string' &&
      typeof thing.keycloakRealm === 'string' &&
      typeof thing.keycloakClientId === 'string'
    );
  }
}

interface BaseTheiaCloudConfig {
  appId: string;
  useKeycloak: boolean;
  workspaceServiceUrl: string;
  workspaceTemplate: string;
}

export interface KeycloakConfig {
  keycloakAuthUrl: string;
  keycloakRealm: string;
  keycloakClientId: string;
}

export type TheiaCloudConfig = BaseTheiaCloudConfig & Partial<KeycloakConfig>;

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
