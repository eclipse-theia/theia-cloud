# Theia.Cloud Web Packages

This folder is a monorepo containing a common web api and UIs.
It is based on [npm workspaces](https://docs.npmjs.com/cli/v8/using-npm/workspaces#running-commands-in-the-context-of-workspaces).

## Requirements

- Node **16.0.0**. This is important because npm workspaces do not work with lower versions!

## Switch to Node 16 with nvm

```bash
nvm use 16.0.0
```

Please use the exact Node version as stated above. Newer Node versions may throw the following error when you install the dependencies: `error TS2307: Cannot find module '@eclipse-theiacloud/common/lib' or its corresponding type declarations.`

## Install dependencies

```bash
npm ci
```

## Build

```bash
npm run build
```

or build only a single package

```bash
npm run build -w <folder>
```

## Run Try Now Page

```bash
npm run start -w try-now-page
```
