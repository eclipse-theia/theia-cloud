# UserWorkspace
## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
| **name** | **String** | The name of the workspace | [default to null] |
| **label** | **String** | The label of the workspace | [default to null] |
| **appDefinition** | **String** | The app this workspace was used with. | [optional] [default to null] |
| **user** | **String** | The user identification, usually the email address. | [default to null] |
| **active** | **Boolean** | Whether the workspace is in use at the moment. | [default to null] |
| **lastUsed** | **String** | Time the operator last observed the workspace enter or leave use by a running session. | [optional] |
| **inUse** | **Boolean** | Whether a running session uses this workspace. | [optional] |

The workspace list sets `active` as soon as a Session references the workspace. The operator sets `inUse` once a session is ready and records `lastUsed` when the workspace enters or leaves use.

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)

