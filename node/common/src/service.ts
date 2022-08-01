export interface ServiceRequest {
    serviceUrl: string;
    appId: string;
    kind?: string;
}

export interface ServiceResponse {
    kind: string;
    success: boolean;
    error?: string;
}
