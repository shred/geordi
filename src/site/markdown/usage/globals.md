# Global Options

Devices and sensors are individually configured by JSON structures, but there are also some global parameters that can be used on all devices and sensors.

To distinguish global from local parameters, global parameters always start with a capital letter by convention.

## Sensor Parameters

* `Compacting` (boolean): Usually Geordi stores every valid sensor sample that was delivered by the device, even if the sensor value was unchanged. If the sensor is read very frequently, but rarely changes its value, this might fill the database with unnecessary records which are consuming precious disk space.

  If `Compacting` is present and set to `true`, Geordi will not store a sensor value if it is equal to the last stored value.

* `CompactingMaxInterval` (string): If compacting is enabled on sensors that rarely change their values, it may lead to that the last sample in the database may become several hours or even days old. If an interval is given with this option, Geordi makes sure to store a sample at least in the given intervals, even if the value was unchanged.

  This is an [ISO-8601](https://en.wikipedia.org/wiki/ISO_8601) formatted duration, e.g. `"P2D"` for "two days" or `"PT1H"` for "one hour".

## Device Parameters

There are currently no global device parameters.
