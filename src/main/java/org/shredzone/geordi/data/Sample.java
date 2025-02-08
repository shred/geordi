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
package org.shredzone.geordi.data;

import java.math.BigDecimal;
import java.time.Instant;

import org.shredzone.geordi.sensor.Sensor;

/**
 * A {@link Sample} contains the current reading of a {@link Sensor}.
 * <p>
 * Objects are immutable.
 */
public class Sample {

    private final Sensor sensor;
    private final Instant timestamp;
    private final BigDecimal value;

    /**
     * Creates a new {@link Sample}.
     *
     * @param sensor
     *            {@link Sensor} that was read
     * @param timestamp
     *            The instant the sensor was read
     * @param value
     *            The value of the sensor at that instant
     */
    public Sample(Sensor sensor, Instant timestamp, BigDecimal value) {
        this.sensor = sensor;
        this.timestamp = timestamp;
        this.value = value;
    }

    /**
     * Returns the {@link Sensor} that was read.
     */
    public Sensor getSensor() {
        return sensor;
    }

    /**
     * Returns the {@link Instant} the sensor was read.
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the sensor value at that instant.
     */
    public BigDecimal getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("Sensor %d (%s): %.2f %s @ %s",
                sensor.getId(),
                sensor.getName(),
                value,
                sensor.getUnit(),
                timestamp.toString()
        );
    }

}
