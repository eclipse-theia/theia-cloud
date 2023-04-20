import { injectable } from '@theia/core/shared/inversify';
import { json, Router } from 'express';

import { MessageLevel, MessagingBackendModule, MessagingFrontendModule } from '../../common/modules/messaging-module';
import { isAuthorized } from '../../common/util';

export const MESSAGING_PATH = '/message';

@injectable()
export class MessagingBackendModuleImpl implements MessagingBackendModule {
  static clients: Array<MessagingFrontendModule> = [];

  registerEndpoints(router: Router): Router {
    const messagingRouter = Router();
    messagingRouter.use(json());
    messagingRouter.post('', (req, res) => {
      if (isAuthorized(req)) {
        const body = req.body;
        if (body.level && body.message) {
          this.displayMessage(body.level, body.message, body.detail, body.fullscreen);
        } else {
          res.end("Body missing 'level' or 'message' fields");
          res.status(400);
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

  displayMessage(level: MessageLevel, message: string, detail: string, fullscreen: boolean): void {
    MessagingBackendModuleImpl.clients.forEach(c => c.displayMessage(level, message, detail, fullscreen));
  }

  setClient(client: MessagingFrontendModule | undefined): void {
    if (client) {
      MessagingBackendModuleImpl.clients.push(client);
    }
  }

  disconnectClient(client: MessagingFrontendModule): void {
    const index = MessagingBackendModuleImpl.clients.indexOf(client);
    if (index !== -1) {
      MessagingBackendModuleImpl.clients.splice(index, 1);
    }
  }

  dispose(): void {
    MessagingBackendModuleImpl.clients.forEach(this.disconnectClient.bind(this));
  }
}
