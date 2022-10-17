export namespace TheiaCloudConfig {
  export function is(thing: any): thing is TheiaCloudConfig {
    return (
      !!thing &&
      typeof thing === 'object' &&
      typeof thing.appId === 'string' &&
      typeof thing.appName === 'string' &&
      typeof thing.serviceUrl === 'string' &&
      typeof thing.appDefinition === 'string' &&
      typeof thing.useKeycloak === 'boolean' &&
      typeof thing.useEphemeralStorage === 'boolean'
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
  useKeycloak: boolean;
  serviceUrl: string;
  appDefinition: string;
  useEphemeralStorage: boolean;
  additionalApps?: AppDefinition[]
}
export interface AppDefinition {
  appId: string;
  appName: string;
}

export interface KeycloakConfig {
  keycloakAuthUrl: string;
  keycloakRealm: string;
  keycloakClientId: string;
}

export type TheiaCloudConfig = AppDefinition & BaseTheiaCloudConfig & Partial<KeycloakConfig>;

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
