# Theia Cloud Testing App

This folder contains a simple test app that allows to login via Keycloak and trigger various Theia Cloud endpoints (e.g. session start or stop).

## Usage

First, adapt the constants `KEYCLOAK_CONFIG` and `SERVICE_URL` at the top of [App.tsx](./src/App.tsx) to fit your deployment.

Install dependencies and start app:

```bash
npm ci
npm start
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

This project was bootstrapped with [Create React App](https://github.com/facebook/create-react-app).

You can learn more in the [Create React App documentation](https://facebook.github.io/create-react-app/docs/getting-started).

To learn React, check out the [React documentation](https://reactjs.org/).
