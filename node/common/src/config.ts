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

/** Configures additional, optional properties for the landing page. */
export interface LandingPageConfig {
  additionalApps: AppDefinition[];
  disableInfo: boolean;
  infoTitle: string;
  infoText: string;
  loadingText: string;
  logoFileExtension: string;
}

export type TheiaCloudConfig = AppDefinition &
  BaseTheiaCloudConfig &
  Partial<KeycloakConfig> &
  Partial<LandingPageConfig>;

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
