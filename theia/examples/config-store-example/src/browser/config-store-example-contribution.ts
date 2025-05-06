import { CommonMenus } from '@theia/core/lib/browser';
import {
  Command,
  CommandContribution,
  CommandRegistry,
  MenuContribution,
  MenuModelRegistry,
  MessageService
} from '@theia/core/lib/common';
import { inject, injectable } from '@theia/core/shared/inversify';

export const ConfigStoreExampleCommand: Command = {
  id: 'ConfigStoreExample.command',
  label: 'Say Hello'
};

@injectable()
export class ConfigStoreExampleCommandContribution implements CommandContribution {
  @inject(MessageService)
  protected readonly messageService!: MessageService;

  registerCommands(registry: CommandRegistry): void {
    registry.registerCommand(ConfigStoreExampleCommand, {
      execute: () => this.messageService.info('Hello World!')
    });
  }
}

@injectable()
export class ConfigStoreExampleMenuContribution implements MenuContribution {
  registerMenus(menus: MenuModelRegistry): void {
    menus.registerMenuAction(CommonMenus.EDIT_FIND, {
      commandId: ConfigStoreExampleCommand.id,
      label: ConfigStoreExampleCommand.label
    });
  }
}
