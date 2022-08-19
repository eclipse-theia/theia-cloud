/**********************************************************************
 * Copyright (c) 2018-2021 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ***********************************************************************/
import { ConnectionHandler, JsonRpcConnectionHandler } from '@theia/core';
import { ContainerModule } from '@theia/core/shared/inversify';

import { ACTIVITY_TRACKER_SERVICE_PATH, TheiaCloudActivityTrackerService } from '../common/activity-tracker-protocol';
import { DefaultTheiaCloudActivityTrackerService } from './activity-tracker-service';

export default new ContainerModule(bind => {
  bind(TheiaCloudActivityTrackerService).to(DefaultTheiaCloudActivityTrackerService).inSingletonScope();

  bind(ConnectionHandler)
    .toDynamicValue(context => new JsonRpcConnectionHandler(ACTIVITY_TRACKER_SERVICE_PATH, () => context.container.get(TheiaCloudActivityTrackerService)))
    .inSingletonScope();
});
