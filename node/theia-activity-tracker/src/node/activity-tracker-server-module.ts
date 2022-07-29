import { ConnectionHandler, JsonRpcConnectionHandler } from '@theia/core';
import { ContainerModule } from '@theia/core/shared/inversify';

import { ACTIVITY_TRACKER_SERVICE_PATH, TheiaCloudActivityTrackerService } from '../common/activity-tracker-protocol';
import { DefaultTheiaCloudActivityTrackerService } from './activity-tracker-service';

export default new ContainerModule(bind => {
  bind(TheiaCloudActivityTrackerService).to(DefaultTheiaCloudActivityTrackerService).inSingletonScope();

  bind(ConnectionHandler)
    .toDynamicValue(context => new JsonRpcConnectionHandler(ACTIVITY_TRACKER_SERVICE_PATH, () => context.container.get(TheiaCloudActivityTrackerService)))
    .inSingletonScope();
});
