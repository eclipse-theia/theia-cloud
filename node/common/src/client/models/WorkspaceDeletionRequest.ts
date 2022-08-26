/* tslint:disable */
/**
 * Request to delete a workspace
 * @export
 * @interface WorkspaceDeletionRequest
 */
export interface WorkspaceDeletionRequest {
    /**
     * The App Id of this Theia.cloud instance. Request without a matching Id will be denied.
     * @type {string}
     * @memberof WorkspaceDeletionRequest
     */
    appId: string;
    /**
     * The user identification, usually the email address.
     * @type {string}
     * @memberof WorkspaceDeletionRequest
     */
    user: string;
    /**
     * The name of the workspace to delete.
     * @type {string}
     * @memberof WorkspaceDeletionRequest
     */
    workspaceName: string;
}

/**
 * Check if a given object implements the WorkspaceDeletionRequest interface.
 */
export function instanceOfWorkspaceDeletionRequest(value: object): boolean {
  let isInstance = true;
  isInstance = isInstance && 'appId' in value;
  isInstance = isInstance && 'user' in value;
  isInstance = isInstance && 'workspaceName' in value;

  return isInstance;
}

export function WorkspaceDeletionRequestFromJSON(json: any): WorkspaceDeletionRequest {
  return WorkspaceDeletionRequestFromJSONTyped(json, false);
}

export function WorkspaceDeletionRequestFromJSONTyped(json: any, ignoreDiscriminator: boolean): WorkspaceDeletionRequest {
  if ((json === undefined) || (json === null)) {
    return json;
  }
  return {

    'appId': json['appId'],
    'user': json['user'],
    'workspaceName': json['workspaceName']
  };
}

export function WorkspaceDeletionRequestToJSON(value?: WorkspaceDeletionRequest | null): any {
  if (value === undefined) {
    return undefined;
  }
  if (value === null) {
    return null;
  }
  return {

    'appId': value.appId,
    'user': value.user,
    'workspaceName': value.workspaceName
  };
}

