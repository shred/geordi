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
package org.shredzone.geordi.service;

import java.util.Collection;
import java.util.List;

import org.shredzone.geordi.data.Sample;
import org.shredzone.geordi.device.Device;
import org.shredzone.geordi.sensor.Sensor;

/**
 * A service that handles all database related things.
 */
public interface DatabaseService {

    /**
     * Returns a list of all {@link Device} defined in the database.
     *
     * @return List of {@link Device}
     */
    public List<Device> fetchDevices();

    /**
     * Returns the {@link Device} with the given ID.
     *
     * @param id
     *            Device ID
     * @return {@link Device}
     */
    public Device getDevice(int id);

    /**
     * Returns all {@link Sensor} of a {@link Device}.
     *
     * @param device
     *            {@link Device} to get the {@link Sensor} list for
     * @return List of {@link Sensor}
     */
    public List<Sensor> fetchSensors(Device device);

    /**
     * Bulk stores all {@link Sample} into the database.
     *
     * @param samples
     *            Collection of {@link Sample} to store into the database.
     */
    public void storeSamples(Collection<Sample> samples);

}
