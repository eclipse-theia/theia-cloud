# Monitor

VSCode extension to monitor Theia Cloud hosted VSCode/Theia products.
The extension will expose a rest service, where an external service can ping the extension to get information about the pod.

The extension is designed to facilitate the addition of further modules in the future.

For this, the extension reads environment variables to check the activation of each corresponding module.

For an example on setting these environment variables, take a look at the `Run extension` launch config in the `.vscode/launch.json` file.

```json
"THEIA_CLOUD_MONITOR_PORT": "8081",
"THEIA_CLOUD_MONITOR_ENABLE_ACTIVITY_TRACKER": "true"
```

## List of modules

| Name                  | description                                                                                                    | ENV to enable                               |
| --------------------- | -------------------------------------------------------------------------------------------------------------- | ------------------------------------------- |
| MessagingModule       | Allows the backend to send messages to be displayed to the user                                                | (always enabled)                            |
| ActivityTrackerModule | Allows the backend to ask about the users last activity and show a warning if the pod is about to be timed out | THEIA_CLOUD_MONITOR_ENABLE_ACTIVITY_TRACKER |

## Debugging the extension

Open this folder in vscode. A `Run Extension` debug config will be available. This will open a new VSCode instance with the extension installed.

## Bundle the extension

Run `npm run build:vsix` to create a `vsix` file that can be installed in VSCode/Theia.
Run `npm run update:demo:monitor` to update the `vsix` file in the `demo-theia-monitor-vscode` component.
