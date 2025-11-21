import { injectable } from '@theia/core/shared/inversify';

import { MONITOR_ENABLE_ACTIVITY_TRACKER } from '../common/env-variables';
import { MonitorBackendModule } from '../common/monitor-backend-module';
import { TheiaCloudBackendMonitorService } from '../common/monitor-protocol';
import { ActivityTrackerBackendModuleImpl } from './modules/activity-tracker-backend-module-impl';
import { MessagingBackendModuleImpl } from './modules/messaging-backend-module-impl';

@injectable()
export class DefaultTheiaCloudBackendMonitorService implements TheiaCloudBackendMonitorService {
  /**
   * Returns true if ran in theia cloud context
   */
  isRunningOnTheiaCloud(): boolean {
    const serviceAuthToken = this.getServiceAuthToken();
    const serviceUrl = process.env.THEIACLOUD_SERVICE_URL;
    const sessionName = process.env.THEIACLOUD_SESSION_NAME;
    return serviceAuthToken && serviceUrl && sessionName ? true : false;
  }

  /**
   * Get the service auth token with fallback to deprecated app id environment variable.
   */
  private getServiceAuthToken(): string | undefined {
    const serviceAuthToken = process.env.THEIACLOUD_SERVICE_AUTH_TOKEN;
    if (serviceAuthToken) {
      return serviceAuthToken;
    }
    
    const appId = process.env.THEIACLOUD_APP_ID;
    if (appId) {
      console.warn('Using deprecated environment variable \'THEIACLOUD_APP_ID\'. ' +
                   'Please migrate to \'THEIACLOUD_SERVICE_AUTH_TOKEN\' in your configuration.');
      return appId;
    }
    
    return undefined;
  }

  async getEnabledBackendModules(): Promise<MonitorBackendModule[]> {
    const modules: MonitorBackendModule[] = [new MessagingBackendModuleImpl()];
    if (process.env[MONITOR_ENABLE_ACTIVITY_TRACKER] === 'true') {
      modules.push(new ActivityTrackerBackendModuleImpl());
    }
    return modules;
  }
}
