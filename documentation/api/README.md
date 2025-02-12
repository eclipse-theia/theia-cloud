# Documentation for Theia Cloud API

<a name="documentation-for-api-endpoints"></a>
## Documentation for API Endpoints

All URIs are relative to *http://localhost*

| Class | Method | HTTP request | Description |
|------------ | ------------- | ------------- | -------------|
| *AppDefinitionAdminResourceApi* | [**serviceAdminAppdefinitionAppDefinitionNamePatch**](Apis/AppDefinitionAdminResourceApi.md#serviceadminappdefinitionappdefinitionnamepatch) | **PATCH** /service/admin/appdefinition/{appDefinitionName} | Updates an app definition |
| *AppDefinitionResourceApi* | [**serviceAppdefinitionAppIdGet**](Apis/AppDefinitionResourceApi.md#serviceappdefinitionappidget) | **GET** /service/appdefinition/{appId} | List app definitions |
| *RootAdminResourceApi* | [**serviceAdminAppIdGet**](Apis/RootAdminResourceApi.md#serviceadminappidget) | **GET** /service/admin/{appId} | Admin Ping |
| *RootResourceApi* | [**serviceAppIdGet**](Apis/RootResourceApi.md#serviceappidget) | **GET** /service/{appId} | Ping |
*RootResourceApi* | [**servicePost**](Apis/RootResourceApi.md#servicepost) | **POST** /service | Launch Session |
| *SessionResourceApi* | [**serviceSessionAppIdUserGet**](Apis/SessionResourceApi.md#servicesessionappiduserget) | **GET** /service/session/{appId}/{user} | List sessions |
*SessionResourceApi* | [**serviceSessionDelete**](Apis/SessionResourceApi.md#servicesessiondelete) | **DELETE** /service/session | Stop session |
*SessionResourceApi* | [**serviceSessionPatch**](Apis/SessionResourceApi.md#servicesessionpatch) | **PATCH** /service/session | Report session activity |
*SessionResourceApi* | [**serviceSessionPerformanceAppIdSessionNameGet**](Apis/SessionResourceApi.md#servicesessionperformanceappidsessionnameget) | **GET** /service/session/performance/{appId}/{sessionName} | Get performance metrics |
*SessionResourceApi* | [**serviceSessionPost**](Apis/SessionResourceApi.md#servicesessionpost) | **POST** /service/session | Start a new session |
| *WorkspaceResourceApi* | [**serviceWorkspaceAppIdUserGet**](Apis/WorkspaceResourceApi.md#serviceworkspaceappiduserget) | **GET** /service/workspace/{appId}/{user} | List workspaces |
*WorkspaceResourceApi* | [**serviceWorkspaceDelete**](Apis/WorkspaceResourceApi.md#serviceworkspacedelete) | **DELETE** /service/workspace | Delete workspace |
*WorkspaceResourceApi* | [**serviceWorkspacePost**](Apis/WorkspaceResourceApi.md#serviceworkspacepost) | **POST** /service/workspace | Create workspace |


<a name="documentation-for-models"></a>
## Documentation for Models

 - [ActivityTracker](./Models/ActivityTracker.md)
 - [AppDefinition](./Models/AppDefinition.md)
 - [AppDefinitionListRequest](./Models/AppDefinitionListRequest.md)
 - [AppDefinitionSpec](./Models/AppDefinitionSpec.md)
 - [AppDefinitionStatus](./Models/AppDefinitionStatus.md)
 - [AppDefinitionUpdateRequest](./Models/AppDefinitionUpdateRequest.md)
 - [EnvironmentVars](./Models/EnvironmentVars.md)
 - [LaunchRequest](./Models/LaunchRequest.md)
 - [ManagedFieldsEntry](./Models/ManagedFieldsEntry.md)
 - [Monitor](./Models/Monitor.md)
 - [ObjectMeta](./Models/ObjectMeta.md)
 - [OwnerReference](./Models/OwnerReference.md)
 - [PingRequest](./Models/PingRequest.md)
 - [SessionActivityRequest](./Models/SessionActivityRequest.md)
 - [SessionListRequest](./Models/SessionListRequest.md)
 - [SessionPerformance](./Models/SessionPerformance.md)
 - [SessionPerformanceRequest](./Models/SessionPerformanceRequest.md)
 - [SessionSpec](./Models/SessionSpec.md)
 - [SessionStartRequest](./Models/SessionStartRequest.md)
 - [SessionStopRequest](./Models/SessionStopRequest.md)
 - [UserWorkspace](./Models/UserWorkspace.md)
 - [WorkspaceCreationRequest](./Models/WorkspaceCreationRequest.md)
 - [WorkspaceDeletionRequest](./Models/WorkspaceDeletionRequest.md)
 - [WorkspaceListRequest](./Models/WorkspaceListRequest.md)


<a name="documentation-for-authorization"></a>
## Documentation for Authorization

<a name="SecurityScheme"></a>
### SecurityScheme

- **Type**: OAuth
- **Flow**: implicit
- **Authorization URL**: 
- **Scopes**: N/A

