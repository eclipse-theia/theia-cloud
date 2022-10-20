/**
 * List of environment variables that the extension uses
 */
export namespace TheiaCloudEnv {
    export const ACTIVITY_SERVICE_HOST = 'THEIA_CLOUD_ACTIVITY_SERVICE_HOST';
    export const ACTIVITY_SERVICE_PORT = 'THEIA_CLOUD_ACTIVITY_SERVICE_PORT';
    export const ACTIVITY_SERVICE_ENABLE_TRACKER = 'THEIA_CLOUD_ACTIVITY_SERVICE_ENABLE_TRACKER';
}

/**
 * Utility function to check the value of an environment variable
 * @returns the value string or undefined if variable is not set
 */
export function getFromEnv(variable: string): string | undefined {
    const env = process.env;
    return env[variable];
}