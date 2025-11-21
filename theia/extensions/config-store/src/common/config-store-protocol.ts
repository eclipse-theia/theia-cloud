import { Event } from '@theia/core/lib/common/event';

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
   */
  getValue(key: string): Promise<string | undefined>;

  /**
   * Sets the value for the given key or unsets it by passing `undefined`.
   * @param key
   * @param value
   */
  setValue(key: string, value: string | undefined): Promise<void>;

  /**
   * Returns the full entry set of the config store.
   */
  getEntries(): Promise<ConfigVariable[]>;
}

/**
 * A config variable consisting of a key and a value.
 */
export interface ConfigVariable {
  key: string;
  value: string;
}

export type ConfigChangeEvent = ConfigValueAddedEvent | ConfigValueRemovedEvent | ConfigValueModifiedEvent;

export interface ConfigValueAddedEvent {
  kind: 'valueAdded';
  key: string;
  newValue: string;
}

export interface ConfigValueRemovedEvent {
  kind: 'valueRemoved';
  key: string;
  oldValue: string;
}

export interface ConfigValueModifiedEvent {
  kind: 'valueModified';
  key: string;
  oldValue: string;
  newValue: string;
}

export const ConfigStoreClient = Symbol('ConfigStoreClient');
export interface ConfigStoreClient {
  /** Subscribe to get notified of config changes. */
  get onDidChangeConfig(): Event<ConfigChangeEvent>;

  /** Called by the store to inform the client of a new change. */
  notifyConfigChange(event: ConfigChangeEvent): void;
}
