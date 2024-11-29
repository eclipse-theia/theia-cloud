import { ServiceConnectionProvider } from '@theia/core/lib/browser';
import { ContainerModule } from '@theia/core/shared/inversify';

import { ConfigStoreClient, ConfigStoreServer, configStoreServicePath } from '../common/config-store-protocol';
import { ConfigStoreFrontendClient } from './config-store-client';

export default new ContainerModule(bind => {
  bind(ConfigStoreClient).to(ConfigStoreFrontendClient).inSingletonScope();

  bind(ConfigStoreServer)
    .toDynamicValue(ctx => {
      const provider = ctx.container.get(ServiceConnectionProvider);
      const client = ctx.container.get<ConfigStoreClient>(ConfigStoreClient);
      return provider.createProxy<ConfigStoreServer>(configStoreServicePath, client);
    })
    .inSingletonScope();
});
