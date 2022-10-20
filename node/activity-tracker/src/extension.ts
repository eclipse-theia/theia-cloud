import * as vscode from 'vscode';
import * as express from 'express';
import { ActivityTrackerModule } from './modules/activity-tracker-module';
import { TrackerModule } from './tracker-module';
import { TheiaCloudEnv, getFromEnv } from './env-variables';

/**
 * Entry point of the extension.
 * Is triggered, when the extension is loaded.
 * Reads out the hostName and hostPort from env and starts the server.
 * If no values are defined it will fallback to `localhost:8081`
 */
export function activate(context: vscode.ExtensionContext) {
	const hostName = getFromEnv(TheiaCloudEnv.ACTIVITY_SERVICE_HOST) ?? 'localhost';
	const hostPort = Number(getFromEnv(TheiaCloudEnv.ACTIVITY_SERVICE_PORT) ?? 8081);

	startServer(hostName, hostPort);
}

/**
 * Starts the REST service and registers the endpoints of all enabled TrackerModules's
 */
export function startServer(address: string, port: number): void {
	const app = express();
	const modules = getEnabledModules();

	if(modules.length === 0) {
		console.log('Did not start tracker REST service as no modules were specified');
	} else {
		for (const module of modules) {
			module.registerEndpoints(app);
		}
		const server = app.listen(port, address, function() {
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
}

/**
 * Utility method to check which TrackerModules's are enabled
 * @returns the list of enabled TrackerModule's
 */
export function getEnabledModules(): TrackerModule[] {
	const modules: TrackerModule[] = [];
	if (getFromEnv(TheiaCloudEnv.ACTIVITY_SERVICE_ENABLE_TRACKER) === '1') {
		modules.push(new ActivityTrackerModule());
	}
	return modules;
}

export function deactivate() {}
