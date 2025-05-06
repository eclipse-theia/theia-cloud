import express, { Router } from 'express';
import * as vscode from 'vscode';

import { getFromEnv, THEIACLOUD_MONITOR_ENABLE_ACTIVITY_TRACKER, THEIACLOUD_MONITOR_PORT } from './env-variables';
import { ActivityTrackerModule } from './modules/activity-tracker-module';
import { MessagingModule } from './modules/messaging-module';
import { MonitorModule } from './monitor-module';

/**
 * Entry point of the extension.
 * Is triggered, when the extension is loaded.
 * Reads out the hostName and hostPort from env and starts the server.
 * If no values are defined it will fallback to `localhost:8081`
 */
export function activate(context: vscode.ExtensionContext): void {
  const hostPort = Number(getFromEnv(THEIACLOUD_MONITOR_PORT) ?? 8081);
  console.debug(`hostPort ${hostPort}`);
  startServer(hostPort);
}

/**
 * Starts the REST service and registers the endpoints of all enabled TrackerModules
 */
export function startServer(port: number): void {
  const app = express();
  const modules = getEnabledModules();
  const monitorRouter = Router();
  for (const module of modules) {
    module.registerEndpoints(monitorRouter);
  }
  app.use('/monitor', monitorRouter);
  const server = app.listen(port, function () {
    const address = server.address();
    if (address) {
      if (typeof address === 'string') {
        console.debug(`http://${address}`);
      } else {
        const host = address.address;
        const addressPort = address.port;
        console.debug(`http://${host}:${addressPort}`);
      }
    } else {
      console.warn('Server not started');
    }
  });
}

/**
 * Utility method to check which TrackerModules are enabled
 * @returns the list of enabled TrackerModules
 */
export function getEnabledModules(): MonitorModule[] {
  const modules: MonitorModule[] = [new MessagingModule()];
  if (getFromEnv(THEIACLOUD_MONITOR_ENABLE_ACTIVITY_TRACKER) === 'true') {
    console.debug('ActivityTrackerModule enabled');
    modules.push(new ActivityTrackerModule());
  } else {
    console.debug(`ActivityTrackerModule disabled ${getFromEnv(THEIACLOUD_MONITOR_ENABLE_ACTIVITY_TRACKER)}`);
  }
  return modules;
}

export function deactivate(): void {}
