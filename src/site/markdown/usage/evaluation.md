# Evaluation

The Geordi database can be evaluated by all kind of analytic tools.

## Grafana

[Grafana](https://grafana.com/) is an open source platform for analytics and monitoring. It also has a built-in Postgresql support.

First, create a new Data Source and connect it to the Geordi database.

<div class="alert alert-info" role="alert">

Do not use the same database user as Geordi, as it has permissions to modify the database. Always create a separate database user for Grafana, and only grant SELECT permissions!
</div>

After that, you can create new dashboards. To render the sensor values, first find out the database ID of the sensor to be rendered. You can then use a SQL query like this in the metrics:

```sql
SELECT
  $__time(time), value, 'Temperature' as metric
FROM
  sample
WHERE
  $__timeFilter(time) AND sensor_id=23
```

Change the sensor ID and the metric string as needed.
