const assert = require('node:assert/strict');
const { it } = require('node:test');

require('@theia/core/shared/reflect-metadata');
const { Container } = require('@theia/core/shared/inversify');
const backendModule = require('../lib/node/monitor-backend-module').default;
const { DefaultTheiaCloudBackendMonitorService } = require('../lib/node/monitor-service');
const { TheiaCloudBackendMonitorService } = require('../lib/common/monitor-protocol');
const { ActivityTrackerBackendModule } = require('../lib/common/modules/activity-tracker-module');
const { MessagingBackendModule } = require('../lib/common/modules/messaging-module');
const { MONITOR_ENABLE_ACTIVITY_TRACKER } = require('../lib/common/env-variables');

it('exposes the default monitor service through its replaceable class binding', () => {
  const container = new Container();
  container.load(backendModule);
  assert.equal(container.get(TheiaCloudBackendMonitorService), container.get(DefaultTheiaCloudBackendMonitorService));

  const custom = { getEnabledBackendModules: async () => [] };
  container.rebind(DefaultTheiaCloudBackendMonitorService).toConstantValue(custom);
  assert.equal(container.get(TheiaCloudBackendMonitorService), custom);
});

it('returns the injected backend modules according to the activity tracker flag', async t => {
  const previous = process.env[MONITOR_ENABLE_ACTIVITY_TRACKER];
  t.after(() => {
    if (previous === undefined) {
      delete process.env[MONITOR_ENABLE_ACTIVITY_TRACKER];
    } else {
      process.env[MONITOR_ENABLE_ACTIVITY_TRACKER] = previous;
    }
  });
  const container = new Container();
  container.load(backendModule);
  const tracker = { registerEndpoints() {} };
  container.rebind(ActivityTrackerBackendModule).toConstantValue(tracker);
  const messaging = container.get(MessagingBackendModule);
  const service = container.get(TheiaCloudBackendMonitorService);

  delete process.env[MONITOR_ENABLE_ACTIVITY_TRACKER];
  for (const flag of [undefined, 'false', 'true']) {
    if (flag !== undefined) {
      process.env[MONITOR_ENABLE_ACTIVITY_TRACKER] = flag;
    }
    const modules = await service.getEnabledBackendModules();
    assert.equal(modules.length, flag === 'true' ? 2 : 1);
    assert.equal(modules[0], messaging);
    if (flag === 'true') {
      assert.equal(modules[1], tracker);
    }
  }
});
