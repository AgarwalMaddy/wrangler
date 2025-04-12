package io.cdap.wrangler.executor;

import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveContext;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.annotations.Categories;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;
import io.cdap.wrangler.api.Arguments;

import java.util.ArrayList;
import java.util.List;

/**
 * A directive that aggregates byte size and time duration statistics from rows.
 * This directive takes four column names as arguments:
 * 1. Source column containing byte sizes (e.g., "10KB", "1.5MB")
 * 2. Source column containing time durations (e.g., "100ms", "2.5s")
 * 3. Target column name for total size in MB
 * 4. Target column name for total time in seconds
 *
 * The directive accumulates the byte sizes and time durations from all rows,
 * converts them to canonical units (bytes and milliseconds), and then
 * returns a single row with the total size in MB and total time in seconds.
 */
@Categories(categories = {"aggregate"})
public class AggregateStatsDirective implements Directive {
  public static final String NAME = "aggregate-stats";
  private String sizeColumn;
  private String timeColumn;
  private String totalSizeColumn;
  private String totalTimeColumn;
  private long totalBytes = 0;
  private long totalNanos = 0;
  private int rowCount = 0;

  /**
   * Defines the arguments required by this directive.
   * The directive requires four column names:
   * 1. size-column: source column containing byte sizes
   * 2. time-column: source column containing time durations
   * 3. total-size-column: target column for total size in MB
   * 4. total-time-column: target column for total time in seconds
   *
   * @return the usage definition
   */
  @Override
  public UsageDefinition define() {
    UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
    builder.define("size-column", TokenType.COLUMN_NAME);
    builder.define("time-column", TokenType.COLUMN_NAME);
    builder.define("total-size-column", TokenType.COLUMN_NAME);
    builder.define("total-time-column", TokenType.COLUMN_NAME);
    return builder.build();
  }

  /**
   * Initializes the directive with the provided arguments.
   * Extracts and stores the column names from the arguments.
   *
   * @param args the arguments containing the column names
   * @throws DirectiveParseException if the arguments are invalid
   */
  @Override
  public void initialize(Arguments args) throws DirectiveParseException {
    if (args == null) {
      throw new DirectiveParseException("Arguments cannot be null");
    }

    if (!args.contains("size-column")) {
      throw new DirectiveParseException("Missing required argument 'size-column'");
    }
    if (!args.contains("time-column")) {
      throw new DirectiveParseException("Missing required argument 'time-column'");
    }
    if (!args.contains("total-size-column")) {
      throw new DirectiveParseException("Missing required argument 'total-size-column'");
    }
    if (!args.contains("total-time-column")) {
      throw new DirectiveParseException("Missing required argument 'total-time-column'");
    }

    this.sizeColumn = ((ColumnName) args.value("size-column")).value();
    this.timeColumn = ((ColumnName) args.value("time-column")).value();
    this.totalSizeColumn = ((ColumnName) args.value("total-size-column")).value();
    this.totalTimeColumn = ((ColumnName) args.value("total-time-column")).value();

    if (this.sizeColumn.isEmpty()) {
      throw new DirectiveParseException("Size column name cannot be empty");
    }
    if (this.timeColumn.isEmpty()) {
      throw new DirectiveParseException("Time column name cannot be empty");
    }
    if (this.totalSizeColumn.isEmpty()) {
      throw new DirectiveParseException("Total size column name cannot be empty");
    }
    if (this.totalTimeColumn.isEmpty()) {
      throw new DirectiveParseException("Total time column name cannot be empty");
    }
  }

  @Override
  public void destroy() {
    // No cleanup needed
  }

  /**
   * Executes the directive on the provided rows.
   * Accumulates byte sizes and time durations from all rows,
   * converts them to canonical units, and returns a single row
   * with the total size in MB and total time in seconds.
   *
   * @param rows the rows to process
   * @param context the execution context
   * @return a list containing a single row with the aggregated statistics
   * @throws DirectiveExecutionException if there is an error processing the rows
   */
  @Override
  public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
    if (rows == null) {
      throw new DirectiveExecutionException("Rows cannot be null");
    }

    for (Row row : rows) {
      if (row == null) {
        throw new DirectiveExecutionException("Row cannot be null");
      }

      Object sizeValue = row.getValue(sizeColumn);
      Object timeValue = row.getValue(timeColumn);

      if (sizeValue == null) {
        throw new DirectiveExecutionException(
          String.format("Size value is null in column '%s'", sizeColumn));
      }
      if (timeValue == null) {
        throw new DirectiveExecutionException(
          String.format("Time value is null in column '%s'", timeColumn));
      }

      if (!(sizeValue instanceof ByteSize)) {
        throw new DirectiveExecutionException(
          String.format("Expected ByteSize in column '%s', but found %s", 
            sizeColumn, sizeValue.getClass().getSimpleName()));
      }
      if (!(timeValue instanceof TimeDuration)) {
        throw new DirectiveExecutionException(
          String.format("Expected TimeDuration in column '%s', but found %s", 
            timeColumn, timeValue.getClass().getSimpleName()));
      }

      totalBytes += ((ByteSize) sizeValue).getBytes();
      totalNanos += ((TimeDuration) timeValue).getMilliseconds() * 1_000_000;
      rowCount++;
    }

    // Create the result row with accumulated statistics
    List<Row> result = new ArrayList<>();
    Row aggregateRow = new Row();
    
    // Convert total bytes to MB (1 MB = 1024 * 1024 bytes)
    double totalSizeMB = totalBytes / (1024.0 * 1024.0);
    // Convert total nanoseconds to seconds
    double totalTimeSec = totalNanos / 1_000_000_000.0;
    
    aggregateRow.add(totalSizeColumn, totalSizeMB);
    aggregateRow.add(totalTimeColumn, totalTimeSec);
    
    result.add(aggregateRow);
    return result;
  }
} 