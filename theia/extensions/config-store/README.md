# Backend Communication Example

The example extension demonstrates how to communicate with backend services.
It contains the following two backend service examples:

- `ConfigStoreServer`: Can be called by the client to create a "Hello World" string. The client provides a name as a parameter, and the server returns the name prefixed with "Hello" (ex: "World" as a parameter returns "Hello World").
- `ConfigStoreWithClientService`: Same as `ConfigStoreServer`, but the name parameter is not directly passed by the client in the first call. Instead, the backend services retrieves the name to say "Hello" to from the client again (`BackendClient`). This example shows how to implement call backs from the backend to the client.
  Further, the example contributes two commands to trigger both types of backend calls.

## How to use the backend communication example

In the running application, trigger the command "Say hello on the backend" or "Say hello on the backend with a callback to the client" via the command palette (F1 => "Say Hello"). A message will be printed out on the console from where you launched the application.
