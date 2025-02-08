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

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.shredzone.geordi.GeordiException;
import org.shredzone.geordi.data.Sample;
import org.shredzone.geordi.sensor.Sensor;
import org.shredzone.geordi.service.DatabaseService;

/**
 * A {@link Device} implementation that reads Aquaero fan controllers. It
 * requires a running <a href="https://codeberg.org/shred/pyquaero">Pyquaero</a> server that
 * is connected to the Aquaero device.
 *
 * @see <a href="https://aquacomputer.de">Aqua Computer GmbH &amp; Co. KG</a>
 */
public class AquaeroDevice extends Device {

    @Inject
    private DatabaseService databaseService;

    @Override
    public List<Sample> readSensors() {
        JSONObject json;
        try (InputStream in = getServerUrl().openStream()) {
            json = new JSONObject(new JSONTokener(in));
        } catch (IOException | JSONException ex) {
            throw new GeordiException("Could not read data for sensor " + getId(), ex);
        }

        Instant ts = Instant.parse(json.getString("time") + "Z");

        List<Sample> result = new ArrayList<>();
        for (Sensor sensor : databaseService.fetchSensors(this)) {
            BigDecimal value = getSensorValue(json, sensor);

            // If Pyquaero runs on a Raspberry Pi 1, there might be a misreading of
            // the sensors due to a hardware bug. We will ignore the 0 value that is
            // returned from a misreading.
            if (BigDecimal.ZERO.equals(value)) {
                return Collections.emptyList();
            }

            result.add(new Sample(sensor, ts, value));
        }

        return result;
    }

    /**
     * Gets the {@link Sensor} value from the JSON data.
     *
     * @param json
     *            JSON data of Pyquaero
     * @param sensor
     *            {@link Sensor} to read
     * @return Value that was read
     */
    private BigDecimal getSensorValue(JSONObject json, Sensor sensor) {
        JSONObject config = sensor.getConfig();
        JSONArray data = locate(json, config.getString("type"));
        JSONObject values = data.getJSONObject(config.getInt("index"));
        return values.getBigDecimal(config.getString("value"));
    }

    /**
     * Locates a JSON array containing sensor data.
     *
     * @param json
     *            JSON data of pyquaero
     * @param path
     *            Path to the array
     * @return JSON array
     */
    private JSONArray locate(JSONObject json, String path) {
        String[] parts = path.split("[/.]");
        JSONObject current = json;
        for (int ix = 0; ix < parts.length - 1; ix++) {
            current = current.getJSONObject(parts[ix]);
        }
        return current.getJSONArray(parts[parts.length - 1]);
    }

    /**
     * Returns the URL of the Pyquaero server.
     */
    private URL getServerUrl() {
        try {
            return new URL(String.format("http://%s:%d/status",
                    getConfig().getString("host"),
                    getConfig().getInt("port")));
        } catch (MalformedURLException | JSONException ex) {
            throw new GeordiException("Bad host config", ex);
        }
    }

}
