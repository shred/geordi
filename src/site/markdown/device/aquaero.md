# Aquaero

Aquaero is a fan controller by [Aqua Computer](https://aquacomputer.de/).

Geordi is able to read all sensor values of an Aquaero device, like temperatures, fan speeds or fan voltages.

[Pyquaero](https://github.com/shred/pyquaero) must be installed and running on the system the Aquaero is connected to via USB.

Pyquaero provides a tiny web server that provides all sensor readings as JSON structure. Geordi connects to that server. It is possible to read multiple Aquaero devices, even on different machines, with one Geordi instance.

## Configuration

First insert a new row to the device table, with the type `aquaero`. In the JSON configuration, set `"host"` to the host name where Pyquaero is running, and `"port"` to the port of the Pyquaero server.

Example:

```sql
INSERT INTO device (name, type, cron, config) VALUES (
  'Server Rack',
  'aquaero',
  '0 */10 * * * ?',
  '{"host":"pyquaero.localdomain", "port":9500}'
);
```

It adds an `aquaero` device called `"Server Rack"`. It is polled every 10 minutes. The Pyquaero server can be reached at `http://pyquaero.localdomain:9500/status`.

Now you can insert sensors into the `sensor` table. Have a look at the JSON status output of the Pyquaero server, to see the available sensors and the structure.

Example, assuming that the device ID of the insert above was 1:

```sql
INSERT INTO sensor (device_id, name, unit, config) VALUES
  (1, 'Main Fan'          , 'RPM', '{"type":"fans", "index":0, "value":"speed"}'),
  (1, 'Intake Temperature', '°C' , '{"type":"temperatures/sensor", "index":0, "value":"temp"}')
;
```
