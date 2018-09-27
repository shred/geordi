# Homematic

Homematic is a brand for home automation products by [eQ-3 AG](https://www.eq-3.de/).

Geordi is able to connect to a CCU2 or CCU3, and read the current state of all connected devices.

The [XML-API CCU Addon](https://github.com/hobbyquaker/XML-API) must be installed on the CCU, as a prerequisite.

## Configuration

First insert a new row to the device table, with the type `ccu2`. In the JSON configuration, set `"host"` to the name or IP address of your CCU.

<div class="alert alert-info" role="alert">

Use the `ccu2` device type even if you are connecting to a CCU3.
</div>

Example:

```sql
INSERT INTO device (name, type, cron, config) VALUES (
  'Homematic',
  'ccu2',
  '0 */1 * * * ?',
  '{"host":"ccu2.localdomain"}'
);
```

It adds a `ccu2` device called `"Homematic"`. It is polled every minute. The CCU can be reached in your network at `http://ccu2.localdomain`.

Adding the sensors is a little difficult, since you need to find out how the individual sensor is addressed in the CCU. First read the current state of the CCU, by pointing your browser to `http://ccu2.localdomain/addons/xmlapi/statelist.cgi`. It will show a long and rather complex XML structure. Now locate the device you want to read, and in that block, find the datapoint of your interest. You will find an XML line like this:

```xml
<datapoint name="HmIP-RF.000X123456FEDC:1.ACTUAL_TEMPERATURE" type="ACTUAL_TEMPERATURE" ise_id="1849" value="22.900000" valuetype="4" valueunit="" timestamp="1235082668" operations="5"/>
```

The datapoint name is what we need for the Geordi database. It consists of the type (`HmIP-RF` for Homematic IP devices, `BidCos-RF` for Homematic devices), the serial number of that device, the channel number, and the datapoint type. This datapoint name will never change, even if the device or the CCU is reset to factory defaults.

Geordi is able to handle numerical and boolean sensor values. For boolean values, _false_ is stored as 0 and _true_ is stored as 1.

Example, assuming that the device ID of the insert above was 1:

```sql
INSERT INTO sensor (device_id, name, unit, config) VALUES
  (1, 'Kitchen Temperature', '°C',
    '{"datapointName":"HmIP-RF.000X123456FEDC:1.ACTUAL_TEMPERATURE"}'),
  (1, 'Kitchen Humidity'   , '%' ,
    '{"datapointName":"HmIP-RF.000X123456FEDC:1.HUMIDITY"}')
;
```
