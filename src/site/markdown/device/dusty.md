# Particulates Sensor

[luftdaten.info](https://luftdaten.info/) is an open network of particulates sensors. The sensors can be assembled by anyone. They are made from cheap components that are readily available. Even though it is mainly used in Germany, sensors can be found all over the world.

Geordi is able to read the sensor values of such a sensor device. Beside the particulates sensor, also temperatures, humidity and atmospheric pressure can be read if the optional sensors are present.

## Configuration

First insert a new row to the device table, with the type `dusty`. In the JSON configuration, set `"host"` to the local IP address of your particulates sensor.

<div class="alert alert-info" role="alert">

Geordi does not read the sensor data from the luftdaten.info servers. It must have access to the same network that your particulates sensor is connected to, or to a reverse proxy that gives access to the sensor.
</div>

Example:

```sql
INSERT INTO device (name, type, cron, config) VALUES (
  'Garden',
  'dusty',
  '0 */2 * * * ?',
  '{"host":"192.168.1.98"}'
);
```

It adds a `dusty` device called `"Garden"`. It is polled every 2 minutes. The sensor can be reached in your local network at `http://192.168.1.98`.

<div class="alert alert-info" role="alert">

In the default configuration, the particulates sensor firmware reads the sensor values every 2 minutes. For this reason, it does not make much sense to poll the sensor values more often than that.
</div>

Now you can insert sensors into the `sensor` table.

Example, assuming that the device ID of the insert above was 1:

```sql
INSERT INTO sensor (device_id, name, unit, config) VALUES
  (1, 'PM10'             , 'µg/m³', '{"value_type":"SDS_P1"}'),
  (1, 'PM2.5'            , 'µg/m³', '{"value_type":"SDS_P2"}'),
  (1, 'Temperature'      , '°C'   , '{"value_type":"temperature"}'),
  (1, 'Humidity'         , '%'    , '{"value_type":"humidity"}'),
  (1, 'Dew Point'        , '°C'   , '{"value_type":"temperature", "dewpoint":true}'),
  (1, 'Pressure absolute', 'mbar' , '{"value_type":"BMP_pressure", "divisor":100}'),
  (1, 'Pressure relative', 'mbar' , '{"value_type":"BMP_pressure", "divisor":100, "height":250}')
;
```

In the `config` column, the `"value_type"` refers to the sensor that is read.

Additionally, Geordi offers a few operations that can be applied to the sensor values:

- `"divisor":100`: Divides the sensor value by the given divisor. It is usually used to convert the air pressure to `mbar`, but can be applied to other sensor readings as well.
- `"dewpoint":true`: The value of the given temperature sensor is converted to the [dew point](https://en.wikipedia.org/wiki/Dew_point) temperature. This operation requires the optional DHT22 temperature and humidity sensor to be present.
- `"height":250`: The value of the given (absolute) atmospheric pressure sensor is converted to the relative pressure. The value of this parameter is the elevation above sea level of your sensor, in meters. This operation requires the optional BMP180, BMP280 or BME280 sensor to be present.
