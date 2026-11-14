import '../../src/browser/monitor-dialog.css';

import { AbstractDialog, codiconArray } from '@theia/core/lib/browser';
import { FrontendApplicationConfigProvider } from '@theia/core/lib/browser/frontend-application-config-provider';

export class MonitorDialog extends AbstractDialog<boolean> {
  constructor(severity: 'info' | 'warning' | 'error', message: string, detail: string, acceptLabel?: string) {
    super({ title: FrontendApplicationConfigProvider.get().applicationName });
    const body = this.contentNode.appendChild(document.createElement('div'));
    body.classList.add('theia-cloud-monitor-dialog');

    const icon = body.appendChild(document.createElement('i'));
    icon.classList.add(...codiconArray(severity));

    const text = body.appendChild(document.createElement('div'));
    text.textContent = message;
    if (detail) {
      text.appendChild(document.createElement('p')).textContent = detail;
    }
    this.appendAcceptButton(acceptLabel);
  }

  get value(): boolean {
    return true;
  }
}
