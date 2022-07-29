import { FrontendApplicationContribution } from '@theia/core/lib/browser';
import { inject, injectable } from '@theia/core/shared/inversify';

import { TheiaCloudActivityTrackerService } from '../common/activity-tracker-protocol';

@injectable()
export class TheiaCloudActivityTrackerFrontendContribution implements FrontendApplicationContribution {
  private static REQUEST_PERIOD_MS = 1 * 60 * 1000; // 1 minute

  private isAnyActivity: boolean;
  private isTimerRunning: boolean;

  constructor(@inject(TheiaCloudActivityTrackerService) protected activityService: TheiaCloudActivityTrackerService) {
    this.isAnyActivity = false;
    this.isTimerRunning = false;
  }

  async initialize(): Promise<void> {
    const onTheiaCloud = await this.activityService.isRunningOnTheiaCloud();
    if (!onTheiaCloud) {
      return;
    }

    window.addEventListener('keydown', () => this.onAnyActivity());
    window.addEventListener('mousedown', () => this.onAnyActivity());
    window.addEventListener('mousemove', () => this.onAnyActivity());

    // needed if user reopens browser tab
    this.sendRequestAndSetTimer();
  }

  protected onAnyActivity(): void {
    this.isAnyActivity = true;

    if (!this.isTimerRunning) {
      this.sendRequestAndSetTimer();
    }
  }

  protected sendRequestAndSetTimer(): void {
    this.activityService.reportActivity();
    this.isAnyActivity = false;

    setTimeout(() => this.checkActivityTimerCallback(), TheiaCloudActivityTrackerFrontendContribution.REQUEST_PERIOD_MS);
    this.isTimerRunning = true;
  }

  protected checkActivityTimerCallback(): void {
    this.isTimerRunning = false;
    if (this.isAnyActivity) {
      this.sendRequestAndSetTimer();
    }
  }
}
