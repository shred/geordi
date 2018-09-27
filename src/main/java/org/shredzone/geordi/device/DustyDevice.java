/*
 * geordi
 *
 * Copyright (C) 2018 Richard "Shred" Körber
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
package org.shredzone.geordi.device;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
 * A {@link Device} implementation that reads particulate sensors. It also supports all
 * other sensors that can be attached to the device, like temperature, humidity, and
 * atmospheric pressure.
 * <p>
 * Note that <em>Dusty</em> is not an official term for the device. It refers to the
 * German word for particulates, "Feinstaub", which literally translates to "fine dust".
 *
 * @see <a href="https://luftdaten.info/">luftdaten.info</a>
 */
public class DustyDevice extends Device {

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

        // Dusty only returns the "age" of the last sample, in seconds. To convert it
        // into a timestamp, we subtract the "age" from the current time, and truncate
        // it to slots having a width of 2 seconds. This way, "ts" will always contain
        // the same timestamp of the sample, taking into account that Dusty's internal
        // clock is not synchronized to the server's clock.
        Instant ts = Instant.now()
                    .minus(json.getLong("age"), ChronoUnit.SECONDS)
                    .with(DustyDevice::truncate2Seconds);

        JSONArray values = json.getJSONArray("sensordatavalues");

        List<Sample> result = new ArrayList<>();
        for (Sensor sensor : databaseService.fetchSensors(this)) {
            getSensorValue(values, sensor)
                    .map(value -> new Sample(sensor, ts, value))
                    .ifPresent(result::add);
        }

        return result;
    }

    /**
     * Reads the value of a {@link Sensor} from the JSON response.
     *
     * @param values
     *            JSON response of Dusty
     * @param sensor
     *            {@link Sensor} to read
     * @return Sensor value, or empty if the sensor provided no value
     */
    private Optional<BigDecimal> getSensorValue(JSONArray values, Sensor sensor) {
        JSONObject config = sensor.getConfig();

        String key = config.getString("value_type");

        Optional<BigDecimal> result = findValue(values, key);

        if (config.has("divisor")) {
            result = result.map(it -> it.divide(config.getBigDecimal("divisor")));
        }

        if (config.has("height")) {
            Optional<BigDecimal> temp = findValue(values, "BMP_temperature");
            if (!temp.isPresent()) {
                return Optional.empty();
            }
            result = result.map(BigDecimal::doubleValue)
                    .map(it -> convertToRelative(it, temp.get().doubleValue(), config.getInt("height")))
                    .map(BigDecimal::new)
                    .map(it -> it.setScale(2, RoundingMode.HALF_UP));
        }

        if (config.has("dewpoint") && config.getBoolean("dewpoint") == true) {
            Optional<BigDecimal> humidity = findValue(values, "humidity");
            if (!humidity.isPresent()) {
                return Optional.empty();
            }

            // Avoid infinity as result
            BigDecimal humidVal = humidity.get();
            if (BigDecimal.ZERO.equals(humidVal)) {
                return Optional.empty();
            }

            result = result.map(BigDecimal::doubleValue)
                    .map(it -> dewpoint(it, humidVal.doubleValue()))
                    .map(BigDecimal::new)
                    .map(it -> it.setScale(2, RoundingMode.HALF_UP));
        }

        return result;
    }

    /**
     * Finds a sensor value in the JSON data.
     *
     * @param values
     *            JSON data
     * @param key
     *            Sensor key
     * @return Sensor value, or empty if not found
     */
    private Optional<BigDecimal> findValue(JSONArray values, String key) {
        for (int ix = 0; ix < values.length(); ix++) {
            JSONObject jo = values.getJSONObject(ix);
            if (key.equals(jo.getString("value_type"))) {
                return Optional.of(jo.getBigDecimal("value"));
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the URL of Dusty's web server.
     */
    private URL getServerUrl() {
        try {
            return new URL(String.format("http://%s/data.json", getConfig().getString("host")));
        } catch (MalformedURLException | JSONException ex) {
            throw new GeordiException("Bad host config", ex);
        }
    }

    /**
     * Converts absolute air pressure to relative air pressure.
     *
     * @param absolute
     *            Absolute pressure, in mbar
     * @param temperature
     *            Temperature, in °C
     * @param height
     *            Height above sea level, in meters
     * @return Relative pressure, in mbar
     * @see <a href="http://keisan.casio.com/exec/system/1224575267">http://keisan.casio.com/exec/system/1224575267</a>
     */
    private static double convertToRelative(double absolute, double temperature, int height) {
        return absolute * Math.pow(
                1.0 - ((0.0065 * height) / (temperature + 0.0065 * height + 273.15)),
                -5.257);
    }

    /**
     * Computes the dew point from the temperature and humidity.
     *
     * @param temp
     *            Temperature (in °C)
     * @param humid
     *            Humidity (in percent)
     * @return Dew point
     * @see <a href="https://en.wikipedia.org/wiki/Dew_point">https://en.wikipedia.org/wiki/Dew_point</a>
     */
    private static double dewpoint(double temp, double humid) {
        final double k2 = 17.62;
        final double k3 = 243.12;

        double d1 = ((k2 * temp) / (k3 + temp)) + Math.log(humid / 100.0);
        double d2 = ((k2 * k3) / (k3 + temp)) - Math.log(humid / 100.0);

        return k3 * (d1 / d2);
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
