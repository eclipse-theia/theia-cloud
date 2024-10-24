# LaunchRequest
## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
| **appId** | **String** | The App Id of this Theia Cloud instance. Request without a matching Id will be denied. | [default to null] |
| **user** | **String** | The user identification, usually the email address. | [default to null] |
| **appDefinition** | **String** | The app to launch. Needs to be set if a new or ephemeral session should be launched. For an existing workspace the last app definition will be used if none is given. | [optional] [default to null] |
| **workspaceName** | **String** | The name of the workspace to mount/create. Needs to be set if an existing workspace should be launched. | [optional] [default to null] |
| **label** | **String** | The label of the workspace to mount/create. If no label is given, a default label will be generated. | [optional] [default to null] |
| **ephemeral** | **Boolean** | If true no workspace will be created for the session. | [optional] [default to null] |
| **timeout** | **Integer** | Number of minutes to wait for session launch. Default is 3 Minutes. | [optional] [default to null] |
| **env** | [**EnvironmentVars**](EnvironmentVars.md) | Environment variables | [optional] [default to null] |

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)

