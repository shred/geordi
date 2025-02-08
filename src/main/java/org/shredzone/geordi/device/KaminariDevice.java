/*
 * geordi
 *
 * Copyright (C) 2020 Richard "Shred" Körber
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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
 * A {@link Device} implementation for the Kaminari lightning sensor project.
 *
 * @see <a href="https://kaminari.shredzone.org">Kaminari project page</a>
 */
public class KaminariDevice extends Device {

    @Inject
    private DatabaseService databaseService;

    @Override
    public List<Sample> readSensors() {
        JSONObject json;
        try (InputStream in = openConnection("status").getInputStream()) {
            json = new JSONObject(new JSONTokener(in));
        } catch (IOException | JSONException ex) {
            throw new GeordiException("Could not read data for sensor " + getId(), ex);
        }

        List<Sensor> sensors = databaseService.fetchSensors(this);
        List<Sample> result = new ArrayList<>();

        JSONArray values = json.getJSONArray("lightnings");
        for (int ix = 0; ix < values.length(); ix++) {
            JSONObject jo = values.getJSONObject(ix);
            for (Sensor sensor : sensors) {
                getLightningValue(jo, sensor).ifPresent(result::add);
            }
        }

        Instant now = Instant.now();
        for (Sensor sensor : sensors) {
            getSensorValue(json, sensor, now).ifPresent(result::add);
        }

        try (InputStream in = openConnection("clear").getInputStream()) {
            while (in.read() != -1) {
                // intentionally left empty
            }
        } catch (IOException ex) {
            throw new GeordiException("Could not clear data for sensor " + getId(), ex);
        }

        return result;
    }

    private Optional<Sample> getLightningValue(JSONObject values, Sensor sensor) {
        JSONObject config = sensor.getConfig();
        if (!config.has("lightning_key")) {
            return Optional.empty();
        }

        Instant ts = Instant.now()
                .minus(values.getLong("age"), ChronoUnit.SECONDS)
                .with(KaminariDevice::truncate2Seconds);

        String key = config.getString("lightning_key");
        return Optional.ofNullable(values.optBigDecimal(key, null))
                .map(value -> new Sample(sensor, ts, value));
    }

    /**
     * Reads the value of a {@link Sensor} from the JSON response.
     *
     * @param values
     *            JSON response of Kaminari
     * @param sensor
     *            {@link Sensor} to read
     * @return Sensor value, or empty if the sensor provided no value
     */
    private Optional<Sample> getSensorValue(JSONObject values, Sensor sensor, Instant now) {
        JSONObject config = sensor.getConfig();
        if (!config.has("key")) {
            return Optional.empty();
        }

        String key = config.getString("key");
        return Optional.ofNullable(values.optBigDecimal(key, null))
                .map(value -> new Sample(sensor, now, value));
    }

    /**
     * Opens a connection to Kaminari.
     */
    private HttpURLConnection openConnection(String target) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) getServerUrl(target).openConnection();
        String apikey = getConfig().getString("apikey");
        if (apikey != null) {
            connection.setRequestProperty("X-API-Key", apikey);
        }
        return connection;
    }

    /**
     * Returns the URL of Kaminari's web server.
     */
    private URL getServerUrl(String target) {
        try {
            return new URL(String.format("http://%s/%s", getConfig().getString("host"), target));
        } catch (MalformedURLException | JSONException ex) {
            throw new GeordiException("Bad host config", ex);
        }
    }

    /**
     * Truncates the {@link Temporal} at "2 seconds" intervals.
     *
     * @param temporal
     *            {@link Temporal} to truncate
     * @return Truncated {@link Temporal}
     */
    private static Temporal truncate2Seconds(Temporal temporal) {
        long time = temporal.getLong(ChronoField.INSTANT_SECONDS);
        time = ((time + 1L) / 2L) * 2L;
        return Instant.ofEpochSecond(time);
    }

}
