/**********************************************************************
 * Copyright (c) 2018-2021 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ***********************************************************************/
import { injectable } from '@theia/core/shared/inversify';
import { reportSessionActivity, SessionActivityRequest } from '@eclipse-theiacloud/common/build';

import { TheiaCloudActivityTrackerService } from '../common/activity-tracker-protocol';

@injectable()
export class DefaultTheiaCloudActivityTrackerService implements TheiaCloudActivityTrackerService {
  // Time before sending next request. If a few requests from frontend(s) are received during this period,
  // only one request to workspace master will be sent.
  protected static REQUEST_PERIOD_MS = 1 * 60 * 1000;
  // Time before resending request to workspace master if a network error occurs.
  protected static RETRY_REQUEST_PERIOD_MS = 5 * 1000;
  // Number of retries before give up if a network error occurs.
  protected static RETRY_COUNT = 5;

  // Indicates state of the timer. If true timer is running.
  protected isTimerRunning: boolean;
  // Flag which is used to check if new requests were received during timer awaiting.
  protected isNewRequest: boolean;

  protected requestData?: SessionActivityRequest;

  constructor() {
    this.isTimerRunning = false;
    this.isNewRequest = false;
    this.requestData = this.getRequestData();
  }

  isRunningOnTheiaCloud(): boolean {
    return !!this.requestData || false;
  }

  /**
   * Invoked each time when a client sends an activity request.
   */
  reportActivity(): void {
    if (!this.isRunningOnTheiaCloud()) {
      return;
    }
    if (this.isTimerRunning) {
      this.isNewRequest = true;
      return;
    }

    this.sendRequestAndSetTimer();
  }

  protected getRequestData(): SessionActivityRequest | undefined {
    const appId = process.env.THEIA_CLOUD_APP_ID;
    const serviceUrl = process.env.THEIA_CLOUD_SERVICE_URL;
    const sessionName = process.env.THEIA_CLOUD_SESSION_NAME;
    return appId && serviceUrl && sessionName ? { appId, serviceUrl, sessionName } : undefined;
  }

  protected sendRequestAndSetTimer(): void {
    this.sendRequest();
    this.isNewRequest = false;

    setTimeout(() => this.checkNewRequestsTimerCallback(), DefaultTheiaCloudActivityTrackerService.REQUEST_PERIOD_MS);
    this.isTimerRunning = true;
  }

  protected checkNewRequestsTimerCallback(): void {
    this.isTimerRunning = false;

    if (this.isNewRequest) {
      this.sendRequestAndSetTimer();
    }
  }

  protected sendRequest(attemptsLeft: number = DefaultTheiaCloudActivityTrackerService.RETRY_COUNT): void {
    try {
      this.updateSessionActivity();
    } catch (error) {
      if (attemptsLeft > 0) {
        setTimeout(() => this.sendRequest(), DefaultTheiaCloudActivityTrackerService.RETRY_REQUEST_PERIOD_MS, --attemptsLeft);
      } else {
        console.error('Activity tracker: Failed to report session activity: ', (error as Error).message);
      }
    }
  }

  protected async updateSessionActivity(): Promise<void> {
    const data = this.getRequestData();
    if(data) {
      reportSessionActivity(data);
    }
  }
}
