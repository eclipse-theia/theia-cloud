import { JsonRpcServer } from '@theia/core';

import { MonitorBackendModule } from '../monitor-backend-module';

export const ActivityTrackerBackendModule = Symbol('ActivityTrackerBackendModule');
export interface ActivityTrackerBackendModule
  extends JsonRpcServer<ActivityTrackerFrontendModule>,
    MonitorBackendModule {
  disconnectClient(client: ActivityTrackerFrontendModule): void;
  reportActivity(reason?: string): void;
}

export const ActivityTrackerFrontendModule = Symbol('ActivityTrackerFrontendModule');
export interface ActivityTrackerFrontendModule {
  displayPopup(): void;
}
