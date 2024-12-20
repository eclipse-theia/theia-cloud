import { ConnectionHandler, JsonRpcConnectionHandler } from '@theia/core';
import { BackendApplicationContribution } from '@theia/core/lib/node/backend-application';
import { ContainerModule } from '@theia/core/shared/inversify';

import { ActivityTrackerBackendModule, ActivityTrackerFrontendModule } from '../common/modules/activity-tracker-module';
import { MessagingBackendModule, MessagingFrontendModule } from '../common/modules/messaging-module';
import {
  ACTIVITY_TRACKER_SERVICE_PATH,
  MESSAGING_SERVICE_PATH,
  MONITOR_BACKEND_SERVICE_PATH,
  TheiaCloudBackendMonitorService
} from '../common/monitor-protocol';
import { ActivityTrackerBackendModuleImpl } from './modules/activity-tracker-backend-module-impl';
import { MessagingBackendModuleImpl } from './modules/messaging-backend-module-impl';
import { TheiaCloudMonitorRESTServiceContribution } from './monitor-rest-service-contribution';
import { DefaultTheiaCloudBackendMonitorService } from './monitor-service';

export default new ContainerModule(bind => {
  // bind TheiaCloudBackendMonitorService
  bind(TheiaCloudBackendMonitorService).to(DefaultTheiaCloudBackendMonitorService).inSingletonScope();

  // handle default service
  bind(ConnectionHandler)
    .toDynamicValue(
      context =>
        new JsonRpcConnectionHandler(MONITOR_BACKEND_SERVICE_PATH, () =>
          context.container.get(TheiaCloudBackendMonitorService)
        )
    )
    .inSingletonScope();

  // handle messaging backend module
  bind(MessagingBackendModuleImpl).toSelf().inSingletonScope();
  bind(MessagingBackendModule).toService(MessagingBackendModuleImpl);
  bind(ConnectionHandler)
    .toDynamicValue(
      context =>
        new JsonRpcConnectionHandler<MessagingFrontendModule>(MESSAGING_SERVICE_PATH, client => {
          const server = context.container.get<MessagingBackendModule>(MessagingBackendModule);
          server.setClient(client);
          client.onDidCloseConnection(() => server.disconnectClient(client));
          return server;
        })
    )
    .inSingletonScope();

  // handle activity tracker backend module
  bind(ActivityTrackerBackendModuleImpl).toSelf().inSingletonScope();
  bind(ActivityTrackerBackendModule).toService(ActivityTrackerBackendModuleImpl);
  bind(ConnectionHandler)
    .toDynamicValue(
      context =>
        new JsonRpcConnectionHandler<ActivityTrackerFrontendModule>(ACTIVITY_TRACKER_SERVICE_PATH, client => {
          const server = context.container.get<ActivityTrackerBackendModule>(ActivityTrackerBackendModule);
          server.setClient(client);
          client.onDidCloseConnection(() => server.disconnectClient(client));
          return server;
        })
    )
    .inSingletonScope();

  // Bind contribution
  bind(TheiaCloudMonitorRESTServiceContribution).toSelf().inSingletonScope();
  bind(BackendApplicationContribution).toService(TheiaCloudMonitorRESTServiceContribution);
});
