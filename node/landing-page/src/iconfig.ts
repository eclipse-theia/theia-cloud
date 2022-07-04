export interface TheiaCloudConfig {
  appId: string;
  appName: string;
  useKeycloak: boolean;
  keycloakAuthUrl?: string;
  keycloakRealm?: string;
  keycloakClientId?: string;
  serviceUrl: string;
  appDefinition: string;
}
