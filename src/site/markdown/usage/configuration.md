# Configuration

Once Geordi is installed and running, it is only configured via the database.

At first, insert a row into the `device` table for each device to be polled. The columns are set like this:

* `name`: A human-readable name of the device to be polled.
* `type`: The device type ([see here](../device/index.html)).
* `cron`: A [cron expression](https://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html) defining when the device is to be polled.
* `config`: A JSON structure containing further configuration parameters for the device (like its hostname).

After that, insert rows into the `sensor` table for each sensor of that device. The colums are set like this:

* `device_id`: Database ID of the device that has been inserted in the previous step.
* `name`: A human-readable name of the sensor that is read.
* `unit`: The physical unit of the sensor value.
* `compact`: If `true`, only changes are stored in database. If `false`, every valid sensor reading is stored, even if the sensor value is unchanged. Default is `true`.
* `config`: A JSON structure containing further configuration parameters for the sensor (like a sensor identifier).

You will find details about the device type, device config and sensor config in the [respective chapters](../device/index.html).

If your database changes are completed, you need to restart Geordi. If it runs on systemd:

```sh
systemctl restart geordi
```

Check the log output to verify that your configuration was correct.

## Remove a sensor or device

To remove a sensor, you first have to delete all the samples that are referencing the sensor:

```sql
DELETE FROM sample WHERE sensor_id = 123;
```

After that, you can remove the sensor itself:

```sql
DELETE FROM sensor WHERE id = 123;
```

To remove a device, first remove all samples and sensors connected to it, then remove the device itself.

Geordi needs to be restarted after that.

<div class="alert alert-info" role="alert">

Geordi should be stopped before removing sensors or devices. Otherwise, it will continue to fill the database with sensor values.
</div>
