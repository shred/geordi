/*
 * geordi
 *
 * Copyright (C) 2018 Richard "Shred" Körber
 *   https://codeberg.org/shred/geordi
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

import static java.util.stream.Collectors.toList;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.shredzone.geordi.data.Sample;
import org.shredzone.geordi.device.Device;
import org.shredzone.geordi.service.CompactingService;
import org.shredzone.geordi.service.DatabaseService;
import org.shredzone.geordi.util.GuiceJobFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Geordi's main runner.
 */
@Singleton
public class GeordiRunner {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String ID_KEY = "id";

    @Inject
    private DatabaseService databaseService;

    @Inject
    private Scheduler scheduler;

    @Inject
    private GuiceJobFactory guiceJobFactory;

    /**
     * Starts Geordi.
     * <p>
     * The Quartz scheduler is started, and the cron expressions of all {@link Device} in
     * the database are added, so each device is triggered on the desired frequency.
     */
    public void start() {
        try {
            scheduler.start();
            scheduler.setJobFactory(guiceJobFactory);

            for (Device dev : databaseService.fetchDevices()) {
                log.info("Registered device: {}", dev.getName());

                JobDetail job = JobBuilder.newJob(DeviceJob.class)
                        .withIdentity(dev.getName())
                        .usingJobData(ID_KEY, dev.getId())
                        .build();

                Trigger trigger = TriggerBuilder.newTrigger()
                        .withIdentity(dev.getName())
                        .startNow()
                        .withSchedule(CronScheduleBuilder.cronSchedule(dev.getCron()))
                        .build();

                scheduler.scheduleJob(job, trigger);
            }
        } catch (SchedulerException ex) {
            throw new IllegalStateException(ex);
        }

        log.info("Geordi is in the engine room!");
    }

    /**
     * A Quartz {@link Job} that fetches a {@link Device} from database, reads all the
     * sensor values and stores them into the database.
     */
    private static class DeviceJob implements Job {
        private final Logger log = LoggerFactory.getLogger(getClass());

        @Inject
        private DatabaseService databaseService;

        @Inject
        private CompactingService compactingService;

        @Override
        public void execute(JobExecutionContext context) {
            int devId = context.getJobDetail().getJobDataMap().getIntValue(ID_KEY);
            try {
                Device device = databaseService.getDevice(devId);

                List<Sample> samples = new LinkedList<>(device.readSensors());
                samples.removeIf(compactingService::wasUnchanged);

                List<Sample> preSamples = samples.stream()
                        .map(compactingService::lastUnchanged)
                        .filter(Objects::nonNull)
                        .collect(toList());

                databaseService.storeSamples(preSamples);
                databaseService.storeSamples(samples);

                samples.forEach(compactingService::rememberSample);
            } catch (Exception ex) {
                log.error("Failed to poll device {}", devId, ex);
            }
        }
    }

}
