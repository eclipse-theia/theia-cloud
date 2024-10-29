/**
 * List of environment variables that the extension uses
 */
export const THEIACLOUD_MONITOR_PORT = 'THEIACLOUD_MONITOR_PORT';
export const THEIACLOUD_MONITOR_ENABLE_ACTIVITY_TRACKER = 'THEIACLOUD_MONITOR_ENABLE_ACTIVITY_TRACKER';
export const THEIACLOUD_SESSION_SECRET = 'THEIACLOUD_SESSION_SECRET';

/**
 * Utility function to check the value of an environment variable
 * @returns the value string or undefined if variable is not set
 */
export function getFromEnv(variable: string): string | undefined {
  const env = process.env;
  return env[variable];
}
