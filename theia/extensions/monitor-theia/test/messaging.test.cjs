const assert = require('node:assert/strict');
const { once } = require('node:events');
const { after, before, beforeEach, describe, it, mock } = require('node:test');
const express = require('express');

require('@theia/core/shared/reflect-metadata');

const { MessagingBackendModuleImpl } = require('../lib/node/modules/messaging-backend-module-impl');

const previousSecret = process.env.THEIACLOUD_SESSION_SECRET;

describe('POST /monitor/message', () => {
  let server;
  let url;
  let responseErrors;
  const displayMessage = mock.fn();

  before(async () => {
    process.env.THEIACLOUD_SESSION_SECRET = 'monitor-test-secret';
    const module = new MessagingBackendModuleImpl();
    module.setClient({ displayMessage });
    const app = express();
    app.set('env', 'test');
    app.use((req, res, next) => {
      res.on('error', error => responseErrors.push(error));
      next();
    });
    app.use('/monitor', module.registerEndpoints(express.Router()));
    server = app.listen(0, '127.0.0.1');
    await once(server, 'listening');
    url = `http://127.0.0.1:${server.address().port}/monitor/message`;
  });

  beforeEach(() => {
    displayMessage.mock.resetCalls();
    responseErrors = [];
  });

  after(async () => {
    if (server) {
      await new Promise((resolve, reject) => server.close(error => (error ? reject(error) : resolve())));
    }
    MessagingBackendModuleImpl.clients = [];
    if (previousSecret === undefined) {
      delete process.env.THEIACLOUD_SESSION_SECRET;
    } else {
      process.env.THEIACLOUD_SESSION_SECRET = previousSecret;
    }
  });

  async function post(body, authorization = 'Bearer monitor-test-secret', contentType = 'application/json') {
    const headers = { 'Content-Type': contentType };
    if (authorization) {
      headers.Authorization = authorization;
    }
    return fetch(url, { method: 'POST', headers, body });
  }

  for (const [name, body] of [
    ['empty object', {}],
    ['missing level', { message: 'Hello' }],
    ['missing message', { level: 'info' }],
    ['empty level', { level: '', message: 'Hello' }],
    ['empty message', { level: 'info', message: '' }],
    ['null level', { level: null, message: 'Hello' }],
    ['null message', { level: 'info', message: null }],
    ['array', []],
    ['absent body', undefined]
  ]) {
    it(`returns 400 without delivering a message for ${name}`, async () => {
      const response = await post(JSON.stringify(body));
      assert.equal(response.status, 400);
      assert.equal(await response.text(), "Body missing 'level' or 'message' fields");
      assert.equal(displayMessage.mock.callCount(), 0);
      assert.deepEqual(responseErrors, []);
    });
  }

  for (const body of ['{', 'null', '"Hello"']) {
    it(`returns 400 for invalid JSON input ${body}`, async () => {
      const response = await post(body);
      assert.equal(response.status, 400);
      await response.text();
      assert.equal(displayMessage.mock.callCount(), 0);
      assert.deepEqual(responseErrors, []);
    });
  }

  it('returns 400 for a body that is not parsed as JSON', async () => {
    const response = await post('Hello', 'Bearer monitor-test-secret', 'text/plain');
    assert.equal(response.status, 400);
    await response.text();
    assert.equal(displayMessage.mock.callCount(), 0);
    assert.deepEqual(responseErrors, []);
  });

  for (const authorization of ['', 'Bearer wrong-secret']) {
    for (const body of [{}, { level: 'info', message: 'Hello' }]) {
      it(`returns 401 for ${JSON.stringify(body)} with ${authorization || 'no authorization'}`, async () => {
        const response = await post(JSON.stringify(body), authorization);
        assert.equal(response.status, 401);
        assert.equal(await response.text(), 'Unauthorized');
        assert.equal(displayMessage.mock.callCount(), 0);
        assert.deepEqual(responseErrors, []);
      });
    }
  }

  for (const level of ['info', 'warn', 'error']) {
    for (const fullscreen of [undefined, false, true]) {
      it(`delivers ${level} with fullscreen=${fullscreen} exactly once and returns 200`, async () => {
        const body = { level, message: 'Hello', fullscreen };
        if (fullscreen) {
          body.detail = 'Details';
        }
        const response = await post(JSON.stringify(body));
        assert.equal(response.status, 200);
        assert.equal(await response.text(), 'success');
        assert.equal(displayMessage.mock.callCount(), 1);
        assert.deepEqual(displayMessage.mock.calls[0].arguments, [level, body.message, body.detail, fullscreen]);
        assert.deepEqual(responseErrors, []);
      });
    }
  }
});
