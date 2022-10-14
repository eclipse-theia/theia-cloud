# activity-tracker

VSCode extension to keep track of user activity inside of a VSCode/Theia product. The extension will expose a rest service, where an external service can ping the extension to get the last activity.

## Debugging the extension

Open this folder in vscode. A `Run Extension` debug config will be available. This will open a new VSCode instance with the extension installed.

## Bundle the extension

Run `yarn bundle` to create a `vsix` file that can be installed in VSCode/Theia.
