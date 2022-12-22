import * as express from 'express';

export interface MonitorModule {
  /**
   * This method is called for each enabled module
   * Should register all endpoints on the app
   */
  registerEndpoints(router: express.Router): express.Router;
}
