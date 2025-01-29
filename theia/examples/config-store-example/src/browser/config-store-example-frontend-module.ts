import { FrontendApplicationContribution } from '@theia/core/lib/browser';
import { CommandContribution, MenuContribution } from '@theia/core/lib/common';
import { ContainerModule } from '@theia/core/shared/inversify';

import {
  ConfigStoreExampleCommandContribution,
  ConfigStoreExampleMenuContribution
} from './config-store-example-contribution';
import { ConfigStoreFrontendUser } from './config-store-frontend-user';

export default new ContainerModule(bind => {
  bind(CommandContribution).to(ConfigStoreExampleCommandContribution).inSingletonScope();
  bind(MenuContribution).to(ConfigStoreExampleMenuContribution).inSingletonScope();
  bind(FrontendApplicationContribution).to(ConfigStoreFrontendUser).inSingletonScope();
});
