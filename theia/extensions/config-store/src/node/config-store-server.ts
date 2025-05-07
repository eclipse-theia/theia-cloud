import { ILogger } from '@theia/core';
import { BackendApplicationContribution } from '@theia/core/lib/node/backend-application';
import { inject, injectable } from '@theia/core/shared/inversify';
import { Application, json, Router } from 'express';

import {
  ConfigChangeEvent,
  ConfigStoreClient,
  ConfigStoreServer,
  ConfigVariable
} from '../common/config-store-protocol';

const configStoreRestPath = '/theia-cloud/config-store';

@injectable()
export class ConfigStoreServerImpl implements ConfigStoreServer, BackendApplicationContribution {
  protected readonly entries: { [key: string]: string } = {};
  protected readonly clients: ConfigStoreClient[] = [];

  @inject(ILogger)
  protected readonly logger: ILogger;

  async configure(app: Application): Promise<void> {
    const configStoreRouter = Router();
    configStoreRouter.use(json());

    // TODO do we need to process receives keys and values for security reasons?
    // I.e. should we remove certain characters or character sequences? Or escape them?
    configStoreRouter.post('/', async (req, res) => {
      const { body } = req;
      if (
        typeof body === 'object' &&
        typeof body.key === 'string' &&
        // eslint-disable-next-line no-null/no-null
        (typeof body.value === 'string' || body.value === undefined || body.value === null)
      ) {
        const { key, value } = body;
        try {
          // eslint-disable-next-line no-null/no-null
          await this.setValue(key, value === null ? undefined : value);
          res.sendStatus(200);
        } catch (error) {
          this.logger.error(`Failed to set value for key: ${key}`, error);
          res.status(500).send(`An unexpected error occurred: ${error}`);
        }
      } else {
        res.status(400).send('Invalid request body');
      }
    });

    // Simple health check endpoint
    configStoreRouter.get('/', async (_req, res) => {
      res.sendStatus(204);
    });

    app.use(configStoreRestPath, configStoreRouter);
  }

  addClient(client: ConfigStoreClient): void {
    this.logger.debug('[ConfigStoreServerImpl] Adding client');
    this.clients.push(client);
  }
  removeClient(client: ConfigStoreClient): boolean {
    this.logger.debug('[ConfigStoreServerImpl] Removing client');
    const index = this.clients.indexOf(client);
    if (index !== -1) {
      this.clients.splice(index, 1);
      return true;
    }
    return false;
  }

  getValue(key: string): Promise<string | undefined> {
    const value = this.entries[key];
    return Promise.resolve(value);
  }

  private notifyClients(event: ConfigChangeEvent): void {
    for (const client of this.clients) {
      client.notifyConfigChange(event);
    }
  }

  async setValue(key: string, value: string | undefined): Promise<void> {
    const oldValue = this.entries[key];
    if (value === undefined) {
      if (oldValue !== undefined) {
        delete this.entries[key];
        this.logger.info(`[ConfigStoreServerImpl] Removed value for key: ${key}`);
        this.notifyClients({ kind: 'valueRemoved', key, oldValue });
      }
      return;
    }

    if (oldValue !== value) {
      this.entries[key] = value;
      if (oldValue === undefined) {
        this.logger.info(`[ConfigStoreServerImpl] Added value for key: ${key}`);
        this.notifyClients({ kind: 'valueAdded', key, newValue: value });
      } else {
        this.logger.info(`[ConfigStoreServerImpl] Modified value for key: ${key}`);
        this.notifyClients({ kind: 'valueModified', key, oldValue, newValue: value });
      }
    }
  }

  async getEntries(): Promise<ConfigVariable[]> {
    return Object.keys(this.entries).map(key => ({ key, value: this.entries[key] }));
  }
}
