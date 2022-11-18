# Monitor

VSCode extension to monitor Theia.cloud hosted VSCode/Theia products. The extension will expose a rest service, where an external service can ping the extension to get information about the pod.

The extension is designed to allow other modules to be added in the future.

For this the extension will read out environment variables to check if a specific module should be activated.

For an example of how these environment variables should be set, take a look at `Run extension` launch config the `.vscode/launch.json` file.

```json
"THEIACLOUD_MONITOR_PORT": "8081",
"THEIACLOUD_MONITOR_ENABLE_ACTIVITY_TRACKER": "1"
```

## List of modules

|Name|description|ENV to enable|
|---|---|---|
|MessagingModule|Allows the backend to send messages to be displayed to the user|(always enabled)|
|ActivityTrackerModule|Allows the backend to ask about the users last activity and show a warning if the pod is about to be timed out|THEIACLOUD_MONITOR_ENABLE_ACTIVITY_TRACKER|

## Debugging the extension

Open this folder in vscode. A `Run Extension` debug config will be available. This will open a new VSCode instance with the extension installed.

## Bundle the extension

Run `yarn bundle` to create a `vsix` file that can be installed in VSCode/Theia.
