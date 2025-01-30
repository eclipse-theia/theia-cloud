import { ConfigStoreClient } from '@eclipse-theiacloud/config-store';
import { ILogger } from '@theia/core';
import { FrontendApplicationContribution } from '@theia/core/lib/browser';
import { inject, injectable } from '@theia/core/shared/inversify';

@injectable()
export class ConfigStoreFrontendUser implements FrontendApplicationContribution {
  @inject(ConfigStoreClient)
  protected readonly configStoreClient: ConfigStoreClient;

  @inject(ILogger)
  protected readonly logger: ILogger;

  onStart(): void {
    this.logger.info('[ConfigStoreFrontendUser] Started with client:', this.configStoreClient);
    this.configStoreClient.onDidChangeConfig(changeEvent => {
      this.logger.info('[ConfigStoreFrontendUser] Config store changed:', changeEvent);
    });
  }
}
