/********************************************************************************
 * Copyright (C) 2022 EclipseSource, Lockular, Ericsson, STMicroelectronics and
 * others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License v. 2.0 are satisfied: GNU General Public License, version 2
 * with the GNU Classpath Exception which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 ********************************************************************************/

import { createApp } from "vue";
import App from "./App.vue";
import { TheiaCloudConfig } from "./iconfig";
declare global {
  interface Window {
    theiaCloudConfig: TheiaCloudConfig;
  }
}

function theiaCloudConfig(): TheiaCloudConfig {
  if (typeof window.theiaCloudConfig === "object") {
    return window.theiaCloudConfig || {};
  }
  return {
    appId: "",
    appName: "",
    useKeycloak: false,
    serviceUrl: "",
    appDefinition: "",
    useEphemeralStorage: true
  };
}

const config: TheiaCloudConfig = Object.freeze({
  ...theiaCloudConfig(),
});

createApp(App, {
  appId: config.appId,
  appName: config.appName,
  useKeycloak: config.useKeycloak,
  keycloakAuthUrl: config.keycloakAuthUrl,
  keycloakRealm: config.keycloakRealm,
  keycloakClientId: config.keycloakClientId,
  serviceUrl: config.serviceUrl,
  appDefinition: config.appDefinition,
  useEphemeralStorage: config.useEphemeralStorage
}).mount("#app");
