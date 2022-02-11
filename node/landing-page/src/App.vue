<template>
  <img alt="Theia logo" src="./assets/logo.png" />
  <WorkspaceLancher
    :workspaceServiceUrl="workspaceServiceUrl"
    :workspaceTemplate="workspaceTemplate"
    :email="email"
  />
</template>

<script lang="ts">
import { defineComponent } from "vue";
import WorkspaceLancher from "./components/WorkspaceLancher.vue";
import Keycloak, { KeycloakConfig } from "keycloak-js";

export default defineComponent({
  name: "App",
  props: {
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
    };
  },
  created() {
    console.log(this.keycloakAuthUrl);
    console.log(this.keycloakRealm);
    console.log(this.keycloakClientId);
    console.log(this.workspaceServiceUrl);
    console.log(this.workspaceTemplate);

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
