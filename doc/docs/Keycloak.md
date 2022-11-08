# Keycloak

This document contains additional information on configuring the installed [Keycloak](https://www.keycloak.org/) instance for Theia.Cloud.

## Keycloak Realm Setup

We are using oauth2-proxy as reverse proxy. The official setup instructions to use the Keycloak OIDC Auth Provider with oauth2-proxy can be found here: https://oauth2-proxy.github.io/oauth2-proxy/docs/configuration/oauth_provider/#keycloak-oidc-auth-provider

The main difference is that our client is not confidential but public, since we are also initiating the login process via the keycloak js library, which requests a public client.

* Add a new Realm in Keycloak called "TheiaCloud"
* Switch to the new Realm and create a new client with
  * client id: "theia-cloud"
  * client protocol: "openid-connect"
* On the settings tab of the new client add
  * \* under valid redirect URIs
    * *Please note:* Don't use * in production but use your actual URIs that need to be redirected too.
  * \* under Web Origins
    * *Please note:* Don't use * in production but use your actual allowed CORS Origins
* On the mappers tab of the new client create two mappers
  * Group Mapper
    * Name: groups
    * Mapper Type: "Group Membership"
    * Token Claim Name: groups
  * Client Audience Mapper
    * Name: audience
    * Mapper Type: Audience
    * Included Client Audience: theia-cloud
    * Included Custom Audience: theia-cloud

## Add users

Go to the keycloak admin console and log in with your admin credentials.
In the top left, select the `TheiaCloud` realm.
Go to `Manage -> Users` in the left panel and select `Add user`.
Set

* Username, e.g. `foo`
* Email, e.g. `foo@theia-cloud.io`
* Email verified to On

Select `Save` and go to `Credentials` Tab.
Set

* Password
* Password Confirmation
* Temporary to OFF

Select `Set Password`.
