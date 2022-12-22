import { json, Router } from 'express';

import { MonitorModule } from '../monitor-module';
import { createFullscreenMessage, createMessage } from '../util/messaging-util';
import { isAuthorized } from '../util/util';

export const MESSAGING_PATH = '/message';

/**
 * This module allows to send messages to the application.
 */
export class MessagingModule implements MonitorModule {
  registerEndpoints(router: Router): Router {
    const messagingRouter = Router();
    messagingRouter.use(json());
    messagingRouter.post('', (req, res) => {
      if (isAuthorized(req)) {
        const body = req.body;
        if (body.level && body.message) {
          if (body.fullscreen) {
            createFullscreenMessage(body.level, body.message, body.detail);
          } else {
            createMessage(body.level, body.message);
          }
        }
        res.status(200);
        res.end('success');
      } else {
        res.status(401);
        res.end('Unauthorized');
      }
    });
    router.use(MESSAGING_PATH, messagingRouter);
    return router;
  }
}
