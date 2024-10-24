# SessionStartRequest
## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
| **appId** | **String** | The App Id of this Theia Cloud instance. Request without a matching Id will be denied. | [default to null] |
| **user** | **String** | The user identification, usually the email address. | [default to null] |
| **appDefinition** | **String** | The app to launch. | [default to null] |
| **workspaceName** | **String** | The name of the workspace to mount/create. | [optional] [default to null] |
| **timeout** | **Integer** | Number of minutes to wait for session launch. Default is 3 Minutes. | [optional] [default to null] |
| **env** | [**EnvironmentVars**](EnvironmentVars.md) | Environment variables | [optional] [default to null] |

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)

