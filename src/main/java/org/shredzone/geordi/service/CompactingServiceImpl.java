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

    @Override
    public void rememberSample(Sample sample) {
        if (sample.getSensor().isCompact()) {
            lastValue.put(sample.getSensor().getId(), sample.getValue());
        }
    }

    @Override
    public boolean wasUnchanged(Sample sample) {
        if (!sample.getSensor().isCompact()) {
            return false;
        }

        BigDecimal last = lastValue.get(sample.getSensor().getId());
        if (last == null) {
            return false;
        }

        return last.compareTo(sample.getValue()) == 0;
    }

}
