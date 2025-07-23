import { Emitter, Event } from '@theia/core/lib/common/event';
import { injectable } from '@theia/core/shared/inversify';

import { ConfigChangeEvent, ConfigStoreClient } from '../common/config-store-protocol';

@injectable()
export class ConfigStoreBackendClient implements ConfigStoreClient {
  private onDidChangeConfigEmitter = new Emitter<ConfigChangeEvent>();
  get onDidChangeConfig(): Event<ConfigChangeEvent> {
    return this.onDidChangeConfigEmitter.event;
  }

  notifyConfigChange(event: ConfigChangeEvent): void {
    this.onDidChangeConfigEmitter.fire(event);
  }
}
