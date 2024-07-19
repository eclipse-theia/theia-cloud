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
package org.eclipse.theia.cloud.service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class ApplicationLifecycleListener {
    protected Logger logger;

    protected void onStart(@Observes StartupEvent event) {
        logger = Logger.getLogger(getClass());
        logConfiguration();
    }

    private void logConfiguration() {
        logConfigurationSources();
        logQuarkusConfiguration();
    }

    private void logConfigurationSources() {
        for (ConfigSource configSource : ConfigProvider.getConfig().getConfigSources()) {
            logger.info(configSource.getName() + " (" + configSource.getOrdinal() + ")");
            logger.info(new JsonObject(new HashMap<String, Object>(configSource.getProperties())).encodePrettily());
        }
    }

    private void logQuarkusConfiguration() {
        Config configuration = ConfigProvider.getConfig();
        Map<String, Object> quarkusProperties = StreamSupport
                .stream(configuration.getPropertyNames().spliterator(), false).filter(this::isQuarkusProperty) //
                .collect(Collectors.toMap(Function.identity(),
                        property -> configuration.getValue(property, String.class)));
        logger.info("Resulting Quarkus Configuration");
        logger.info(new JsonObject(quarkusProperties).encodePrettily());
    }

    private boolean isQuarkusProperty(Object name) {
        return name.toString().startsWith("quarkus.");
    }

}
