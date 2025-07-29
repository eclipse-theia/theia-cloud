export namespace TheiaCloudConfig {
  export function is(thing: any): thing is TheiaCloudConfig {
    return (
      !!thing &&
      typeof thing === 'object' &&
      (typeof thing.appId === 'string' || typeof thing.serviceAuthToken === 'string') &&
      (typeof thing.appName === 'string' || typeof thing.serviceAuthToken === 'string') &&
      typeof thing.serviceUrl === 'string' &&
      typeof thing.appDefinition === 'string' &&
      typeof thing.useKeycloak === 'boolean' &&
      typeof thing.useEphemeralStorage === 'boolean'
    );
  }
  
  export function getServiceAuthToken(config: TheiaCloudConfig): string {
    if ('serviceAuthToken' in config && config.serviceAuthToken) {
      return config.serviceAuthToken;
    }
    if ('appId' in config && config.appId) {
      console.warn('Using deprecated property \'appId\'. Please migrate to \'serviceAuthToken\' in your configuration.');
      return config.appId;
    }
    throw new Error('Neither serviceAuthToken nor appId found in configuration');
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
  /** @deprecated Use serviceAuthToken instead */
  appId: string;
  appName: string;
}

export interface ServiceConfig {
  serviceAuthToken: string;
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

export type TheiaCloudConfig = (
  | (AppDefinition & Partial<ServiceConfig>)
  | (Pick<AppDefinition, 'appName'> & ServiceConfig)
) &
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
