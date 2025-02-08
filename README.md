# Geordi ![build status](https://shredzone.org/badge/geordi.svg)

Geordi is a daemon that collects sensor data from various sources, and stores them in a database for later evaluation.

It is written in Java and runs on any operating system.

## Features

* Collects data from different sensors.
* Cron-like expressions for collection frequency.
* Easily extensible.
* Collected data can be processed by analytic tools like [Grafana](https://grafana.com/).
* Uses [Postgresql](https://www.postgresql.org/) as database (requires version 9.3 or higher).
* Only requires Java 8 or higher at runtime.

## Supported Sensors

* [Homematic](https://www.eq-3.de/) home automation devices (via CCU2, CCU3)
* [luftdaten.info](https://luftdaten.info/) particulates sensors
* [Aquaero](https://aquacomputer.de/aquaero-5.html) fan controllers (via [Pyquaero](https://codeberg.org/shred/pyquaero))
* [AVM](https://avm.de) smart home components (via FRITZ!Box).
* [Kaminari](https://kaminari.shredzone.org) Franklin lightning detector

Other sensor sources can be added easily.

## Documentation

Read the [online documentation](http://www.shredzone.org/maven/geordi/) about how to set up, configure and run your own Geordi instance.

Please note that Geordi does not come with a fancy installer or a configuration tool. You need Linux knowledge and some basic SQL skills to run it.

## Contribute

* Fork the [Source code at Codeberg](https://codeberg.org/shred/geordi). Feel free to send pull requests.
* Found a bug? [File a bug report!](https://codeberg.org/shred/geordi/issues)

## Licenses

_Geordi_ is open source software. The source code is distributed under the terms of [GNU General Public License V3](http://www.gnu.org/licenses/gpl-3.0.html).
