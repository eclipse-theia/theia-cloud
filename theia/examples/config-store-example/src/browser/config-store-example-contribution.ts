import { ConfigStoreServer } from '@eclipse-theiacloud/config-store';
import { CommonMenus } from '@theia/core/lib/browser';
import {
  Command,
  CommandContribution,
  CommandRegistry,
  MenuContribution,
  MenuModelRegistry,
  MessageService,
  QuickInputService
} from '@theia/core/lib/common';
import { inject, injectable } from '@theia/core/shared/inversify';

export const GetConfigValueCommand: Command = {
  id: 'ConfigStoreExample.getConfigValue',
  label: 'Get Config Value'
};

export const SetConfigValueCommand: Command = {
  id: 'ConfigStoreExample.setConfigValue',
  label: 'Set Config Value'
};

export const UnsetConfigValueCommand: Command = {
  id: 'ConfigStoreExample.unsetConfigValue',
  label: 'Unset Config Value'
};

@injectable()
export class ConfigStoreExampleCommandContribution implements CommandContribution {
  @inject(MessageService)
  protected readonly messageService: MessageService;

  @inject(ConfigStoreServer)
  protected readonly configStoreServer: ConfigStoreServer;

  @inject(QuickInputService)
  protected readonly quickInputService: QuickInputService;

  registerCommands(registry: CommandRegistry): void {
    registry.registerCommand(GetConfigValueCommand, {
      execute: async () => {
        const key = await this.quickInputService.input({
          prompt: 'Enter the key to get the value'
        });
        if (key) {
          const value = await this.configStoreServer.getValue(key);
          this.messageService.info(`Value for key "${key}": ${value}`);
        }
      }
    });

    registry.registerCommand(SetConfigValueCommand, {
      execute: async () => {
        const key = await this.quickInputService.input({
          prompt: 'Enter the key to set the value'
        });
        if (key) {
          const value = await this.quickInputService.input({
            prompt: `Enter the value for key "${key}"`
          });
          if (value !== undefined) {
            await this.configStoreServer.setValue(key, value);
            this.messageService.info(`Set value for key "${key}" to "${value}"`);
          }
        }
      }
    });

    registry.registerCommand(UnsetConfigValueCommand, {
      execute: async () => {
        const key = await this.quickInputService.input({
          prompt: 'Enter the key to unset the value'
        });
        if (key) {
          await this.configStoreServer.setValue(key, undefined);
          this.messageService.info(`Unset value for key "${key}"`);
        }
      }
    });
  }
}

@injectable()
export class ConfigStoreExampleMenuContribution implements MenuContribution {
  registerMenus(menus: MenuModelRegistry): void {
    menus.registerMenuAction(CommonMenus.EDIT_FIND, {
      commandId: GetConfigValueCommand.id,
      label: GetConfigValueCommand.label
    });
    menus.registerMenuAction(CommonMenus.EDIT_FIND, {
      commandId: SetConfigValueCommand.id,
      label: SetConfigValueCommand.label
    });
    menus.registerMenuAction(CommonMenus.EDIT_FIND, {
      commandId: UnsetConfigValueCommand.id,
      label: UnsetConfigValueCommand.label
    });
  }
}
