import { FrontendApplicationContribution, WebSocketConnectionProvider } from '@theia/core/lib/browser';
import { ContainerModule } from '@theia/core/shared/inversify';

import { ActivityTrackerBackendModule, ActivityTrackerFrontendModule } from '../common/modules/activity-tracker-module';
import { MessagingBackendModule, MessagingFrontendModule } from '../common/modules/messaging-module';
import {
  ACTIVITY_TRACKER_SERVICE_PATH,
  MESSAGING_SERVICE_PATH,
  MONITOR_BACKEND_SERVICE_PATH,
  TheiaCloudBackendMonitorService
} from '../common/monitor-protocol';
import {
  ActivityTrackerFrontendContribution,
  ActivityTrackerFrontendModuleImpl
} from './modules/activity-tracker-frontend-module-impl';
import { MessagingFrontendContribution, MessagingFrontendModuleImpl } from './modules/messaging-frontend-module-impl';

export default new ContainerModule(bind => {
  // default backend service
  bind(TheiaCloudBackendMonitorService)
    .toDynamicValue(context =>
      context.container
        .get(WebSocketConnectionProvider)
        .createProxy<TheiaCloudBackendMonitorService>(MONITOR_BACKEND_SERVICE_PATH)
    )
    .inSingletonScope();

  // messaging service
  bind(MessagingFrontendModuleImpl).toSelf().inSingletonScope();
  bind(MessagingFrontendModule).toService(MessagingFrontendModuleImpl);
  bind(MessagingBackendModule)
    .toDynamicValue(context => {
      const client = context.container.get(MessagingFrontendModuleImpl);
      return WebSocketConnectionProvider.createProxy(context.container, MESSAGING_SERVICE_PATH, client);
    })
    .inSingletonScope();
  bind(FrontendApplicationContribution).to(MessagingFrontendContribution);

  // activity tracker service
  bind(ActivityTrackerFrontendModuleImpl).toSelf().inSingletonScope();
  bind(ActivityTrackerFrontendModule).toService(ActivityTrackerFrontendModuleImpl);
  bind(ActivityTrackerBackendModule)
    .toDynamicValue(context => {
      const client = context.container.get(ActivityTrackerFrontendModuleImpl);
      return WebSocketConnectionProvider.createProxy(context.container, ACTIVITY_TRACKER_SERVICE_PATH, client);
    })
    .inSingletonScope();
  bind(FrontendApplicationContribution).to(ActivityTrackerFrontendContribution);
});
