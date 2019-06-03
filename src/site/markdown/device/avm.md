# AVM

[AVM GmbH](https://avm.de) is a manufacturer for network components and smart home devices.

Geordi is able to read some sensor values of their smart home components, like remote power switches and thermostats.

## Configuration

First insert a new row to the device table, with the type `avm`. In the JSON configuration, you must set the `user` and `password` of the login to your FRITZ!Box. It is recommended to use a login that is limited to smart home permissions only.

By default, Gerodi connects to `http://fritz.box`. You can change the host name via `host`, and use https by setting `tls` to `true`.

Example:

```sql
INSERT INTO device (name, type, cron, config) VALUES (
  'FRITZ!Box',
  'avm',
  '0 */2 * * * ?',
  '{"user": "home", "password": "sEcReT!"}'
);
```

It adds an `avm` device called `"FRITZ!Box"`. It is polled every 2 minutes (sensor data is updated every 2 minutes, so it doesn't make much sense to poll more frequently). The FRITZ!Box router is expected at `http://fritz.box/`, which is the default.

Now you can insert sensors into the `sensor` table.

Example, assuming that the device ID of the insert above was 1:

```sql
INSERT INTO sensor (device_id, name, unit, config) VALUES
  (1, 'Power Meter'  , 'W' , '{"ain": "11657 0074701", "type": "power"}'),
  (1, 'Power Voltage', 'V' , '{"ain": "11657 0074701", "type": "voltage"}'),
  (1, 'Temperature'  , '°C', '{"ain": "11657 0074701", "type": "temperature"}')
;
```

Both the `ain` and `type` parameters are mandatory.

The `ain` is the _Actor Identification Number_ of the smart home component. You can find it in the administration panel of your FRITZ!Box in the "Smart Home" area, by editing the configuration of the desired component.

Available `type` values are:

* `power`: The current power consumed by the device that is connected to the remote power switch.
* `voltage`: The current grid power measured by the remote power switch.
* `temperature`: The current temperature of the remote power switch.
* `switch`: The current switching state of the remote power switch.
* `alert`: The current alert state of a remote alarm device.
* `currentTemperature`: The current temperature measured by the remote thermostate.
* `targetTemperature`: The target temperature the remote thermostate is set to.
