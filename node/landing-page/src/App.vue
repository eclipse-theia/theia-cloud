<template>
  <img alt="Theia logo" src="./assets/logo.png" />
  <WorkspaceLancher
    :workspaceServiceUrl="workspaceServiceUrl"
    :workspaceTemplate="workspaceTemplate"
    :email="email"
    :appId="appId"
  />
</template>

<script lang="ts">
import { defineComponent } from "vue";
import WorkspaceLancher from "./components/WorkspaceLancher.vue";
import Keycloak, { KeycloakConfig } from "keycloak-js";
import { v4 as uuidv4 } from "uuid";

interface AppData {
  email: string | undefined;
}

export default defineComponent({
  name: "App",
  props: {
    appId: String,
    useKeycloak: Boolean,
    keycloakAuthUrl: String,
    keycloakRealm: String,
    keycloakClientId: String,
    workspaceServiceUrl: String,
    workspaceTemplate: String,
  },
  components: {
    WorkspaceLancher,
  },
  data() {
    return {
      email: undefined,
    } as AppData;
  },
  created() {
    if (this.useKeycloak) {
      const keycloakConfig: KeycloakConfig = {
        url: this.keycloakAuthUrl,
        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
        realm: this.keycloakRealm!,
        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
        clientId: this.keycloakClientId!,
      };

      const keycloak = Keycloak(keycloakConfig);

      keycloak
        .init({
          onLoad: "login-required",
          redirectUri: window.location.href,
          checkLoginIframe: false,
        })
        .then((auth) => {
          if (!auth) {
            window.location.reload();
          } else {
            const parsedToken = keycloak.idTokenParsed;
            if (parsedToken) {
              this.email = parsedToken.email;
            }
          }
        })
        .catch(() => {
          console.error("Authentication Failed");
        });
    } else {
      this.email = uuidv4() + "@theia.cloud";
    }
  },
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
