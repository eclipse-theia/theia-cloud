import { MessageService } from '@theia/core';
import { FrontendApplicationContribution } from '@theia/core/lib/browser';
import { inject, injectable } from '@theia/core/shared/inversify';

import { MessageLevel, MessagingBackendModule, MessagingFrontendModule } from '../../common/modules/messaging-module';
import { TheiaCloudBackendMonitorService } from '../../common/monitor-protocol';
import { MonitorDialog } from '../monitor-dialog';

@injectable()
export class MessagingFrontendModuleImpl implements MessagingFrontendModule {
  constructor(@inject(MessageService) private readonly messageService: MessageService) {}

  displayMessage(level: MessageLevel, message: string, detail: string, fullscreen: boolean): void {
    if (fullscreen) {
      const severity = level === MessageLevel.WARN ? 'warning' : level === MessageLevel.ERROR ? 'error' : 'info';
      new MonitorDialog(severity, message, detail).open();
    } else if (level === MessageLevel.ERROR) {
      this.messageService.error(message);
    } else {
      this.messageService.warn(message);
    }
  }
}

@injectable()
export class MessagingFrontendContribution implements FrontendApplicationContribution {
  constructor(
    @inject(TheiaCloudBackendMonitorService)
    protected backendService: TheiaCloudBackendMonitorService,
    @inject(MessagingBackendModule)
    private readonly messagingBackendModule: MessagingBackendModule
  ) {}

  async initialize(): Promise<void> {
    const onTheiaCloud = await this.backendService.isRunningOnTheiaCloud();
    if (!onTheiaCloud) {
      return;
    }

    // Workaround to make sure that the backend module is initialized
    // eslint-disable-next-line no-unused-expressions
    this.messagingBackendModule;
  }
}
