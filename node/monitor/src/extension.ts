import * as vscode from 'vscode';
import * as express from 'express';
import { ActivityTrackerModule } from './modules/activity-tracker-module';
import { MonitorModule } from './monitor-module';
import { MONITOR_ENABLE_ACTIVITY_TRACKER, MONITOR_PORT, getFromEnv } from './env-variables';
import { MessagingModule } from './modules/messaging-module';

/**
 * Entry point of the extension.
 * Is triggered, when the extension is loaded.
 * Reads out the hostName and hostPort from env and starts the server.
 * If no values are defined it will fallback to `localhost:8081`
 */
export function activate(context: vscode.ExtensionContext) {
	const hostPort = Number(getFromEnv(MONITOR_PORT) ?? 8081);

	startServer(hostPort);
}

/**
 * Starts the REST service and registers the endpoints of all enabled TrackerModules's
 */
 export function startServer(port: number): void {
	const app = express();
	const modules = getEnabledModules();

	for (const module of modules) {
		module.registerEndpoints(app);
	}
	const server = app.listen(port, function() {
		const address = server.address();
		if( address) {
			if(typeof address === 'string') {
				console.debug(`http://${address}`);
			} else {
				var host = address.address;
				var port = address.port;
				console.debug(`http://${host}:${port}`);
			}
		} else {
			console.debug(`Server not started`);
		}
	});
}

/**
 * Utility method to check which TrackerModules's are enabled
 * @returns the list of enabled TrackerModule's
 */
export function getEnabledModules(): MonitorModule[] {
	const modules: MonitorModule[] = [new MessagingModule()];
	if (getFromEnv(MONITOR_ENABLE_ACTIVITY_TRACKER) === '1') {
		modules.push(new ActivityTrackerModule());
	}
	return modules;
}

export function deactivate() {}
