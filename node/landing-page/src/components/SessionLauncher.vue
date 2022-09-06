<template>
  <div>
    <div>{{ text }}</div>
    <div class="loader" v-if="showSpinner"></div>
  </div>
</template>

<script lang="ts">
import { LaunchRequest, TheiaCloud } from '@eclipse-theiacloud/common';
import { defineComponent } from 'vue';

export default defineComponent({
  name: 'SessionLauncher',
  props: {
    serviceUrl: String,
    appDefinition: String,
    email: String,
    appId: String,
    useEphemeralStorage: Boolean,
    workspaceName: String
  },
  created() {
    if (this.email) {
      this.startSession(10);
    }
  },
  data() {
    return {
      text: 'Please wait while we get your Theia session ready...',
      showSpinner: true
    };
  },
  watch: {
    email() {
      if (this.email) {
        this.startSession(10);
      }
    }
  },
  methods: {
    startSession(retries: number) {
      console.log('Calling to ' + this.serviceUrl);
      TheiaCloud.launch(this.useEphemeralStorage
          ? LaunchRequest.ephemeral(this.serviceUrl!, this.appId!, this.appDefinition!)
          : LaunchRequest.createWorkspace(this.serviceUrl!, this.appId!, this.appDefinition!)
      , retries, 300000).catch(error => {
            this.text = error;
            this.showSpinner = false;
            console.error(error);
          });
        }
    }
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
