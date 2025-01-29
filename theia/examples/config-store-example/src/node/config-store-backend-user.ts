import { ConfigStoreClient } from '@eclipse-theiacloud/config-store';
import { ILogger } from '@theia/core';
import { BackendApplicationContribution } from '@theia/core/lib/node';
import { inject, injectable } from '@theia/core/shared/inversify';

@injectable()
export class ConfigStoreBackendUser implements BackendApplicationContribution {
  @inject(ConfigStoreClient)
  protected readonly configStoreClient: ConfigStoreClient;

  @inject(ILogger)
  protected readonly logger: ILogger;

  onStart(): void {
    this.logger.info('[ConfigStoreBackendUser] Started with client:', this.configStoreClient);
    this.configStoreClient.onDidChangeConfig(changeEvent => {
      this.logger.info('[ConfigStoreBackendUserConfig] store changed:', changeEvent);
      // Here you can react to the change event, i.e. use injected credentials to access a service
    });
  }
}
