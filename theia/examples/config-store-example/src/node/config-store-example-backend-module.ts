import { BackendApplicationContribution } from '@theia/core/lib/node';
import { ContainerModule } from '@theia/core/shared/inversify';

import { ConfigStoreBackendUser } from './config-store-backend-user';

export default new ContainerModule(bind => {
  bind(BackendApplicationContribution).to(ConfigStoreBackendUser).inSingletonScope();
});
