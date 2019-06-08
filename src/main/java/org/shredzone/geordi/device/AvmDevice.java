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
package org.shredzone.geordi.device;

import static java.math.RoundingMode.HALF_UP;
import static java.nio.charset.StandardCharsets.UTF_16LE;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
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
 * A {@link Device} implementation that reads values from AVM FRITZ!DECT smart home
 * devices.
 *
 * @see <a href="https://avm.de/">AVM GmbH</a>
 */
public class AvmDevice  extends Device {
    private static final char[] HEX = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };
    private static final String NO_SESSION = "0000000000000000";
    private static final BigDecimal TWO = new BigDecimal("2");
    private static final BigDecimal TEN = BigDecimal.TEN;
    private static final BigDecimal ONE_THOUSAND = new BigDecimal("1000");

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private DatabaseService databaseService;

    @Override
    public List<Sample> readSensors() {
        String sid = getSessionId();
        XQuery values = fetchFromServer(sid);
        Instant instant = Instant.now();

        return databaseService.fetchSensors(this).stream()
                .map(sensor -> getSensorValue(values, sensor, instant))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Reads the current sensor value from the given {@link Sensor}.
     *
     * @param values
     *         XML that was read from the AHA interface
     * @param sensor
     *         {@link Sensor} to be read
     * @param instant
     *         {@link Instant} of sensor reading
     * @return {@link Sample} containing the sensor value
     */
    private Sample getSensorValue(XQuery values, Sensor sensor, Instant instant) {
        try {
            JSONObject config = sensor.getConfig();

            XQuery sensorDevice = values.get(String.format(
                    "//device[@identifier='%s']",
                    config.getString("ain")
            ));

            if ("0".equals(sensorDevice.get("present").text())) {
                return null;
            }

            BigDecimal value = null;
            String type = config.getString("type");
            if ("power".equals(type)) {
                value = getValue(sensorDevice, "powermeter/power",
                        v -> v.setScale(3, HALF_UP)
                            .divide(ONE_THOUSAND, HALF_UP));
            } else if ("voltage".equals(type)) {
                value = getValue(sensorDevice, "powermeter/voltage",
                        v -> v.setScale(3, HALF_UP)
                            .divide(ONE_THOUSAND, HALF_UP));
            } else if ("temperature".equals(type)) {
                value = getValue(sensorDevice, "temperature/celsius",
                        v -> v.setScale(1, HALF_UP)
                            .divide(TEN, HALF_UP));
            } else if ("switch".equals(type)) {
                value = getValue(sensorDevice, "switch/state", v -> v);
            } else if ("alert".equals(type)) {
                value = getValue(sensorDevice, "alert/state", v -> v);
            } else if ("currentTemperature".equals(type)) {
                value = getValue(sensorDevice, "hkr/tist", AvmDevice::convertTemp);
            } else if ("targetTemperature".equals(type)) {
                value = getValue(sensorDevice, "hkr/tsoll", AvmDevice::convertTemp);
            }

            if (value != null) {
                return new Sample(sensor, instant, value);
            }
        } catch (Exception ex) {
            log.warn("Could not read sensor id {} ({})", sensor.getId(), sensor.getName(), ex);
        }
        return null;
    }

    /**
     * Gets the value from the XML structure.
     *
     * @param xml
     *         XML to get the value from
     * @param path
     *         Value's XPath
     * @param mapper
     *         A mapper that converts the value that was found
     * @return Value, or {@code null} if the value is not present
     */
    private BigDecimal getValue(XQuery xml, String path, Function<BigDecimal, BigDecimal> mapper) {
        return xml.select(path)
                .findFirst()
                .map(XQuery::text)
                .filter(t -> !t.isEmpty())
                .map(BigDecimal::new)
                .map(mapper)
                .orElse(null);
    }

    /**
     * Logs into the AHA interface and returns a Session ID.
     *
     * @return Session ID
     */
    private String getSessionId() {
        try {
            String challenge;

            URL url1 = new URL(getHostName() + "/login_sid.lua");
            try (Reader in = new InputStreamReader(url1.openStream(), UTF_8)) {
                XQuery xml = XQuery.parse(in);
                String sid = findSessionId(xml);
                if (sid != null && !NO_SESSION.equals(sid)) {
                    return sid;
                }

                challenge = findChallenge(xml);
            }

            if (challenge == null) {
                throw new GeordiException("No challenge provided by FRITZ!Box " + getHostName());
            }

            String user = getConfig().getString("user");
            String password = getConfig().getString("password");
            String response = computeResponse(challenge, password);

            URL url2 = new URL(getHostName() + "/login_sid.lua?username="
                    + user + "&response=" + response);
            try (Reader in = new InputStreamReader(url2.openStream(), UTF_8)) {
                XQuery xml = XQuery.parse(in);
                String sid = findSessionId(xml);
                if (sid == null || NO_SESSION.equals(sid)) {
                    throw new GeordiException("Access denied by FRITZ!Box " + getHostName() + " for user " + user);
                }

                return sid;
            }
        } catch (IOException ex) {
            throw new GeordiException("Could not connect to FRITZ!Box", ex);
        }
    }

    /**
     * Finds the session ID in the XML structure.
     *
     * @param xml
     *         XML to search
     * @return Session ID, or {@code null} if none was found
     */
    private String findSessionId(XQuery xml) {
        return xml.select("/SessionInfo/SID")
                .findFirst()
                .map(XQuery::text)
                .orElse(null);
    }

    /**
     * Finds the challenge in the XML structure.
     *
     * @param xml
     *         XML to search
     * @return Challenge, or {@code null} if none was found
     */
    private String findChallenge(XQuery xml) {
        return xml.select("/SessionInfo/Challenge")
                .findFirst()
                .map(XQuery::text)
                .orElse(null);
    }

    /**
     * Computes the response to the given challenge.
     *
     * @param challenge
     *         Challenge sent by the AHA interface.
     * @param password
     *         User's password
     * @return Response to the challenge
     */
    private String computeResponse(String challenge, String password) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(challenge.getBytes(UTF_16LE));
            md5.update("-".getBytes(UTF_16LE));
            md5.update(password.getBytes(UTF_16LE));
            StringBuilder sb = new StringBuilder();
            sb.append(challenge).append('-');
            for (byte b : md5.digest()) {
                sb.append(HEX[(b >> 4) & 0x0F]).append(HEX[b & 0x0F]);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex); // Should never happen, md5 is standard
        }
    }

    /**
     * Reads the current status from the AHA interface (aka FRITZ!Box).
     *
     * @param sid
     *         Session ID
     * @return XML containing the overall sensor status
     */
    private XQuery fetchFromServer(String sid) {
        try {
            URL url = new URL(getHostName()
                    + "/webservices/homeautoswitch.lua"
                    + "?switchcmd=getdevicelistinfos"
                    + "&sid=" + sid);

            try (Reader in = new InputStreamReader(url.openStream(), UTF_8)) {
                return XQuery.parse(in);
            }
        } catch (IOException ex) {
            throw new GeordiException("Could not read from FRITZ!Box", ex);
        }
    }

    /**
     * Returns the host name of the AHA interface.
     *
     * @return Host name (e.g. "http://fritz.box")
     */
    private String getHostName() {
        return getConfig().optBoolean("tls", false) ? "https" : "http"
                + "://"
                + getConfig().optString("host", "fritz.box");
    }

    /**
     * Converts a thermostat decimal into a temperature value.
     *
     * @param value
     *         Decimal value
     * @return Temperature, or {@code null} if this is not a temperature value
     */
    private static BigDecimal convertTemp(BigDecimal value) {
        int val = value.intValue();
        if (val < 16 || val > 56) {
            // This is not a valid temperature value
            return null;
        }

        return value
                .setScale(1, HALF_UP)
                .divide(TWO, HALF_UP);
    }

}
