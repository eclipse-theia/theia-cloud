import { ConnectionHandler, RpcConnectionHandler } from '@theia/core';
import { BackendApplicationContribution } from '@theia/core/lib/node/backend-application';
import { ContainerModule } from '@theia/core/shared/inversify';

import { ConfigStoreClient, ConfigStoreService, configStoreServicePath } from '../common/config-store-protocol';
import { ConfigStoreBackendClient } from './config-store-client';
import { ConfigStoreServiceImpl } from './config-store-service';

export default new ContainerModule(bind => {
  bind(ConfigStoreClient).to(ConfigStoreBackendClient).inSingletonScope();
  bind(ConfigStoreServiceImpl).toSelf().inSingletonScope();
  bind(BackendApplicationContribution).toService(ConfigStoreServiceImpl);

  // Bind ConfigStoreService dynamically to add the backend client to the service.
  bind(ConfigStoreService)
    .toDynamicValue(ctx => {
      const service = ctx.container.get(ConfigStoreServiceImpl);
      const client = ctx.container.get<ConfigStoreClient>(ConfigStoreBackendClient);
      service.addClient(client);
      return service;
    })
    .inSingletonScope();

  bind(ConnectionHandler)
    .toDynamicValue(
      ctx =>
        new RpcConnectionHandler<ConfigStoreClient>(configStoreServicePath, client => {
          const service = ctx.container.get<ConfigStoreService>(ConfigStoreService);
          service.addClient(client);
          client.onDidCloseConnection(() => {
            service.removeClient(client);
          });
          return service;
        })
    )
    .inSingletonScope();
});
