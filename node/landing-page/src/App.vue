<template>
  <img alt="Theia logo" src="/logo.png" />
  <SessionLauncher
    :serviceUrl="serviceUrl"
    :appDefinition="appDefinition"
    :email="email"
    :appId="appId"
    :useEphemeralStorage="useEphemeralStorage"
    :workspaceName="workspaceName"
    :token="token"
  />
</template>

<script lang="ts">
import { defineComponent } from 'vue';
import SessionLauncher from './components/SessionLauncher.vue';
import Keycloak, { KeycloakConfig } from 'keycloak-js';
import { v4 as uuidv4 } from 'uuid';

interface AppData {
  email: string | undefined;
  token: string | undefined;
  workspaceName?: string;
}

export default defineComponent({
  name: 'App',
  props: {
    appId: String,
    appName: String,
    useKeycloak: Boolean,
    keycloakAuthUrl: String,
    keycloakRealm: String,
    keycloakClientId: String,
    serviceUrl: String,
    appDefinition: String,
    useEphemeralStorage: Boolean
  },
  components: {
    SessionLauncher
  },
  data() {
    return {
      email: undefined,
      token: undefined,
      workspaceName: undefined
    } as AppData;
  },
  created() {
    document.title = this.appName ?? 'Theia.cloud';
    if (this.useKeycloak) {
      const keycloakConfig: KeycloakConfig = {
        url: this.keycloakAuthUrl,
        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
        realm: this.keycloakRealm!,
        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
        clientId: this.keycloakClientId!
      };

      const keycloak = Keycloak(keycloakConfig);

      keycloak
        .init({
          onLoad: 'login-required',
          redirectUri: window.location.href,
          checkLoginIframe: false
        })
        .then(auth => {
          if (!auth) {
            window.location.reload();
          } else {
            const parsedToken = keycloak.idTokenParsed;
            if (parsedToken) {
              const userMail = parsedToken.email;
              this.workspaceName = this.useEphemeralStorage
                ? undefined
                : 'ws-' + this.appId + '-' + this.appDefinition + '-' + userMail;
              this.token = keycloak.idToken;
              this.email = userMail;
            }
          }
        })
        .catch(() => {
          console.error('Authentication Failed');
        });
    } else {
      // for ephemeral storage we generate new addresses every time, for fixed storages we simulate a stable user with a fixed address
      const userMail = (this.useEphemeralStorage ? uuidv4() : this.appId + '-' + this.appDefinition) + '@theia.cloud';
      this.workspaceName = this.useEphemeralStorage
        ? undefined
        : 'ws-' + this.appId + '-' + this.appDefinition + '-' + userMail;
      this.email = userMail;
    }
  }
});
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
  margin-top: 60px;
}
</style>
