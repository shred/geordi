/*
 * geordi
 *
 * Copyright (C) 2018 Richard "Shred" Körber
 *   https://github.com/shred/geordi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package org.shredzone.geordi;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.shredzone.geordi.device.AquaeroDevice;
import org.shredzone.geordi.device.Ccu2Device;
import org.shredzone.geordi.device.Device;
import org.shredzone.geordi.device.DustyDevice;
import org.shredzone.geordi.service.CompactingService;
import org.shredzone.geordi.service.CompactingServiceImpl;
import org.shredzone.geordi.service.DatabaseService;
import org.shredzone.geordi.service.DatabaseServiceImpl;

/**
 * Guice module definitions for Geordi.
 */
public class GeordiModule extends AbstractModule {

    private String databaseHost;
    private String databaseUser;
    private String databasePassword;

    public void setDatabaseHost(String databaseHost) {
        this.databaseHost = databaseHost;
    }

    public void setDatabaseUser(String databaseUser) {
        this.databaseUser = databaseUser;
    }

    public void setDatabasePassword(String databasePassword) {
        this.databasePassword = databasePassword;
    }

    @Override
    protected void configure() {
        bind(DatabaseService.class).to(DatabaseServiceImpl.class);
        bind(CompactingService.class).to(CompactingServiceImpl.class);

        MapBinder<String, Device> mapbinder = MapBinder.newMapBinder(binder(), String.class, Device.class);
        mapbinder.addBinding("aquaero").to(AquaeroDevice.class);
        mapbinder.addBinding("ccu2").to(Ccu2Device.class);
        mapbinder.addBinding("dusty").to(DustyDevice.class);
        // Add more device implementations here...

        try {
            bind(Scheduler.class).toInstance(StdSchedulerFactory.getDefaultScheduler());
        } catch (SchedulerException ex) {
            throw new IllegalStateException(ex);
        }

        Jdbi jdbi;
        if (databaseUser != null && databasePassword != null) {
            jdbi = Jdbi.create(databaseHost, databaseUser, databasePassword);
        } else {
            jdbi = Jdbi.create(databaseHost);
        }

        jdbi.installPlugin(new PostgresPlugin());
        bind(Jdbi.class).toInstance(jdbi);
    }

}
