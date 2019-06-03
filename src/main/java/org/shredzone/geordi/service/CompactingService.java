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

import org.shredzone.geordi.data.Sample;

/**
 * This service remembers the last sample value stored in database. It helps that sensor
 * values are not stored in database if the sensor is in compact mode and the value has
 * not been changed since the last time it was stored in database.
 */
public interface CompactingService {

    /**
     * Remember a {@link Sample} value.
     *
     * @param sample
     *         {@link Sample} to remember
     */
    void rememberSample(Sample sample);

    /**
     * Checks if the given {@link Sample} can be compacted.
     *
     * @param sample
     *         {@link Sample} to check
     * @return {@code true} if the corresponding sensor is in compact mode, and the
     * sample's value is equal to the previously stored sensor value.
     */
    boolean wasUnchanged(Sample sample);

}
