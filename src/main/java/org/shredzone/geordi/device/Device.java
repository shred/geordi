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
package org.shredzone.geordi.device;

import java.util.List;

import org.json.JSONObject;
import org.quartz.CronExpression;
import org.shredzone.geordi.data.Sample;

/**
 * A {@link Device} is some kind of hardware that is to be frequently polled for new
 * sensor data. A device can have one or more sensors.
 * <p>
 * Even though all Geordi devices use the network to access sensor data, this is not a
 * requirement. A {@link Device} implementation could also read hardware sensors directly.
 */
public abstract class Device {

    private int id;
    private String name;
    private CronExpression cron;
    private JSONObject config;

    /**
     * Reads the device ID.
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the device ID. It must be unique.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Reads a human-readable device name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the device name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Reads the {@link CronExpression} that is used for polling the device sensors.
     */
    public CronExpression getCron() {
        return cron;
    }

    /**
     * Sets the {@link CronExpression} that is used for polling the device sensors.
     */
    public void setCron(CronExpression cron) {
        this.cron = cron;
    }

    /**
     * Reads the JSON configuration of the device.
     */
    public JSONObject getConfig() {
        return config;
    }

    /**
     * Sets the JSON configuration of the device.
     */
    public void setConfig(JSONObject config) {
        this.config = config;
    }

    /**
     * Reads all sensors of this device.
     *
     * @return List of {@link Sample} objects containing all current sensor values that
     *         have been read.
     */
    public abstract List<Sample> readSensors();

}
