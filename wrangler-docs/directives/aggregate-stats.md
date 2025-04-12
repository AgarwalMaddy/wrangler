# Aggregate Stats Directive

The `aggregate-stats` directive aggregates byte sizes and time durations from multiple rows, calculating total size in MB and total time in seconds.

## Syntax

```
aggregate-stats :size_column :time_column :total_size_column :total_time_column
```

## Description

The `aggregate-stats` directive processes a set of rows containing byte size and time duration values, and produces a single row with aggregated statistics. It:

1. Reads byte size values from the specified size column (e.g., "10KB", "1.5MB")
2. Reads time duration values from the specified time column (e.g., "500ms", "2s")
3. Converts all values to canonical units (bytes and milliseconds)
4. Aggregates the values across all rows
5. Converts the totals to MB and seconds
6. Outputs the results in the specified target columns

## Arguments

* **size_column**: The source column containing byte size values
* **time_column**: The source column containing time duration values
* **total_size_column**: The target column for the total size in MB
* **total_time_column**: The target column for the total time in seconds

## Examples

Using this record as an example:

```
{
  "data_transfer_size": "10KB",
  "response_time": "500ms"
}
```

Apply this directive:

```
aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec
```

The result will be:

```
{
  "total_size_mb": 0.009765625,
  "total_time_sec": 0.5
}
```

## Notes

* The directive expects byte size values to be in one of these formats: B, KB, MB, GB, TB
* The directive expects time duration values to be in one of these formats: ms, s, m, h
* The output size is always in MB (1 MB = 1024 * 1024 bytes)
* The output time is always in seconds
* The directive processes all rows and returns a single row with the aggregated values
* If any row contains invalid values, the directive will throw an error

## Error Handling

The directive will throw an error if:
* Any of the required columns are missing
* Any of the column names are empty
* Any of the values cannot be parsed as byte sizes or time durations
* Any of the values are null 