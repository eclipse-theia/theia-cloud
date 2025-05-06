import { injectable } from '@theia/core/shared/inversify';
import { json, Router } from 'express';

import {
  ActivityTrackerBackendModule,
  ActivityTrackerFrontendModule
} from '../../common/modules/activity-tracker-module';
import { isAuthorized } from '../../common/util';

export const ACTIVITY_TRACKER_PATH = '/activity';
export const LAST_ACTIVITY_PATH = '/lastActivity';
export const POST_POPUP = '/popup';

@injectable()
export class ActivityTrackerBackendModuleImpl implements ActivityTrackerBackendModule {
  static clients: Array<ActivityTrackerFrontendModule> = [];
  static timeInMilliseconds: number | undefined;
  protected messageAlreadyDisplayed = false;

  registerEndpoints(router: Router): Router {
    const activityRouter = Router();
    activityRouter.use(json());
    activityRouter.get(LAST_ACTIVITY_PATH, async (req, res) => {
      if (isAuthorized(req)) {
        if (ActivityTrackerBackendModuleImpl.timeInMilliseconds) {
          res.status(200);
          res.end(ActivityTrackerBackendModuleImpl.timeInMilliseconds.toString());
        } else {
          res.status(201);
          res.end('No time was reported yet');
        }
      } else {
        res.status(401);
        res.end('Unauthorized');
      }
    });

    activityRouter.post(POST_POPUP, (req, res) => {
      if (isAuthorized(req)) {
        console.debug('POPUP REQUESTED');
        this.createPopup();
        res.status(200);
        res.end('success');
      } else {
        res.status(401);
        res.end('Unauthorized');
      }
    });
    router.use(ACTIVITY_TRACKER_PATH, activityRouter);
    return router;
  }

  reportActivity(reason?: string): void {
    this.messageAlreadyDisplayed = false;
    ActivityTrackerBackendModuleImpl.timeInMilliseconds = Date.now();
    console.debug(
      `Activity reported: ${this.formatTime(ActivityTrackerBackendModuleImpl.timeInMilliseconds)} (${
        reason ?? 'unknown'
      })`
    );
  }

  formatTime(timeInMilliseconds: number): string {
    const date = new Date(timeInMilliseconds);
    return date.toISOString();
  }

  protected getLastActivity(): number | undefined {
    return ActivityTrackerBackendModuleImpl.timeInMilliseconds;
  }

  protected createPopup(): void {
    if (!this.messageAlreadyDisplayed) {
      ActivityTrackerBackendModuleImpl.clients.forEach(c => c.displayPopup());
      this.messageAlreadyDisplayed = true;
    }
  }

  setClient(client: ActivityTrackerFrontendModule | undefined): void {
    if (client) {
      ActivityTrackerBackendModuleImpl.clients.push(client);
    }
  }

  disconnectClient(client: ActivityTrackerFrontendModule): void {
    const index = ActivityTrackerBackendModuleImpl.clients.indexOf(client);
    if (index !== -1) {
      ActivityTrackerBackendModuleImpl.clients.splice(index, 1);
    }
  }

  dispose(): void {
    ActivityTrackerBackendModuleImpl.clients.forEach(this.disconnectClient.bind(this));
  }
}
