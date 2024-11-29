import { ConnectionHandler, RpcConnectionHandler } from '@theia/core';
import { BackendApplicationContribution } from '@theia/core/lib/node/backend-application';
import { ContainerModule } from '@theia/core/shared/inversify';

import { ConfigStoreClient, ConfigStoreServer, configStoreServicePath } from '../common/config-store-protocol';
import { ConfigStoreBackendClient } from './config-store-client';
import { ConfigStoreServerImpl } from './config-store-service';

export default new ContainerModule(bind => {
  bind(ConfigStoreClient).to(ConfigStoreBackendClient).inSingletonScope();
  bind(ConfigStoreServerImpl).toSelf().inSingletonScope();
  bind(BackendApplicationContribution).toService(ConfigStoreServerImpl);

  // Bind ConfigStoreServer dynamically to add the backend client to the service.
  bind(ConfigStoreServer)
    .toDynamicValue(ctx => {
      const service = ctx.container.get(ConfigStoreServerImpl);
      const client = ctx.container.get<ConfigStoreClient>(ConfigStoreBackendClient);
      service.addClient(client);
      return service;
    })
    .inSingletonScope();

  bind(ConnectionHandler)
    .toDynamicValue(
      ctx =>
        new RpcConnectionHandler<ConfigStoreClient>(configStoreServicePath, client => {
          const service = ctx.container.get<ConfigStoreServer>(ConfigStoreServer);
          service.addClient(client);
          client.onDidCloseConnection(() => {
            service.removeClient(client);
          });
          return service;
        })
    )
    .inSingletonScope();
});
