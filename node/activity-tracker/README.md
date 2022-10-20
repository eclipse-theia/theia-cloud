# activity-tracker

VSCode extension to keep track of user activity inside of a VSCode/Theia product. The extension will expose a rest service, where an external service can ping the extension to get the last activity.

The extension is designed to allow other modules to be added in the future.

For this the extension will read out environment variables to check if a specific module should be activated.

For an example of how these environment variables should be set, take a look at `Run extension` launch config the `.vscode/launch.json` file.

```json
"THEIA_CLOUD_ACTIVITY_SERVICE_HOST": "localhost",
"THEIA_CLOUD_ACTIVITY_SERVICE_PORT": "8081",
"THEIA_CLOUD_ACTIVITY_SERVICE_ENABLE_TRACKER": "1"
```

## Debugging the extension

Open this folder in vscode. A `Run Extension` debug config will be available. This will open a new VSCode instance with the extension installed.

## Bundle the extension

Run `yarn bundle` to create a `vsix` file that can be installed in VSCode/Theia.
