/**********************************************************************
 * Copyright (c) 2018-2021 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ***********************************************************************/
export const ACTIVITY_TRACKER_SERVICE_PATH = '/services/theiacloud-activity-tracker';

export const TheiaCloudActivityTrackerService = Symbol('TheiaCloudActivityTrackerService');

export interface TheiaCloudActivityTrackerService {
  /**
   * Rests the current session inactivity timeout.
   */
  reportActivity(): void;

  /**
   * Returns true if we are running in the Theia.cloud environment.
   */
  isRunningOnTheiaCloud(): boolean;
}
