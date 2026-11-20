import { CommandRegistry } from '@theia/core';
import { FrontendApplicationContribution } from '@theia/core/lib/browser';
import { inject, injectable } from '@theia/core/shared/inversify';

import {
  ActivityTrackerBackendModule,
  ActivityTrackerFrontendModule
} from '../../common/modules/activity-tracker-module';
import { COMMAND_ACTIVITY_REPORT_TITLE, TheiaCloudBackendMonitorService } from '../../common/monitor-protocol';
import { MonitorDialog } from '../monitor-dialog';

@injectable()
export class ActivityTrackerFrontendModuleImpl implements ActivityTrackerFrontendModule {
  constructor(
    @inject(CommandRegistry)
    private readonly commandRegistry: CommandRegistry
  ) {}

  displayPopup(): void {
    new MonitorDialog(
      'warning',
      'Are you still here?',
      'This session has been inactive for a while and will be shut down soon.',
      'I am still here'
    )
      .open()
      .then(confirmed => {
        if (confirmed) {
          this.commandRegistry.executeCommand(COMMAND_ACTIVITY_REPORT_TITLE);
        }
      });
  }
}

@injectable()
export class ActivityTrackerFrontendContribution implements FrontendApplicationContribution {
  constructor(
    @inject(TheiaCloudBackendMonitorService)
    protected backendService: TheiaCloudBackendMonitorService,
    @inject(CommandRegistry)
    private readonly commandRegistry: CommandRegistry,
    @inject(ActivityTrackerBackendModule)
    private readonly activityTrackerBackendModule: ActivityTrackerBackendModule
  ) {}

  async initialize(): Promise<void> {
    const onTheiaCloud = await this.backendService.isRunningOnTheiaCloud();
    if (!onTheiaCloud) {
      return;
    }

    this.registerCommand();

    window.addEventListener('keydown', () => this.activityTrackerBackendModule.reportActivity('keydown'));
    window.addEventListener('mousedown', () => this.activityTrackerBackendModule.reportActivity('mousedown'));
    window.addEventListener('mousemove', () => this.activityTrackerBackendModule.reportActivity('mousemove'));

    this.activityTrackerBackendModule.reportActivity('init');
  }

  protected registerCommand(): void {
    this.commandRegistry.registerCommand(
      { id: COMMAND_ACTIVITY_REPORT_TITLE },
      {
        execute: reason => this.activityTrackerBackendModule.reportActivity(`${reason ?? 'activity'} (via command)`),
        isEnabled: () => true
      }
    );
  }
}
