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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

import org.shredzone.geordi.data.Sample;

/**
 * Implementation of CompactingService. It stores the last sample value in a hash map.
 * This implementation is threadsafe.
 */
@Singleton
public class CompactingServiceImpl implements CompactingService {

    private final Map<Integer, BigDecimal> lastValue = new HashMap<>();
    private final Map<Integer, Instant> lastUnchanged = new HashMap<>();

    @Override
    public boolean wasUnchanged(Sample sample) {
        if (!sample.getSensor().isCompact()) {
            return false;
        }

        int id = sample.getSensor().getId();

        BigDecimal last = lastValue.get(id);
        if (last == null) {
            return false;
        }

        boolean unchanged = last.compareTo(sample.getValue()) == 0;

        if (unchanged) {
            lastUnchanged.put(id, sample.getTimestamp());
        }

        return unchanged;
    }

    @Override
    public Sample lastUnchanged(Sample sample) {
        if (!sample.getSensor().isCompact()) {
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
        if (sample.getSensor().isCompact()) {
            int id = sample.getSensor().getId();
            lastValue.put(id, sample.getValue());
            lastUnchanged.remove(id);
        }
    }

}
