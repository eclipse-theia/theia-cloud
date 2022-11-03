/********************************************************************************
 * Copyright (c) 2020-2021 EclipseSource and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License v. 2.0 are satisfied: GNU General Public License, version 2
 * with the GNU Classpath Exception which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 ********************************************************************************/
package org.eclipse.theia.cloud.monitor.di;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

public final class MultiBinding<T> {
    public static <T> MultiBinding<T> create(final Class<T> type) {
	return new MultiBinding<>(TypeLiteral.get(type));
    }

    public static <T> MultiBinding<T> create(final TypeLiteral<T> type) {
	return new MultiBinding<>(type);
    }

    private final Set<Class<? extends T>> bindings;

    private final TypeLiteral<T> type;

    private String annotationName;

    private MultiBinding(final TypeLiteral<T> type) {
	this.type = type;
	bindings = new LinkedHashSet<>();
    }

    public MultiBinding<T> setAnnotationName(final String annotationName) {
	this.annotationName = annotationName;
	return this;
    }

    public String getAnnotationName() {
	return annotationName;
    }

    /**
     * Applies the stored bindings to the given binder in form of a set binding.
     *
     * @param binder binder
     */
    public void applyBinding(final Binder binder) {
	Multibinder<T> multiBinder = this.annotationName == null ? Multibinder.newSetBinder(binder, getType())
		: Multibinder.newSetBinder(binder, getType(), Names.named(annotationName));
	bindings.forEach(b -> multiBinder.addBinding().to(b));
    }

    public boolean add(final Class<? extends T> newBinding) {
	return bindings.add(newBinding);
    }

    public boolean addAll(final Collection<Class<? extends T>> newBindings) {
	return bindings.addAll(newBindings);
    }

    public boolean remove(final Class<? extends T> toRemove) {
	return bindings.remove(toRemove);
    }

    public boolean removeAll(final Collection<Class<? extends T>> toRemove) {
	return bindings.removeAll(toRemove);
    }

    public boolean rebind(final Class<? extends T> oldBinding, final Class<? extends T> newBinding) {
	if (remove(oldBinding)) {
	    add(newBinding);
	    return true;
	}
	return false;
    }

    public Set<Class<? extends T>> getAll() {
	return this.bindings;
    }

    public boolean contains(final Class<? extends T> binding) {
	return bindings.contains(binding);
    }

    TypeLiteral<T> getType() {
	return type;
    }

}
