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
  // add your contribution bindings here
  bind(CommandContribution).to(ConfigStoreExampleCommandContribution);
  bind(MenuContribution).to(ConfigStoreExampleMenuContribution);
});
