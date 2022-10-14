import * as express from 'express';

export interface TrackerModule {
    registerEndpoints(app: express.Express): express.Express;
}