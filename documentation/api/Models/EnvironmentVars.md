# EnvironmentVars
## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
| **fromMap** | **Map** | Map of environment variables to be passed to Deployment.  Ignored if Theia applications are started eagerly.  Empty by default. | [optional] [default to null] |
| **fromConfigMaps** | **List** | List of ConfigMaps (by name) containing environment variables to be passed to Deployment as envFrom.configMapRef.  Ignored if Theia applications are started eagerly.  Empty by default. | [optional] [default to null] |
| **fromSecrets** | **List** | List of Secrets (by name) containing environment variables to be passed to Deployment as envFrom.secretRef.  Ignored if Theia applications are started eagerly.  Empty by default. | [optional] [default to null] |

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)

