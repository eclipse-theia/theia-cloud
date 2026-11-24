const assert = require('node:assert/strict');
const { it } = require('node:test');

require('@theia/core/shared/reflect-metadata');
const { CommandRegistry } = require('@theia/core');
const { Container } = require('@theia/core/shared/inversify');
const { FrontendApplicationContribution } = require('@theia/core/lib/browser/frontend-application-contribution');
const { RemoteConnectionProvider } = require('@theia/core/lib/browser/messaging/service-connection-provider');
const { WebSocketConnectionProvider } = require('@theia/core/lib/browser/messaging/ws-connection-provider');

// This test file runs in its own Node process. Supply only the browser exports
// used by the bindings, so importing the module doesn't load DOM widgets or CSS.
require.cache[require.resolve('@theia/core/lib/browser')] = {
  exports: { FrontendApplicationContribution, WebSocketConnectionProvider }
};
require.cache[require.resolve('../lib/browser/monitor-dialog')] = { exports: {} };

const frontendModule = require('../lib/browser/monitor-frontend-module').default;
const { ActivityTrackerFrontendContribution } = require('../lib/browser/modules/activity-tracker-frontend-module-impl');
const { MessagingFrontendContribution } = require('../lib/browser/modules/messaging-frontend-module-impl');
const { ActivityTrackerBackendModule, ActivityTrackerFrontendModule } = require('../lib/common/modules/activity-tracker-module');
const { MessagingBackendModule, MessagingFrontendModule } = require('../lib/common/modules/messaging-module');
const { TheiaCloudBackendMonitorService } = require('../lib/common/monitor-protocol');

it('resolves each application contribution through its concrete singleton binding', () => {
  const container = new Container();
  container.load(frontendModule);
  container.bind(CommandRegistry).toConstantValue({});
  for (const token of [TheiaCloudBackendMonitorService, ActivityTrackerBackendModule, MessagingBackendModule]) {
    container.rebind(token).toConstantValue({});
  }

  const messaging = container.get(MessagingFrontendContribution);
  const tracker = container.get(ActivityTrackerFrontendContribution);
  const defaults = container.getAll(FrontendApplicationContribution);
  assert.equal(defaults.length, 2);
  assert.ok(defaults.includes(tracker));
  assert.ok(defaults.includes(messaging));

  const customTracker = { initialize() {} };
  container.rebind(ActivityTrackerFrontendContribution).toConstantValue(customTracker);
  const contributions = container.getAll(FrontendApplicationContribution);
  assert.equal(contributions.length, 2);
  assert.ok(contributions.includes(customTracker));
  assert.ok(contributions.includes(messaging));
});

it('uses downstream frontend clients for the monitor RPC proxies', () => {
  const container = new Container();
  container.load(frontendModule);
  container.bind(RemoteConnectionProvider).toConstantValue({ createProxy: (_path, client) => client });
  for (const [frontend, backend] of [
    [ActivityTrackerFrontendModule, ActivityTrackerBackendModule],
    [MessagingFrontendModule, MessagingBackendModule]
  ]) {
    const client = {};
    container.rebind(frontend).toConstantValue(client);
    assert.equal(container.get(backend), client);
  }
});
