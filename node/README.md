# Theia.Cloud Web Packages

This folder is a monorepo containing a common web api and UIs.
It is based on [npm workspaces](https://docs.npmjs.com/cli/v8/using-npm/workspaces#running-commands-in-the-context-of-workspaces).

## Requirements

- Node **16** or higher. This is important because npm workspaces do not work with lower versions!

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
