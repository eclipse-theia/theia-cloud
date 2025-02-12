# WorkspaceResourceApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**serviceWorkspaceAppIdUserGet**](WorkspaceResourceApi.md#serviceWorkspaceAppIdUserGet) | **GET** /service/workspace/{appId}/{user} | List workspaces |
| [**serviceWorkspaceDelete**](WorkspaceResourceApi.md#serviceWorkspaceDelete) | **DELETE** /service/workspace | Delete workspace |
| [**serviceWorkspacePost**](WorkspaceResourceApi.md#serviceWorkspacePost) | **POST** /service/workspace | Create workspace |


<a name="serviceWorkspaceAppIdUserGet"></a>
# **serviceWorkspaceAppIdUserGet**
> List serviceWorkspaceAppIdUserGet(appId, user)

List workspaces

    Lists the workspaces of a user.

### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **appId** | **String**|  | [default to null] |
| **user** | **String**|  | [default to null] |

### Return type

[**List**](../Models/UserWorkspace.md)

### Authorization

[SecurityScheme](../README.md#SecurityScheme)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="serviceWorkspaceDelete"></a>
# **serviceWorkspaceDelete**
> Boolean serviceWorkspaceDelete(WorkspaceDeletionRequest)

Delete workspace

    Deletes a workspace.

### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **WorkspaceDeletionRequest** | [**WorkspaceDeletionRequest**](../Models/WorkspaceDeletionRequest.md)|  | |

### Return type

**Boolean**

### Authorization

[SecurityScheme](../README.md#SecurityScheme)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: text/plain

<a name="serviceWorkspacePost"></a>
# **serviceWorkspacePost**
> UserWorkspace serviceWorkspacePost(WorkspaceCreationRequest)

Create workspace

    Creates a new workspace for a user.

### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **WorkspaceCreationRequest** | [**WorkspaceCreationRequest**](../Models/WorkspaceCreationRequest.md)|  | |

### Return type

[**UserWorkspace**](../Models/UserWorkspace.md)

### Authorization

[SecurityScheme](../README.md#SecurityScheme)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

