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
package org.shredzone.geordi.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.StatementContext;
import org.json.JSONException;
import org.json.JSONObject;
import org.quartz.CronExpression;
import org.shredzone.geordi.data.Sample;
import org.shredzone.geordi.device.Device;
import org.shredzone.geordi.sensor.Sensor;

/**
 * Implementation of {@link DatabaseService} that uses a Postgresql database via JDBI.
 */
@Singleton
public class DatabaseServiceImpl implements DatabaseService {

    @Inject
    private Map<String, Provider<Device>> devices;

    @Inject
    private Provider<Sensor> sensorProvider;

    @Inject
    private Jdbi jdbi;

    @Override
    public List<Device> fetchDevices() {
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT * FROM device")
                    .map(new DeviceMapper())
                    .list()
        );
    }

    @Override
    public Device getDevice(int id) {
        // TODO: Lazily prefetch all devices, then use that map
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT * FROM device WHERE id=:id")
                    .bind("id", id)
                    .map(new DeviceMapper())
                    .findOnly()
        );
    }

    @Override
    public List<Sensor> fetchSensors(Device device) {
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT * FROM sensor WHERE device_id=:id")
                    .bind("id", device.getId())
                    .map(new SensorMapper())
                    .list()
        );
    }

    @Override
    public void storeSamples(Collection<Sample> samples) {
        if (samples.isEmpty()) {
            return;
        }

        jdbi.useHandle(handle -> {
            PreparedBatch batch = handle.prepareBatch(
                            "INSERT INTO sample (sensor_id, time, value)"
                            + " VALUES (:s.sensor.id, :s.timestamp, :s.value)"
                            + " ON CONFLICT (sensor_id, time) DO NOTHING");
            samples.forEach(s -> batch.bindBean("s", s).add());
            batch.execute();
        });
    }

    /**
     * Maps a row of the device table to a {@link Device} object.
     */
    private class DeviceMapper implements RowMapper<Device> {
        @Override
        public Device map(ResultSet rs, StatementContext ctx) throws SQLException {
            try {
                Device dev = devices.get(rs.getString("type")).get();
                dev.setId(rs.getInt("id"));
                dev.setName(rs.getString("name"));
                dev.setCron(new CronExpression(rs.getString("cron")));
                dev.setConfig(new JSONObject(rs.getString("config")));
                return dev;
            } catch (ParseException ex) {
                throw new SQLException("Bad cron expression", ex);
            } catch (JSONException ex) {
                throw new SQLException("Bad config JSON", ex);
            }
        }
    }

    /**
     * Maps a row of the sensor table to a {@link Sensor} object.
     */
    private class SensorMapper implements RowMapper<Sensor> {
        @Override
        public Sensor map(ResultSet rs, StatementContext ctx) throws SQLException {
            try {
                Sensor sens = sensorProvider.get();
                sens.setId(rs.getInt("id"));
                sens.setName(rs.getString("name"));
                sens.setUnit(rs.getString("unit"));
                sens.setConfig(new JSONObject(rs.getString("config")));
                sens.setCompact(rs.getBoolean("compact"));
                return sens;
            } catch (JSONException ex) {
                throw new SQLException("Bad config JSON", ex);
            }
        }
    }

}
