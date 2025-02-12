# SessionResourceApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**serviceSessionAppIdUserGet**](SessionResourceApi.md#serviceSessionAppIdUserGet) | **GET** /service/session/{appId}/{user} | List sessions |
| [**serviceSessionDelete**](SessionResourceApi.md#serviceSessionDelete) | **DELETE** /service/session | Stop session |
| [**serviceSessionPatch**](SessionResourceApi.md#serviceSessionPatch) | **PATCH** /service/session | Report session activity |
| [**serviceSessionPerformanceAppIdSessionNameGet**](SessionResourceApi.md#serviceSessionPerformanceAppIdSessionNameGet) | **GET** /service/session/performance/{appId}/{sessionName} | Get performance metrics |
| [**serviceSessionPost**](SessionResourceApi.md#serviceSessionPost) | **POST** /service/session | Start a new session |


<a name="serviceSessionAppIdUserGet"></a>
# **serviceSessionAppIdUserGet**
> List serviceSessionAppIdUserGet(appId, user)

List sessions

    List sessions of a user.

### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **appId** | **String**|  | [default to null] |
| **user** | **String**|  | [default to null] |

### Return type

[**List**](../Models/SessionSpec.md)

### Authorization

[SecurityScheme](../README.md#SecurityScheme)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="serviceSessionDelete"></a>
# **serviceSessionDelete**
> Boolean serviceSessionDelete(SessionStopRequest)

Stop session

    Stops a session.

### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **SessionStopRequest** | [**SessionStopRequest**](../Models/SessionStopRequest.md)|  | |

### Return type

**Boolean**

### Authorization

[SecurityScheme](../README.md#SecurityScheme)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: text/plain

<a name="serviceSessionPatch"></a>
# **serviceSessionPatch**
> Boolean serviceSessionPatch(SessionActivityRequest)

Report session activity

    Updates the last activity timestamp for a session to monitor activity.

### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **SessionActivityRequest** | [**SessionActivityRequest**](../Models/SessionActivityRequest.md)|  | |

### Return type

**Boolean**

### Authorization

[SecurityScheme](../README.md#SecurityScheme)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: text/plain

<a name="serviceSessionPerformanceAppIdSessionNameGet"></a>
# **serviceSessionPerformanceAppIdSessionNameGet**
> SessionPerformance serviceSessionPerformanceAppIdSessionNameGet(appId, sessionName)

Get performance metrics

    Returns the current CPU and memory usage of the session&#39;s pod.

### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **appId** | **String**|  | [default to null] |
| **sessionName** | **String**|  | [default to null] |

### Return type

[**SessionPerformance**](../Models/SessionPerformance.md)

### Authorization

[SecurityScheme](../README.md#SecurityScheme)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="serviceSessionPost"></a>
# **serviceSessionPost**
> String serviceSessionPost(SessionStartRequest)

Start a new session

    Starts a new session for an existing workspace and responds with the URL of the started session.

### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **SessionStartRequest** | [**SessionStartRequest**](../Models/SessionStartRequest.md)|  | |

### Return type

**String**

### Authorization

[SecurityScheme](../README.md#SecurityScheme)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: text/plain

