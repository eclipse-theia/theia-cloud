# Config Store Extension

The Config Store extension provides a mechanism to store and manage configuration values within a Theia application. It allows clients to set, get, and remove configuration values, and notifies clients about changes to these values.

## Features

- Store and retrieve configuration values.
- Notify clients about configuration value changes.
- REST API for setting and unsetting values: This is the crucial feature for the Theia Cloud use case because this enables injecting configuration (e.g. credentials) when a session pod is already running.

## Installation

To install the Config Store extension, add it to your Theia application's package.json dependencies:

```json
{
  "dependencies": {
    "@eclipse-theiacloud/config-store": "1.2.0-next"
  }
}
```

## Usage

For an example on how to listen to change events and set, get, unset values of the config store,
see the [example extension](../../examples/config-store-example/).

### REST API

The Config Store extension exposes a REST endpoint at `/theia-cloud/config-store` for managing configuration values. You can use this API to set and remove configuration values. It is not possible to read out values via the REST endpoint. This is done to protect potentially sensitive values (e.g. credentials).

Assume your Theia instance is locally running at <http://localhost:3000>.
The endpoint then is <<http://localhost:3000//theia-cloud/config-store>.

Set a value via a `POST` request with a JSON body like so:

```json
{
  "key": "my_value_key",
  "value": "my_value"
}
```

Unset a value via a POST request to the same endpoint like so:

```json
{
  "key": "my_value_key",
  "value": null
}
```

A simple `GET` endpoint is also provided to allow checking that the config server is available.
