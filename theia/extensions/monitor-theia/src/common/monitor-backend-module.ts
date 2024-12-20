import { Router } from 'express';

export interface MonitorBackendModule {
  /**
   * This method is called for each enabled module
   * Should register all endpoints on the app
   */
  registerEndpoints(router: Router): Router;
}
