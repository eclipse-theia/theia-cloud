/**
 * Generated using theia-extension-generator
 */
import { CommandContribution, MenuContribution } from '@theia/core/lib/common';
import { ContainerModule } from '@theia/core/shared/inversify';

import {
  ConfigStoreExampleCommandContribution,
  ConfigStoreExampleMenuContribution
} from './config-store-example-contribution';

export default new ContainerModule(bind => {
  bind(CommandContribution).to(ConfigStoreExampleCommandContribution).inSingletonScope();
  bind(MenuContribution).to(ConfigStoreExampleMenuContribution).inSingletonScope();
});
