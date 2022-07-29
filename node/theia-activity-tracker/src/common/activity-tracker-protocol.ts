export const ACTIVITY_TRACKER_SERVICE_PATH = '/services/theiacloud-activity-tracker';

export const TheiaCloudActivityTrackerService = Symbol('TheiaCloudActivityTrackerService');

export interface TheiaCloudActivityTrackerService {
  /**
   * Rests the current session inactivity timeout.
   */
  reportActivity(): void;

  /**
   * Returns true if we are running in the Theia.cloud environment.
   */
  isRunningOnTheiaCloud(): boolean;
}
