/* tslint:disable */
/**
 * A request to list the sessions of a user.
 * @export
 * @interface SessionListRequest
 */
export interface SessionListRequest {
    /**
     * The App Id of this Theia.cloud instance. Request without a matching Id will be denied.
     * @type {string}
     * @memberof SessionListRequest
     */
    appId: string;
    /**
     * The user identification, usually the email address.
     * @type {string}
     * @memberof SessionListRequest
     */
    user: string;
}

/**
 * Check if a given object implements the SessionListRequest interface.
 */
export function instanceOfSessionListRequest(value: object): boolean {
  let isInstance = true;
  isInstance = isInstance && 'appId' in value;
  isInstance = isInstance && 'user' in value;

  return isInstance;
}

export function SessionListRequestFromJSON(json: any): SessionListRequest {
  return SessionListRequestFromJSONTyped(json, false);
}

export function SessionListRequestFromJSONTyped(json: any, ignoreDiscriminator: boolean): SessionListRequest {
  if ((json === undefined) || (json === null)) {
    return json;
  }
  return {

    'appId': json['appId'],
    'user': json['user']
  };
}

export function SessionListRequestToJSON(value?: SessionListRequest | null): any {
  if (value === undefined) {
    return undefined;
  }
  if (value === null) {
    return null;
  }
  return {

    'appId': value.appId,
    'user': value.user
  };
}

