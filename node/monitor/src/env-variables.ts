/**
 * List of environment variables that the extension uses
 */
export const MONITOR_PORT = 'THEIA_CLOUD_MONITOR_PORT';
export const MONITOR_ENABLE_ACTIVITY_TRACKER = 'THEIA_CLOUD_MONITOR_ENABLE_ACTIVITY_TRACKER';
export const THEIA_CLOUD_SESSION_SECRET = 'THEIA_CLOUD_SESSION_SECRET';

/**
 * Utility function to check the value of an environment variable
 * @returns the value string or undefined if variable is not set
 */
export function getFromEnv(variable: string): string | undefined {
  const env = process.env;
  return env[variable];
}
