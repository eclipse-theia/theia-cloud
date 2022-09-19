/********************************************************************************
 * Copyright (C) 2022 EclipseSource, Lockular, Ericsson, STMicroelectronics and 
 * others.
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

import java.util.AbstractMap;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public final class JavaUtil {

    private JavaUtil() {
    }

    public static <T, V> Entry<T, V> tuple(T a, V b) {
	return new AbstractMap.SimpleEntry<T, V>(a, b);
    }

    public static <T> UnaryOperator<T> toUnary(Consumer<T> consumer) {
	return item -> {
	    consumer.accept(item);
	    return item;
	};
    }
}
