import { MonitorBackendModule } from './monitor-backend-module';

export const MONITOR_BACKEND_SERVICE_PATH = '/services/theiacloud-monitor';
export const MESSAGING_SERVICE_PATH = '/services/theiacloud-monitor/messaging';
export const ACTIVITY_TRACKER_SERVICE_PATH = '/services/theiacloud-monitor/activity-tracker';

export const COMMAND_ACTIVITY_REPORT_TITLE = 'theia.cloud.monitor.activity.report';

export const TheiaCloudBackendMonitorService = Symbol('TheiaCloudBackendMonitorService');

export interface TheiaCloudBackendMonitorService {
  /**
   * Returns true if we are running in the Theia.cloud environment.
   */
  isRunningOnTheiaCloud(): boolean;

  getEnabledBackendModules(): Promise<MonitorBackendModule[]>;
}
