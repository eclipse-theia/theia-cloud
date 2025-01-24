export const configStoreServicePath = '/services/theia-cloud/config-store';

export const ConfigStoreServer = Symbol('ConfigStoreServer');
export interface ConfigStoreServer {
  /** Add client to be notified about config store changes. */
  addClient(client: ConfigStoreClient): void;
  /**
   * Remove client from the list of clients to be notified about config store changes.
   * @returns `true` if the client was previously registered and now removed, `false` otherwise.
   */
  removeClient(client: ConfigStoreClient): boolean;
  /**
   * Returns the value for the given key or `undefined` if none is registered.
   * TODO: Add support for default values.
   * TODO: Use EnvVariable as return type? Similar to EnvVariablesServer.
   */
  getValue(key: string): Promise<string | undefined>;
}

export const ConfigStoreClient = Symbol('ConfigStoreClient');
export interface ConfigStoreClient {
  // TODO Remove example method. Add notification method(s) instead.
  getName(): Promise<string>;
}
