import { getFromEnv, THEIACLOUD_SESSION_SECRET } from '../env-variables';

export function isAuthorized(req: any): boolean {
  const sessionSecret = getFromEnv(THEIACLOUD_SESSION_SECRET);
  if (sessionSecret && req.body && req.body.secret && sessionSecret === req.body.secret) {
    return true;
  }
  return false;
}
