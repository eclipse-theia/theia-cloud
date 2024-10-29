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
    const appId = process.env.THEIACLOUD_APP_ID;
    const serviceUrl = process.env.THEIACLOUD_SERVICE_URL;
    const sessionName = process.env.THEIACLOUD_SESSION_NAME;
    return appId && serviceUrl && sessionName ? true : false;
  }

  async getEnabledBackendModules(): Promise<MonitorBackendModule[]> {
    const modules: MonitorBackendModule[] = [new MessagingBackendModuleImpl()];
    if (process.env[MONITOR_ENABLE_ACTIVITY_TRACKER] === 'true') {
      modules.push(new ActivityTrackerBackendModuleImpl());
    }
    return modules;
  }
}
