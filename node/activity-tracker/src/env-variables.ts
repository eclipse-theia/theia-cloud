export namespace TheiaCloudEnv {
    export const ACTIVITY_SERVICE_HOST = 'THEIA_CLOUD_ACTIVITY_SERVICE_HOST';
    export const ACTIVITY_SERVICE_PORT = 'THEIA_CLOUD_ACTIVITY_SERVICE_PORT';
    export const ACTIVITY_SERVICE_ENABLE_TRACKER = 'THEIA_CLOUD_ACTIVITY_SERVICE_ENABLE_TRACKER';
}

export function getFromEnv(variable: string): string | undefined {
    const env = process.env;
    return env[variable];
}