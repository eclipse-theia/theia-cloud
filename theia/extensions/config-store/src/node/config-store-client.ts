import { injectable } from '@theia/core/shared/inversify';

import { ConfigStoreClient } from '../common/config-store-protocol';

@injectable()
export class ConfigStoreBackendClient implements ConfigStoreClient {
  getName(): Promise<string> {
    throw new Error('Method not implemented.');
  }
}
