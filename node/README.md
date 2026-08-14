# Theia Cloud Web Packages

This folder is a monorepo containing a common web api and UIs.
It is based on [npm workspaces](https://docs.npmjs.com/cli/v8/using-npm/workspaces#running-commands-in-the-context-of-workspaces).

## Requirements

- Node **24** (see [.nvmrc](./.nvmrc)). This is enforced via the `engines` field and `engine-strict=true` in [.npmrc](./.npmrc), so `npm ci` fails on older versions.

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

## Run Landing Page

```bash
npm run start -w landing-page
```
