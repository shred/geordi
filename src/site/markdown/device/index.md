# Devices

A device is a piece of hardware that is equipped with sensors. Geordi knows a set of device types already, so it is easy to get sensor readings from them:

* [Aquaero](./aquaero.html) fan controllers by Aqua Computer
* [Particulates Sensor](./dusty.html) by [luftdaten.info](https://luftdaten.info/)
* [Homematic](./homematic.html) home automation by eQ-3

## Adding new devices

With some Java knowledge, it is easy to add other hardware devices to the Geordi source code:

- Add a new class to the `org.shredzone.geordi.device` package. It must extend the class `org.shredzone.geordi.device.Device`.
- Implement the `List<Sample> readSensors()` method. It must return `Sample` instances for each sensor that was read.
- Use `getConfig()` to read the device's JSON configuration.
- You may use Guice in your device class, e.g. for injecting the `DatabaseService`.
- Remember to add a binding to your device implementation in `org.shredzone.geordi.GeordiModule`.

If you think other people might be interested in your device class, please consider to send a pull request. I will gladly add your class to Geordi's code base. Please remember to add some lines of documentation as well, so other people will know how to use your class.
