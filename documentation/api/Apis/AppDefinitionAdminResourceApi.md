# AppDefinitionAdminResourceApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**serviceAdminAppdefinitionAppDefinitionNamePatch**](AppDefinitionAdminResourceApi.md#serviceAdminAppdefinitionAppDefinitionNamePatch) | **PATCH** /service/admin/appdefinition/{appDefinitionName} | Updates an app definition |


<a name="serviceAdminAppdefinitionAppDefinitionNamePatch"></a>
# **serviceAdminAppdefinitionAppDefinitionNamePatch**
> AppDefinition serviceAdminAppdefinitionAppDefinitionNamePatch(appDefinitionName, AppDefinitionUpdateRequest)

Updates an app definition

    Updates an app definition&#39;s properties. Allowed properties to update are defined by AppDefinitionUpdateRequest.

### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **appDefinitionName** | **String**| The K8S resource name of the app definition to update. | [default to null] |
| **AppDefinitionUpdateRequest** | [**AppDefinitionUpdateRequest**](../Models/AppDefinitionUpdateRequest.md)|  | |

### Return type

[**AppDefinition**](../Models/AppDefinition.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

