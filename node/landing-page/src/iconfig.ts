export interface TheiaCloudConfig {
  appId: string;
  useKeycloak: boolean;
  keycloakAuthUrl?: string;
  keycloakRealm?: string;
  keycloakClientId?: string;
  workspaceServiceUrl: string;
  workspaceTemplate: string;
}
