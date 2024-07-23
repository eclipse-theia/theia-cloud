# Keycloak Realm Setup

We are using oauth2-proxy as reverse proxy. The official setup instructions to use the Keycloak OIDC Auth Provider with oauth2-proxy can be found here: <https://oauth2-proxy.github.io/oauth2-proxy/docs/configuration/oauth_provider/#keycloak-oidc-auth-provider>

The main difference is that our client is not confidential but public, since we are also initiating the login process via the keycloak js library, which requests a public client.

- Add a new Realm in Keycloak called "TheiaCloud"
- Switch to the new Realm and create a new client with
  - client id: "theia-cloud"
  - client protocol: "openid-connect"
- On the settings tab of the new client add
  - \* under valid redirect URIs
    - _Please note:_ Don't use \* in production but use your actual URIs that need to be redirected too.
  - \* under Web Origins
    - _Please note:_ Don't use \* in production but use your actual allowed CORS Origins
- On the `Client Scopes` tab of the new client make sure `email` is part of the `Assigned Default Client Scopes`.
- On the mappers tab of the new client create two mappers
  - Group Mapper
    - Name: groups
    - Mapper Type: "Group Membership"
    - Token Claim Name: groups
    - Open the "Add to ID token"
    - Open the "Add to access token"
    - Open the "Add to userinfo"
    - Open the "Full group path"
  - Client Audience Mapper
    - Name: audience
    - Mapper Type: Audience
    - Included Client Audience: theia-cloud
    - Included Custom Audience: theia-cloud
    - Open the "Add to ID token"
    - Open the "Add to access token"
