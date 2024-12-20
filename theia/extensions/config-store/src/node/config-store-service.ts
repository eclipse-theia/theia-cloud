import { ILogger } from '@theia/core';
import { BackendApplicationContribution } from '@theia/core/lib/node/backend-application';
import { inject, injectable } from '@theia/core/shared/inversify';
import { Application, Router } from 'express';

import { ConfigStoreClient, ConfigStoreServer } from '../common/config-store-protocol';

const configStoreRestPath = '/theia-cloud/config-store';

@injectable()
export class ConfigStoreServerImpl implements ConfigStoreServer, BackendApplicationContribution {
  protected clients: ConfigStoreClient[] = [];

  @inject(ILogger)
  protected readonly logger: ILogger;

  async configure(app: Application): Promise<void> {
    const router = Router();

    // TODO configure endpoints
    app.use(configStoreRestPath, router);
  }

  addClient(client: ConfigStoreClient): void {
    this.logger.debug('Adding client');
    this.clients.push(client);
  }
  removeClient(client: ConfigStoreClient): boolean {
    this.logger.debug('Removing client');
    const index = this.clients.indexOf(client);
    if (index !== -1) {
      this.clients.splice(index, 1);
      return true;
    }
    return false;
  }

  getValue(key: string): Promise<string | undefined> {
    throw new Error('Method not implemented.');
  }
  sayHelloTo(name: string): Promise<string> {
    return new Promise<string>(resolve => resolve('Hello ' + name));
  }
}
