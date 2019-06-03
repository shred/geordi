# Installation

Geordi does not come with a fancty installer. We need to do the steps manually.

## Prerequistes

Geordi requires:

* Java 8 or higher (you can use OpenJDK or Oracle Java)
* Postgresql 9.3 or higher

## Getting the Binary

You can download a precompiled `geordi.jar` file [at GitHub](https://github.com/shred/geordi/releases). It is a static jar file that already contains Geordi and all the required libraries it depends on.

To build Geordi yourself, check out the project [at GitHub](https://github.com/shred/geordi) and build it with maven (`mvn clean install`). You will then find the `geordi.jar` file in the `target` folder of your checkout.

For RedHat based systems, there is also a rpm file [at GitHub](https://github.com/shred/geordi/releases). After installation, just edit the `/etc/geordi.conf` file and start Geordi via systemd as described below.

## Database

Create a new Postgresql database for Geordi, then create the tables and sequences:

```sql
CREATE SEQUENCE ser_device;

CREATE TABLE device (
  id integer PRIMARY KEY DEFAULT nextval('ser_device'),
  name varchar(255) NOT NULL,
  type varchar(100) NOT NULL,
  cron varchar(50) NOT NULL,
  config json NOT NULL default '{}'
);

CREATE SEQUENCE ser_sensor;

CREATE TABLE sensor (
  id integer PRIMARY KEY DEFAULT nextval('ser_sensor'),
  device_id integer NOT NULL REFERENCES device,
  name varchar(255) NOT NULL,
  unit varchar(20) NOT NULL,
  compact boolean NOT NULL DEFAULT TRUE,
  config json NOT NULL default '{}'
);

CREATE TABLE sample (
  sensor_id integer NOT NULL REFERENCES sensor,
  time timestamptz NOT NULL,
  value decimal NOT NULL,
  UNIQUE(sensor_id, time)
);
```

The database will later contain the sensor configurations and the collected sensor data.

<div class="alert alert-info" role="alert">

Depending on the number of sensors and the poll frequency, the database can grow to a considerable size of some gigabytes per year. I recommend to provision sufficient space on the database partition, and to use an SSD for a better evaluation performance.
</div>

## Starting Geordi

Start Geordi from the command line:

```sh
java -jar geordi.jar
```

By default, it will connect to a database called `geordi` on the local machine, without giving authentication. You can change this behavior either by command line parameters, or by environment variables.

Let's assume your Postgresql server is at `postgres.localdomain`, the database is called `sensors`, the database user is `geordi` and the password is `laForge`. To connect to it via command line, invoke Geordi like this:

```sh
java -jar geordi.jar \
  --database jdbc:postgresql://postgres.localdomain/sensors \
  --user geordi \
  --password laForge
```

You can also use environment variables, so the password is not visible in the process list:

```sh
export GEORDI_DATABASE=jdbc:postgresql://postgres.localdomain/sensors
export GEORDI_USER=geordi
export GEORDI_PASSWORD=laForge
java -jar geordi.jar
```

To stop Geordi again, just kill the process or press ctrl-c on the command line.

## systemd

To run Geordi on Linux via systemd, create a file `/usr/lib/systemd/system/geordi.service` with the following content (adapt the `ExecStart` paths to your installation):

```
[Unit]
Description=Geordi, the sensor poller

[Service]
EnvironmentFile=/etc/geordi.conf
User=nobody
ExecStart=/usr/bin/java -jar /usr/local/lib/geordi.jar
SuccessExitStatus=143
TimeoutStopSec=10
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

Also create a file `/etc/geordi.conf` with the following content (adapted to your database):

```
GEORDI_DATABASE=jdbc:postgresql://postgres.localdomain/sensors
GEORDI_USER=geordi
GEORDI_PASSWORD=laForge
```

Make sure this file can only be read by the root user.

You can now start Geordi via:

```
systemctl start geordi
```

Check if Geordi is actually running:

```
systemctl status geordi
```

To make Geordi start after a reboot, use:

```
systemctl enable geordi
```
