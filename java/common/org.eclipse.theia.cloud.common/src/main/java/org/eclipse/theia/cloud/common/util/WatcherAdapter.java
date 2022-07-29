/********************************************************************************
 * Copyright (C) 2022 EclipseSource and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License v. 2.0 are satisfied: GNU General Public License, version 2
 * with the GNU Classpath Exception which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 ********************************************************************************/
package org.eclipse.theia.cloud.common.util;

import java.util.function.BiConsumer;

import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.fabric8.kubernetes.client.WatcherException;

public final class WatcherAdapter {
    private WatcherAdapter() {
    }

    public static <T> Watcher<T> eventReceived(BiConsumer<Action, T> consumer) {
	return new Watcher<T>() {
	    @Override
	    public void eventReceived(Action action, T resource) {
		consumer.accept(action, resource);
	    }

	    @Override
	    public void onClose(WatcherException cause) {
		// do nothing
	    }
	};
    }
}
