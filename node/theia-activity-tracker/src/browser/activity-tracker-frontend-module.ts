import { FrontendApplicationContribution, WebSocketConnectionProvider } from '@theia/core/lib/browser';
import { ContainerModule } from '@theia/core/shared/inversify';

import { ACTIVITY_TRACKER_SERVICE_PATH, TheiaCloudActivityTrackerService } from '../common/activity-tracker-protocol';
import { TheiaCloudActivityTrackerFrontendContribution } from './activity-tracker-contribution';

export default new ContainerModule(bind => {
  bind(TheiaCloudActivityTrackerService)
    .toDynamicValue(context => context.container.get(WebSocketConnectionProvider).createProxy<TheiaCloudActivityTrackerService>(ACTIVITY_TRACKER_SERVICE_PATH))
    .inSingletonScope();

  bind(FrontendApplicationContribution).to(TheiaCloudActivityTrackerFrontendContribution);
});
