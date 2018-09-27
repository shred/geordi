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
package org.shredzone.geordi.util;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

import com.google.inject.Injector;

/**
 * A Quartz {@link JobFactory} that uses Guice for dependency injection.
 */
@Singleton
public class GuiceJobFactory implements JobFactory {

    private final Injector guice;

    @Inject
    public GuiceJobFactory(Injector guice) {
        this.guice = guice;
    }

    @Override
    public Job newJob(TriggerFiredBundle triggerFiredBundle, Scheduler scheduler)
            throws SchedulerException {
        try {
            return guice.getInstance(triggerFiredBundle.getJobDetail().getJobClass());
        } catch (Exception ex) {
            throw new UnsupportedOperationException(ex);
        }
    }

}
