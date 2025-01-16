import { JsonRpcServer } from '@theia/core';

import { MonitorBackendModule } from '../monitor-backend-module';

export const MessagingBackendModule = Symbol('MessagingBackendModule');
export interface MessagingBackendModule extends JsonRpcServer<MessagingFrontendModule>, MonitorBackendModule {
  disconnectClient(client: MessagingFrontendModule): void;
}

export const MessagingFrontendModule = Symbol('MessagingFrontendModule');
export interface MessagingFrontendModule {
  displayMessage(level: MessageLevel, message: string, detail: string, fullscreen: boolean): void;
}

// eslint-disable-next-line no-shadow
export enum MessageLevel {
  WARN = 'warn',
  ERROR = 'error',
  INFO = 'info'
}
