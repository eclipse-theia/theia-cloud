import * as express from 'express';

export interface MonitorModule {
    /**
     * This method is called for each enabled module
     * Should register all endpoints on the app
     * @param app the current Express object
     * @returns the same Express object with endpoints registered
     */
    registerEndpoints(app: express.Express): express.Express;
}