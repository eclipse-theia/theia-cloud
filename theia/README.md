# Theia Cloud Theia Extensions

This folder contains Theia extensions that may be useful in Theia applications deployed via Theia cloud.
The folder also contains a Theia example browser app featuring all extensions developed here
as well as further example extensions using them.

## Contents

- [configs](./configs/): Common configs such as ESLint, prettier, and licensing configs
- [examples](./examples/): The browser example app and test extensions that facilitate testing the developed extensions
- [extensions](./extensions/): The Theia extensions developed for Theia apps running in Theia Cloud
  - [config-store](./extensions/config-store/): Adds a REST endpoint to inject data (e.g. user credentials) into the Theia instance. Provides clients to set/unset/get values and get notified of changes to configured values.
  - [monitor-theia](./extensions/monitor-theia/): Adds REST endpoints to Theia to monitor activity and send messages to display to the user.

## Setup

Install dependencies and build

```sh
yarn
```

## Run

Run example app with extensions

```sh
yarn start
```

Access app at <http://localhost:3000>.

## Development

Rebuild extensions and app without re-installing dependencies

```sh
yarn build
```

Lint code with ESLint

```sh
yarn lint
```

Watch all extensions for file changes and automatically rebuild

```sh
yarn watch
```
