/*
 * geordi
 *
 * Copyright (C) 2019 Richard "Shred" Körber
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
package org.shredzone.geordi.service;

import org.shredzone.geordi.data.Sample;

/**
 * This service remembers the last sample value stored in database. It helps that sensor
 * values are not stored in database if the sensor is in compact mode and the value has
 * not been changed since the last time it was stored in database.
 */
public interface CompactingService {

    /**
     * Checks if the given {@link Sample} can be compacted.
     *
     * @param sample
     *         {@link Sample} to check
     * @return {@code true} if the corresponding sensor is in compact mode, and the
     * sample's value is equal to the previously stored sensor value.
     */
    boolean wasUnchanged(Sample sample);

    /**
     * Regenerates the last unchanged {@link Sample} before the value has changed. This
     * way, interpolations can start from the timestamp of the last unchanged value,
     * instead of the first unchanged value.
     *
     * @param sample
     *         {@link Sample} to regenerate the last unchanged sample for
     * @return Regenerated {@link Sample}, or {@code null} if there was no last sample
     * that could be generated
     */
    Sample lastUnchanged(Sample sample);

    /**
     * Remember a {@link Sample} value.
     *
     * @param sample
     *         {@link Sample} to remember
     */
    void rememberSample(Sample sample);

}
