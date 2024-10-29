import { getFromEnv, THEIACLOUD_SESSION_SECRET } from '../env-variables';

export function isAuthorized(req: any): boolean {
  const bearerHeader = req.headers['authorization'];
  if (bearerHeader) {
    const splitBearer = bearerHeader.split(' ');
    if (splitBearer.length === 2) {
      const bearerToken = splitBearer[1];
      const sessionSecret = getFromEnv(THEIACLOUD_SESSION_SECRET);
      if (bearerToken === sessionSecret) {
        return true;
      } else {
        console.debug('unauthorized');
      }
    }
  }
  return false;
}
