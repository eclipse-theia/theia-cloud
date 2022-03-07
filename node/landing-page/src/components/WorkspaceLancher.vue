<template>
  <div>Please wait while we get your workspace ready.</div>
  <div class="loader"></div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import axios from "axios";

export default defineComponent({
  name: "WorkspaceLancher",
  props: {
    workspaceServiceUrl: String,
    workspaceTemplate: String,
    email: String,
    appId: String,
  },
  created() {
    if (this.email) {
      this.startWorkspace();
    }
  },
  watch: {
    email() {
      if (this.email) {
        this.startWorkspace();
      }
    },
  },
  methods: {
    startWorkspace() {
      console.log("Calling to " + (this.workspaceServiceUrl + "/workspaces"));
      axios
        .post(this.workspaceServiceUrl + "/workspaces", {
          template: this.workspaceTemplate,
          user: this.email,
          appId: this.appId,
        })
        .then((response) => {
          if (response.data.success) {
            location.replace("https://" + response.data.url);
          } else {
            console.error(response.data.error);
          }
        });
    },
  },
});
</script>

<style scoped>
.loader {
  border: 16px solid #f3f3f3; /* Light grey */
  border-top: 16px solid #0882ff; /* Blue */
  border-radius: 50%;
  width: 120px;
  height: 120px;
  animation: spin 2s linear infinite;
  margin-top: 25px;
  margin-left: auto;
  margin-right: auto;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style>
