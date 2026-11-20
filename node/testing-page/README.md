# Theia Cloud Testing App

This folder contains a simple test app that allows to login via Keycloak and trigger various Theia Cloud endpoints (e.g. session start or stop).

## Usage

First, adapt the constants `KEYCLOAK_CONFIG` and `SERVICE_URL` at the top of [App.tsx](./src/App.tsx) to fit your deployment.

Install dependencies and build the monorepo in the parent directory. This also builds and links the common package:

```bash
cd ..
npm ci
npm run build
```

Start the app's dev server:

```bash
cd testing-page
npm run dev
```

Open <http://localhost:3000>

Click the Login button to login via Keycloak.
Do not login to test anonymous requests.
If the user input is filled, this is sent as the user in requests.
If the input is empty, the logged in user is used.
Specify a user different from the logged in one to test authorization of the service (i.e. that you cannot stop sessions of another user).

Use your browser's DevTools to see outgoing requests and their results.
Return values of outgoing requests are also logged to the console.

## Tech Stack

The page is based on [ViteJS](https://vitejs.dev/) with React and Typescript.
