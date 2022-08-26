/* tslint:disable */
/**
 * Request to ping the availability of the service.
 * @export
 * @interface PingRequest
 */
export interface PingRequest {
    /**
     * The App Id of this Theia.cloud instance. Request without a matching Id will be denied.
     * @type {string}
     * @memberof PingRequest
     */
    appId: string;
}

/**
 * Check if a given object implements the PingRequest interface.
 */
export function instanceOfPingRequest(value: object): boolean {
  let isInstance = true;
  isInstance = isInstance && 'appId' in value;

  return isInstance;
}

export function PingRequestFromJSON(json: any): PingRequest {
  return PingRequestFromJSONTyped(json, false);
}

export function PingRequestFromJSONTyped(json: any, ignoreDiscriminator: boolean): PingRequest {
  if ((json === undefined) || (json === null)) {
    return json;
  }
  return {

    'appId': json['appId']
  };
}

export function PingRequestToJSON(value?: PingRequest | null): any {
  if (value === undefined) {
    return undefined;
  }
  if (value === null) {
    return null;
  }
  return {

    'appId': value.appId
  };
}

