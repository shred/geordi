# Housekeeping

Geordi does not clean up old sensor data, so the database can grow and consume quite some space over the years.

The only table that needs cleanup is the `sample` table. This example query deletes all sample data that is older than a year:

```sql
DELETE FROM sample WHERE time < now() - interval '1 year';
```

After deleting a large number of records, the table should be vacuumed to return the emptied space to the operating system:

```sql
VACUUM FULL sample;
```

<div class="alert alert-info" role="alert">

`VACUUM FULL` locks the table while vacuuming. It may take a considerable amount of time on large tables, and Geordi will not be able to insert new samples until it is finished. During the operation, the table is copied to a second file, so there should be sufficient space on the volume.
</div>

To keep the database tidy, the `DELETE` query can be executed in a cronjob. Future releases of Geordi might have a feature that cleans up the database automatically.
