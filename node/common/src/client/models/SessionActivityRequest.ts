/* tslint:disable */
/**
 * A request to report activity for a running session.
 * @export
 * @interface SessionActivityRequest
 */
export interface SessionActivityRequest {
    /**
     * The App Id of this Theia.cloud instance. Request without a matching Id will be denied.
     * @type {string}
     * @memberof SessionActivityRequest
     */
    appId: string;
    /**
     * The name of the session for which activity is reported.
     * @type {string}
     * @memberof SessionActivityRequest
     */
    sessionName: string;
}

/**
 * Check if a given object implements the SessionActivityRequest interface.
 */
export function instanceOfSessionActivityRequest(value: object): boolean {
  let isInstance = true;
  isInstance = isInstance && 'appId' in value;
  isInstance = isInstance && 'sessionName' in value;

  return isInstance;
}

export function SessionActivityRequestFromJSON(json: any): SessionActivityRequest {
  return SessionActivityRequestFromJSONTyped(json, false);
}

export function SessionActivityRequestFromJSONTyped(json: any, ignoreDiscriminator: boolean): SessionActivityRequest {
  if ((json === undefined) || (json === null)) {
    return json;
  }
  return {

    'appId': json['appId'],
    'sessionName': json['sessionName']
  };
}

export function SessionActivityRequestToJSON(value?: SessionActivityRequest | null): any {
  if (value === undefined) {
    return undefined;
  }
  if (value === null) {
    return null;
  }
  return {

    'appId': value.appId,
    'sessionName': value.sessionName
  };
}

