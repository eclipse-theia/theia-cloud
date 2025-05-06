# RootResourceApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**serviceAppIdGet**](RootResourceApi.md#serviceAppIdGet) | **GET** /service/{appId} | Ping |
| [**servicePost**](RootResourceApi.md#servicePost) | **POST** /service | Launch Session |


<a name="serviceAppIdGet"></a>
# **serviceAppIdGet**
> Boolean serviceAppIdGet(appId)

Ping

    Replies if the service is available.

### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **appId** | **String**|  | [default to null] |

### Return type

**Boolean**

### Authorization

[SecurityScheme](../README.md#SecurityScheme)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: text/plain

<a name="servicePost"></a>
# **servicePost**
> String servicePost(LaunchRequest)

Launch Session

    Launches a session and creates a workspace if required. Responds with the URL of the launched session.

### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **LaunchRequest** | [**LaunchRequest**](../Models/LaunchRequest.md)|  | |

### Return type

**String**

### Authorization

[SecurityScheme](../README.md#SecurityScheme)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: text/plain

