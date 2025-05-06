import { BackendApplicationContribution } from '@theia/core/lib/node/backend-application';
import { inject, injectable } from '@theia/core/shared/inversify';
import { Application, Router } from 'express';

import { TheiaCloudBackendMonitorService } from '../common/monitor-protocol';

@injectable()
export class TheiaCloudMonitorRESTServiceContribution implements BackendApplicationContribution {
  constructor(
    @inject(TheiaCloudBackendMonitorService)
    protected service: TheiaCloudBackendMonitorService
  ) {}

  async configure(app: Application): Promise<void> {
    const router = Router();
    for (const module of await this.service.getEnabledBackendModules()) {
      module.registerEndpoints(router);
    }
    app.use('/monitor', router);
  }
}
