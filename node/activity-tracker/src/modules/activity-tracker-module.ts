import { Express } from 'express';
import * as vscode from 'vscode';
import { TrackerModule } from '../tracker-module';

export const GET_ACTIVITY_PATH = '/activity';
export const POST_POPUP = '/popup';
export const COMMAND_ACTIVITY_REPORT_TITLE = 'activity.report';

export class ActivityTrackerModule implements TrackerModule{
    protected lastActivity: Date;

    constructor() {
        this.lastActivity = new Date();
        this.registerCommand();
        this.setupListeners();
    }

    registerEndpoints(app: Express): Express {
        app.get(GET_ACTIVITY_PATH, async (req, res) => {
            res.end(this.getLastActivity());
        });
    
        app.post(POST_POPUP, (req, res) => {
            this.createPopup();
            res.status(200);
            res.end('success');
        });    
        return app;
    }

    protected reportActivity(reason?: string): void {
        console.debug(`Activity reported: ${new Date().toISOString()} (${reason ?? 'unknown'})`);
        this.lastActivity = new Date();
    }

    protected getLastActivity(): string {
        return this.lastActivity.toISOString();
    }

    protected registerCommand(): void {
        vscode.commands.registerCommand(COMMAND_ACTIVITY_REPORT_TITLE, (reason) => this.reportActivity(`${reason ?? 'activity'} (via command)`));
    }

    protected createPopup(): void {
        const options: vscode.MessageOptions = {
            detail: 'Pod will be shutdown after some inactivity',
            modal: true
        };
    
        const yesOption: vscode.MessageItem = { title: 'Yes', isCloseAffordance: true };
    
        const message = vscode.window.showInformationMessage('Are you still here?', options, yesOption);
        
        message.then((answer) => {
            if (answer === yesOption) {
                this.reportActivity('Popup dialog was confirmed');
            }
        });
    }

    protected setupListeners(): void {
        vscode.authentication.onDidChangeSessions(() => this.reportActivity('sessionChange'));
        vscode.debug.onDidChangeActiveDebugSession(() => this.reportActivity('changeDebugSession'));
        vscode.debug.onDidChangeBreakpoints(() => this.reportActivity('changeBreakpoints'));
        vscode.debug.onDidReceiveDebugSessionCustomEvent(() => this.reportActivity('customEvent'));
        vscode.debug.onDidStartDebugSession(() => this.reportActivity('startDebugSession'));
        vscode.debug.onDidTerminateDebugSession(() => this.reportActivity('terminateDebugSession'));
        vscode.env.onDidChangeTelemetryEnabled(() => this.reportActivity('changeTelemetryEnabled'));
        vscode.extensions.onDidChange(() => this.reportActivity(`extensionsChanged`));
        vscode.languages.onDidChangeDiagnostics(() => this.reportActivity(`changeDiagnostics`));
        vscode.tasks.onDidEndTask(() => this.reportActivity(`taskEnd`));
        vscode.tasks.onDidEndTaskProcess(() => this.reportActivity(`taskEndProcess`));
        vscode.tasks.onDidStartTask(() => this.reportActivity(`startTask`));
        vscode.tasks.onDidStartTaskProcess(() => this.reportActivity(`startTaskProcess`));
        vscode.window.onDidChangeActiveColorTheme(() => this.reportActivity(`changeColorTheme`));
        vscode.window.onDidChangeActiveNotebookEditor(() => this.reportActivity(`changeNotebookEditor`));
        vscode.window.onDidChangeActiveTerminal(() => this.reportActivity(`changeTerminal`));
        vscode.window.onDidChangeActiveTextEditor(() => this.reportActivity(`changeTextEditor`));
        vscode.window.onDidChangeNotebookEditorSelection(() => this.reportActivity(`changeNotebookEditorSelection`));
        vscode.window.onDidChangeNotebookEditorVisibleRanges(() => this.reportActivity(`changeNotebookEditorVisibleRanges`));
        vscode.window.onDidChangeTerminalState(() => this.reportActivity(`changeTerminalState`));
        vscode.window.onDidChangeTextEditorOptions(() => this.reportActivity(`changeTextEditorOptions`));
        vscode.window.onDidChangeTextEditorSelection(() => this.reportActivity(`changeTextEditorSelection`));
        vscode.window.onDidChangeTextEditorViewColumn(() => this.reportActivity(`changeTextEditorViewColumn`));
        vscode.window.onDidChangeTextEditorVisibleRanges(() => this.reportActivity(`changeTextEditorVisibleRanges`));
        vscode.window.onDidChangeVisibleNotebookEditors(() => this.reportActivity(`changeVisibleNotebookEditors`));
        vscode.window.onDidChangeVisibleTextEditors(() => this.reportActivity(`changeVisibleTextEditors`));
        vscode.window.onDidChangeWindowState(() => this.reportActivity(`changeWindowState`));
        vscode.workspace.onDidChangeConfiguration(() => this.reportActivity(`changeConfiguration`));
        vscode.workspace.onDidChangeNotebookDocument(() => this.reportActivity(`changeNotebookDocument`));
        vscode.workspace.onDidChangeTextDocument(() => this.reportActivity(`changeTextDocument`));
        vscode.workspace.onDidChangeWorkspaceFolders(() => this.reportActivity(`changeWorkspaceFolders`));
        vscode.workspace.onDidCloseNotebookDocument(() => this.reportActivity(`closeNotebookDocument`));
        vscode.workspace.onDidCloseTextDocument(() => this.reportActivity(`closeTextDocument`));
        vscode.workspace.onDidCreateFiles(() => this.reportActivity(`createFiles`));
        vscode.workspace.onDidDeleteFiles(() => this.reportActivity(`deleteFiles`));
        vscode.workspace.onDidGrantWorkspaceTrust(() => this.reportActivity(`grantWorkspaceTrust`));
        vscode.workspace.onDidOpenNotebookDocument(() => this.reportActivity(`openNotebookDocument`));
        vscode.workspace.onDidOpenTextDocument(() => this.reportActivity(`openTextDocument`));
        vscode.workspace.onDidRenameFiles(() => this.reportActivity(`renameFiles`));
        vscode.workspace.onDidSaveNotebookDocument(() => this.reportActivity(`saveNotebookDocument`));
        vscode.workspace.onDidSaveTextDocument(() => this.reportActivity(`saveTextDocument`));
        vscode.workspace.onWillCreateFiles(() => this.reportActivity(`willCreateFiles`));
        vscode.workspace.onWillDeleteFiles(() => this.reportActivity(`willDeleteFiles`));
        vscode.workspace.onWillRenameFiles(() => this.reportActivity(`willRenameFiles`));
        vscode.workspace.onWillSaveTextDocument(() => this.reportActivity(`willSaveTextDocument`));
    }
 }