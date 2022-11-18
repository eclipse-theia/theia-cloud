import * as vscode from 'vscode';

/* eslint-disable @typescript-eslint/naming-convention */
export enum MessageLevel {
    WARN = 'warn',
    ERROR = 'error',
    INFO = 'info'
}

export function createMessage(level: MessageLevel, message: string): void {
    switch(level) {
        case MessageLevel.WARN:
            vscode.window.showWarningMessage(message);
            break;
        case MessageLevel.ERROR:
            vscode.window.showErrorMessage(message);
            break;
        default:
            vscode.window.showInformationMessage(message);
            break;
    }
}

export function createFullscreenMessage(level: MessageLevel, message: string, title: string): void {
    switch(level) {
        case MessageLevel.WARN:
            vscode.window.showWarningMessage(message, { modal: true, detail: title });
            break;
        case MessageLevel.ERROR:
            vscode.window.showErrorMessage(message, { modal: true, detail: title });
            break;
        default:
            vscode.window.showInformationMessage(message, { modal: true, detail: title });
            break;
    }
}