import { MessageService } from '@theia/core';
import { FrontendApplicationContribution } from '@theia/core/lib/browser';
import { inject, injectable } from '@theia/core/shared/inversify';
import { MessageType, ModalNotification } from '@theia/plugin-ext/lib/main/browser/dialogs/modal-notification';

import { MessageLevel, MessagingBackendModule, MessagingFrontendModule } from '../../common/modules/messaging-module';
import { TheiaCloudBackendMonitorService } from '../../common/monitor-protocol';

@injectable()
export class MessagingFrontendModuleImpl implements MessagingFrontendModule {
  constructor(@inject(MessageService) private readonly messageService: MessageService) {}

  displayMessage(level: MessageLevel, message: string, detail: string, fullscreen: boolean): void {
    switch (level) {
      case MessageLevel.WARN:
        fullscreen ? this.displayModalDialog(MessageType.Warning, message, detail) : this.messageService.warn(message);
        break;
      case MessageLevel.ERROR:
        fullscreen ? this.displayModalDialog(MessageType.Error, message, detail) : this.messageService.error(message);
        break;
      default:
        fullscreen ? this.displayModalDialog(MessageType.Info, message, detail) : this.messageService.warn(message);
        break;
    }
  }

  displayModalDialog(type: MessageType, message: string, detail: string): void {
    const modalNotification = new ModalNotification();
    modalNotification.showDialog(type, message, { modal: true, detail }, []);
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
