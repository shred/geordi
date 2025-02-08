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
package org.shredzone.geordi.sensor;

import org.json.JSONObject;
import org.shredzone.geordi.device.Device;

/**
 * A {@link Sensor} describes a single sensor on a {@link Device}.
 */
public class Sensor {

    private int id;
    private String name;
    private String unit;
    private JSONObject config;

    /**
     * Reads the sensor ID.
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the sensor ID. It must be unique.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Reads the human-readable sensor name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets a human-readable sensor name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Reads the physical unit of the sensor value.
     */
    public String getUnit() {
        return unit;
    }

    /**
     * Sets the physical unit of the sensor value.
     */
    public void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * Reads the sensor config.
     */
    public JSONObject getConfig() {
        return config;
    }

    /**
     * Sets the sensor config.
     */
    public void setConfig(JSONObject config) {
        this.config = config;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Sensor)) {
            return false;
        }

        return ((Sensor) obj).id == this.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

}
