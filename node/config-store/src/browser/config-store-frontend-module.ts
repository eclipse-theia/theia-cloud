import { ServiceConnectionProvider } from '@theia/core/lib/browser';
import { ContainerModule } from '@theia/core/shared/inversify';

import { ConfigStoreClient, ConfigStoreService, configStoreServicePath } from '../common/config-store-protocol';
import { ConfigStoreFrontendClient } from './config-store-client';

export default new ContainerModule(bind => {
  bind(ConfigStoreClient).to(ConfigStoreFrontendClient).inSingletonScope();

  bind(ConfigStoreService)
    .toDynamicValue(ctx => {
      const provider = ctx.container.get(ServiceConnectionProvider);
      const client = ctx.container.get<ConfigStoreClient>(ConfigStoreClient);
      return provider.createProxy<ConfigStoreService>(configStoreServicePath, client);
    })
    .inSingletonScope();
});
