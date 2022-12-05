import { Express, json } from 'express';

import { MonitorModule } from '../monitor-module';
import { createFullscreenMessage, createMessage } from '../util/messaging-util';

export const POST_MESSAGE = '/message';
export const POST_TIMEOUT = '/message';

/**
 * This module allows to send messages to the application.
 */
export class MessagingModule implements MonitorModule {
  registerEndpoints(app: Express): Express {
    app.use(json());
    app.post(POST_MESSAGE, (req, res) => {
      const body = req.body;
      if (body.level && body.message) {
        if (body.fullscreen) {
          createFullscreenMessage(body.level, body.message, body.detail);
        } else {
          createMessage(body.level, body.message);
        }
      }
      res.end('success');
      res.status(200);
    });
    return app;
  }
}
