export interface TheiaCloudConfig {
  appId: string;
  appName: string;
  useKeycloak: boolean;
  keycloakAuthUrl?: string;
  keycloakRealm?: string;
  keycloakClientId?: string;
  workspaceServiceUrl: string;
  workspaceTemplate: string;
}
