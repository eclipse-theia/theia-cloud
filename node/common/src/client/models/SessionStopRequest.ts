/* tslint:disable */
/**
 * A request to stop a session
 * @export
 * @interface SessionStopRequest
 */
export interface SessionStopRequest {
    /**
     * The App Id of this Theia.cloud instance. Request without a matching Id will be denied.
     * @type {string}
     * @memberof SessionStopRequest
     */
    appId: string;
    /**
     * The user identification, usually the email address.
     * @type {string}
     * @memberof SessionStopRequest
     */
    user: string;
    /**
     * The name of the session to stop.
     * @type {string}
     * @memberof SessionStopRequest
     */
    sessionName: string;
}

/**
 * Check if a given object implements the SessionStopRequest interface.
 */
export function instanceOfSessionStopRequest(value: object): boolean {
  let isInstance = true;
  isInstance = isInstance && 'appId' in value;
  isInstance = isInstance && 'user' in value;
  isInstance = isInstance && 'sessionName' in value;

  return isInstance;
}

export function SessionStopRequestFromJSON(json: any): SessionStopRequest {
  return SessionStopRequestFromJSONTyped(json, false);
}

export function SessionStopRequestFromJSONTyped(json: any, ignoreDiscriminator: boolean): SessionStopRequest {
  if ((json === undefined) || (json === null)) {
    return json;
  }
  return {

    'appId': json['appId'],
    'user': json['user'],
    'sessionName': json['sessionName']
  };
}

export function SessionStopRequestToJSON(value?: SessionStopRequest | null): any {
  if (value === undefined) {
    return undefined;
  }
  if (value === null) {
    return null;
  }
  return {

    'appId': value.appId,
    'user': value.user,
    'sessionName': value.sessionName
  };
}

