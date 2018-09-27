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
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.json.JSONObject;
import org.shredzone.commons.xml.XQuery;
import org.shredzone.geordi.GeordiException;
import org.shredzone.geordi.data.Sample;
import org.shredzone.geordi.sensor.Sensor;
import org.shredzone.geordi.service.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link Device} implementation that reads values from Homematic CCU2 home automation
 * centrals.
 * <p>
 * Requires the <a href="https://github.com/hobbyquaker/XML-API">XML-API CCU addon</a> to
 * be installed on the CCU2.
 *
 * @see <a href="https://www.eq-3.de/">eQ-3 AG</a>
 */
public class Ccu2Device extends Device {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private DatabaseService databaseService;

    @Override
    public List<Sample> readSensors() {
        XQuery values = fetchFromServer();

        return databaseService.fetchSensors(this).stream()
                .map(sensor -> getSensorValue(values, sensor))
                .filter(it -> it != null)
                .collect(Collectors.toList());
    }

    /**
     * Reads the current sensor value from the given {@link Sensor}.
     *
     * @param values
     *            XML that was read from the CCU2
     * @param sensor
     *            {@link Sensor} to be read
     * @return {@link Sample} containing the sensor value
     */
    private Sample getSensorValue(XQuery values, Sensor sensor) {
        try {
            JSONObject config = sensor.getConfig();

            XQuery sensorDevice;

            if (config.has("datapointName")) {
                sensorDevice = values.get(String.format(
                        "//datapoint[@name='%s']",
                        config.getString("datapointName")));
            } else if (config.has("datapointId")) {
                sensorDevice = values.get(String.format(
                        "/stateList/device[@ise_id='%d']/channel[@ise_id='%d']/datapoint[@ise_id='%d']",
                        config.getInt("deviceId"),
                        config.getInt("channelId"),
                        config.getInt("datapointId")));
            } else {
                sensorDevice = values.get(String.format(
                        "/stateList/device[@ise_id='%d']/channel[@ise_id='%d']/datapoint[@type='%s']",
                        config.getInt("deviceId"),
                        config.getInt("channelId"),
                        config.getString("type")));
            }

            String timestampStr = sensorDevice.attr().get("timestamp");
            Instant timestamp = Instant.ofEpochMilli(Long.parseLong(timestampStr) * 1000L);

            String valueStr = sensorDevice.attr().get("value");
            BigDecimal value;
            if ("false".equals(valueStr)) {
                value = BigDecimal.ZERO;
            } else if ("true".equals(valueStr)) {
                value = BigDecimal.ONE;
            } else {
                value = new BigDecimal(valueStr);
            }

            return new Sample(sensor, timestamp, value);
        } catch (Exception ex) {
            log.warn("Could not read sensor id {} ({})", sensor.getId(), sensor.getName(), ex);
            return null;
        }
    }

    /**
     * Reads the current status from the CCU2.
     *
     * @return XML containing the sensor status
     */
    private XQuery fetchFromServer() {
        try {
            URL url = new URL("http://"
                            + getConfig().getString("host")
                            + "/addons/xmlapi/statelist.cgi");

            try (Reader in = new InputStreamReader(url.openStream(), "iso-8859-1")) {
                return XQuery.parse(in);
            }
        } catch (IOException ex) {
            throw new GeordiException("Could not read CCU2", ex);
        }
    }

}
