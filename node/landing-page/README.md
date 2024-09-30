# Theia Cloud - Landing Page

At TUM, we configured this Landing Page to welcome all users into the Theia environment. As we have multiple user groups profiting from Theia, there are two common configurations and behaviours for this page.

## Configuration through Query Parameters

`appDef` (optional) is used to pass which blueprint should be started and must match the `appId` set in the landing page's configuration (either `config.js` or K8s manifest). In our setup, it's always the image's URI (*ghcr.io/ls1intum/theia/blueprint:latest*).

`gitUri` (optional) contains the clone URI for the user's repository. It's used to automatically setup the working environment when/before the users enters Theia.

`gitToken` (optional) should be filled with the the user's *personal access token* which can we automatically generated within Artemis. This is primarily used to clone and push afterwards.

## Use-Cases
Dependent on the available query parameters, the landing page is configured to its different use-cases. 

### Artemis Exercise Workflow
For Artemis users, Theia should be as transparent as possible - making use of automated authentication, automated image selection and repo cloning in regards to the currently opened exercise, and instant session start. When a user is logged in with KeyCloak, the landing page should not require any interaction from the user. 

This mode of execution is started when the following parameters are set:
- `appDef`
- `gitUri`
- `gitToken`

### Guest / School Users
Users not in the Artemis flow want to understand what is happening, have more power of choice, and want a more slow-paced environment. Thus, no pre-selection or automatic startup takes place. After login, the user is presented with a graphically pleasing way to select the fitting image.

This mode of execution is used as a fallback when not all three parameters are set.

## Development

Install dependencies in the parent directory defining the monorepo.
Also, build the monorepo to build and link the common package

```bash
cd ..
npm ci
npm run build
```
