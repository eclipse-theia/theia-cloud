import { createHmac, randomBytes, timingSafeEqual } from 'node:crypto';

import { THEIACLOUD_SESSION_SECRET } from './env-variables';

/**
 * Per-process key used only to length-normalise the two values before they are
 * compared. Hashing both sides keeps the compared buffers the same size, so
 * timingSafeEqual never throws on a length mismatch and the secret's length is
 * not observable from response timing either.
 */
const COMPARE_KEY = randomBytes(32);

function secretEquals(actual: string, expected: string): boolean {
  const left = createHmac('sha256', COMPARE_KEY).update(actual).digest();
  const right = createHmac('sha256', COMPARE_KEY).update(expected).digest();
  return timingSafeEqual(left, right);
}

export function isAuthorized(req: any): boolean {
  const bearerHeader = req.headers['authorization'];
  if (bearerHeader) {
    const splitBearer = bearerHeader.split(' ');
    if (splitBearer.length === 2) {
      const bearerToken = splitBearer[1];
      const sessionSecret = process.env[THEIACLOUD_SESSION_SECRET];
      if (sessionSecret !== undefined && secretEquals(bearerToken, sessionSecret)) {
        return true;
      }
    }
  }
  return false;
}
