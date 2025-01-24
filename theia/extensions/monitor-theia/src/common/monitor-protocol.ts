import { MonitorBackendModule } from './monitor-backend-module';

export const MONITOR_BACKEND_SERVICE_PATH = '/services/theia-cloud-monitor';
export const MESSAGING_SERVICE_PATH = '/services/theia-cloud-monitor/messaging';
export const ACTIVITY_TRACKER_SERVICE_PATH = '/services/theia-cloud-monitor/activity-tracker';

export const COMMAND_ACTIVITY_REPORT_TITLE = 'theia.cloud.monitor.activity.report';

export const TheiaCloudBackendMonitorService = Symbol('TheiaCloudBackendMonitorService');

export interface TheiaCloudBackendMonitorService {
  /**
   * Returns true if we are running in the Theia Cloud environment.
   */
  isRunningOnTheiaCloud(): boolean;

  getEnabledBackendModules(): Promise<MonitorBackendModule[]>;
}
