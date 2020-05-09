# Kaminari

[Kaminari](https://kaminari.shredzone.org) is an open source Franklin lightning detector project, based on the AS3935 detector chip.

Geordi is able to read lightning events, but also the current noise floor level and detected disturbers. For lightning events, the estimated distance and energy can be stored.

## Configuration

First insert a new row to the device table, with the type `kaminari`. In the JSON configuration, set `"host"` to the local IP address of your lightning sensor, and `"apikey"` to the API key that is to be used.

Example:

```sql
INSERT INTO device (name, type, cron, config) VALUES (
  'Lightning',
  'kaminari',
  '0 */5 * * * ?',
  '{"host":"192.168.1.99","apikey":"yOuRsEcReTaPiKeY"}'
);
```

It adds a `kaminari` device called `"Lightning"`. It is polled every 5 minutes. The detector can be reached in your local network at `http://192.168.1.99`, and requires the API key `yOuRsEcReTaPiKeY` for clearing the events.

<div class="alert alert-info" role="alert">

There should be only one Geordi instance reading the Kaminari sensor. If you have to use multiple instances (which is not recommended), make sure that only the last instance polling the status has the `apikey` set.
</div>

Now you can insert sensors into the `sensor` table.

Example, assuming that the device ID of the insert above was 1:

```sql
INSERT INTO sensor (device_id, name, unit, config) VALUES
  (1, 'Lightning Energy'  , ''     , '{"lightning_key":"energy"}'),
  (1, 'Lightning Distance', 'km'   , '{"lightning_key":"distance"}'),
  (1, 'Noise Floor Level' , 'µVrms', '{"key":"noiseFloorLevel"}'),
  (1, 'Generic Energy'    , ''     , '{"key":"energy"}'),
  (1, 'Generic Distance'  , 'km'   , '{"key":"distance"}')
;
```

In the `config` column, there are two kind of keys. `lightning_key` refers to the value of an actually detected lightning. Its timestamp is the time of the actual lighting event. `key` refers to a generic value, with the timestamp of the time Geordi picked up the value.
