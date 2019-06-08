/*
 * geordi
 *
 * Copyright (C) 2019 Richard "Shred" Körber
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
package org.shredzone.geordi.service;

import static java.time.Instant.now;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

import org.shredzone.geordi.data.Sample;
import org.shredzone.geordi.sensor.Sensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of CompactingService. It stores the last sample value in a hash map.
 * This implementation is threadsafe.
 */
@Singleton
public class CompactingServiceImpl implements CompactingService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<Integer, BigDecimal> lastValue = new HashMap<>();
    private final Map<Integer, Instant> firstUnchanged = new HashMap<>();
    private final Map<Integer, Instant> lastUnchanged = new HashMap<>();

    @Override
    public boolean wasUnchanged(Sample sample) {
        if (!isCompacting(sample.getSensor())) {
            return false;
        }

        int id = sample.getSensor().getId();

        BigDecimal last = lastValue.get(id);
        if (last == null) {
            return false;
        }

        boolean unchanged = last.compareTo(sample.getValue()) == 0;

        Instant firstTs = firstUnchanged.get(id);
        if (unchanged && firstTs != null && isStorageRequired(sample.getSensor(), firstTs)) {
            firstUnchanged.put(id, sample.getTimestamp());
            lastUnchanged.remove(id); // do not generate a "last unchanged" sample
            return false;
        }

        if (unchanged) {
            firstUnchanged.putIfAbsent(id, sample.getTimestamp());
            lastUnchanged.put(id, sample.getTimestamp());
        }

        return unchanged;
    }

    @Override
    public Sample lastUnchanged(Sample sample) {
        if (!isCompacting(sample.getSensor())) {
            return null;
        }

        int id = sample.getSensor().getId();
        Instant ts = lastUnchanged.get(id);
        BigDecimal value = lastValue.get(id);

        if (ts == null || value == null) {
            return null;
        }

        return new Sample(sample.getSensor(), ts, value);
    }

    @Override
    public void rememberSample(Sample sample) {
        if (isCompacting(sample.getSensor())) {
            int id = sample.getSensor().getId();
            lastValue.put(id, sample.getValue());
            lastUnchanged.remove(id);
        }
    }

    /**
     * Checks if the sensor is in compacting mode.
     *
     * @param sensor
     *         {@link Sensor} to check
     * @return {@code true} if in compacting mode
     */
    private boolean isCompacting(Sensor sensor) {
        return sensor.getConfig().optBoolean("Compacting", false);
    }

    /**
     * Checks if the sensor has a compacting interval set.
     *
     * @param sensor
     *         {@link Sensor} to check
     * @return Compacting interval {@link Duration}, guaranteed to be negative. If there
     * is no such interval, {@code null} is returned instead.
     */
    private Duration getStorageInterval(Sensor sensor) {
        String span = sensor.getConfig().optString("CompactingMaxInterval", null);
        if (span == null) {
            return null;
        }

        try {
            Duration result = Duration.parse(span);
            if (!result.isNegative()) {
                result = result.negated();
            }
            return result;
        } catch (DateTimeParseException ex) {
            log.warn("Sensor #" + sensor.getId() + " has invalid compactingInterval '"
                    + span + "'", ex);
            return null;
        }
    }

    /**
     * Checks if for a compacting sensor, the unchanged sensor value is old enough to be
     * stored anyway.
     *
     * @param sensor
     *         {@link Sensor} to check
     * @param lastStored
     *         {@link Instant} of the oldest unchanged sensor value
     * @return {@code true} if the unchanged sensor value is to be stored, {@code false}
     * if not
     */
    private boolean isStorageRequired(Sensor sensor, Instant lastStored) {
        Duration interval = getStorageInterval(sensor);

        if (interval == null) {
            return false;
        }

        return lastStored.isBefore(now().plus(interval).plus(2, ChronoUnit.SECONDS));
    }

}
