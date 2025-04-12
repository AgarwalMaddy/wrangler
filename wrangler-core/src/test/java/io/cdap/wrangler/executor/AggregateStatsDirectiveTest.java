package io.cdap.wrangler.executor;

import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.Token;
import io.cdap.wrangler.api.parser.TokenType;
import com.google.gson.JsonElement;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AggregateStatsDirectiveTest {
  private static class MockArguments implements Arguments {
    private final Map<String, Token> arguments;

    public MockArguments(String... args) {
      arguments = new HashMap<>();
      for (int i = 0; i < args.length; i += 2) {
        if (i + 1 < args.length) {
          arguments.put(args[i], new ColumnName(args[i + 1]));
        }
      }
    }

    @Override
    public Token value(String name) {
      return arguments.get(name);
    }

    @Override
    public boolean contains(String name) {
      return arguments.containsKey(name);
    }

    @Override
    public int size() {
      return arguments.size();
    }

    @Override
    public TokenType type(String name) {
      return null;
    }

    @Override
    public int line() {
      return 0;
    }

    @Override
    public int column() {
      return 0;
    }

    @Override
    public String source() {
      return "";
    }

    @Override
    public JsonElement toJson() {
      return null;
    }
  }

  @Test
  public void testAggregateStats() throws DirectiveExecutionException, DirectiveParseException {
    // Create sample data
    List<Row> rows = new ArrayList<>();
    
    // Row 1: 100KB, 100ms
    Row row1 = new Row();
    row1.add("data_transfer_size", new ByteSize("100KB"));
    row1.add("response_time", new TimeDuration("100ms"));
    rows.add(row1);
    
    // Row 2: 1.5MB, 2.5s
    Row row2 = new Row();
    row2.add("data_transfer_size", new ByteSize("1.5MB"));
    row2.add("response_time", new TimeDuration("2.5s"));
    rows.add(row2);
    
    // Row 3: 750KB, 750ms
    Row row3 = new Row();
    row3.add("data_transfer_size", new ByteSize("750KB"));
    row3.add("response_time", new TimeDuration("750ms"));
    rows.add(row3);

    // Create directive
    AggregateStatsDirective directive = new AggregateStatsDirective();
    
    // Create mock arguments
    MockArguments args = new MockArguments("size-column", "response_time", "total-size-column", "total_time_sec");
    
    // Initialize directive
    directive.initialize(args);
    
    // Create mock context
    ExecutorContext context = Mockito.mock(ExecutorContext.class);
    
    // Execute directive
    List<Row> results = directive.execute(rows, context);

    // Verify results
    Assert.assertEquals(1, results.size());
    
    // Calculate expected values
    double expectedTotalSizeMB = (100.0/1024.0) + 1.5 + (750.0/1024.0); // Convert KB to MB
    double expectedTotalTimeSec = 0.1 + 2.5 + 0.75; // Convert ms to seconds
    
    // Assert with tolerance for floating point comparison
    Assert.assertEquals(expectedTotalSizeMB, (Double)results.get(0).getValue("total_size_mb"), 0.001);
    Assert.assertEquals(expectedTotalTimeSec, (Double)results.get(0).getValue("total_time_sec"), 0.001);
  }

  @Test(expected = DirectiveExecutionException.class)
  public void testMissingColumns() throws DirectiveExecutionException, DirectiveParseException {
    List<Row> rows = new ArrayList<>();
    Row row = new Row();
    rows.add(row);

    AggregateStatsDirective directive = new AggregateStatsDirective();
    MockArguments args = new MockArguments("size-column", "missing_size", "time-column", "missing_time", "total-size-column", "total_size_mb", "total-time-column", "total_time_sec");
    
    directive.initialize(args);
    directive.execute(rows, Mockito.mock(ExecutorContext.class));
  }

  @Test(expected = DirectiveExecutionException.class)
  public void testInvalidValues() throws DirectiveExecutionException, DirectiveParseException {
    List<Row> rows = new ArrayList<>();
    Row row = new Row();
    row.add("data_transfer_size", "invalid");
    row.add("response_time", "invalid");
    rows.add(row);

    AggregateStatsDirective directive = new AggregateStatsDirective();
    MockArguments args = new MockArguments("size-column", "data_transfer_size", "time-column", "response_time", "total-size-column", "total_size_mb", "total-time-column", "total_time_sec");
    
    directive.initialize(args);
    directive.execute(rows, Mockito.mock(ExecutorContext.class));
  }

  @Test
  public void testEdgeCases() throws DirectiveParseException, DirectiveExecutionException {
    AggregateStatsDirective directive = new AggregateStatsDirective();
    directive.initialize(new MockArguments(
      "size_col", "time_col", "total_size_mb", "total_time_sec"
    ));

    // Test zero values
    Row row1 = new Row();
    row1.add("size_col", new ByteSize("0B"));
    row1.add("time_col", new TimeDuration("0ms"));

    // Test large values
    Row row2 = new Row();
    row2.add("size_col", new ByteSize("1TB"));
    row2.add("time_col", new TimeDuration("24h"));

    // Test decimal values with different units
    Row row3 = new Row();
    row3.add("size_col", new ByteSize("1.5GB"));
    row3.add("time_col", new TimeDuration("1.5h"));

    // Test mixed units
    Row row4 = new Row();
    row4.add("size_col", new ByteSize("500KB"));
    row4.add("time_col", new TimeDuration("90s"));

    List<Row> rows = Arrays.asList(row1, row2, row3, row4);
    List<Row> results = directive.execute(rows, null);

    Assert.assertEquals(1, results.size());
    Row result = results.get(0);

    // Calculate expected values
    // 0B + 1TB + 1.5GB + 500KB in MB
    double expectedSizeMB = (0 + 1024.0 * 1024.0 * 1024.0 + 1.5 * 1024.0 + 0.48828125) * 1024.0;
    // 0ms + 24h + 1.5h + 90s in seconds
    double expectedTimeSec = 0 + 24 * 3600 + 1.5 * 3600 + 90;

    Assert.assertEquals(expectedSizeMB, result.getValue("total_size_mb"));
    Assert.assertEquals(expectedTimeSec, result.getValue("total_time_sec"));
  }

  @Test
  public void testAllUnits() throws DirectiveParseException, DirectiveExecutionException {
    AggregateStatsDirective directive = new AggregateStatsDirective();
    directive.initialize(new MockArguments(
      "size_col", "time_col", "total_size_mb", "total_time_sec"
    ));

    // Test all byte size units
    List<Row> rows = Arrays.asList(
      createRow("1B", "1ms"),
      createRow("1KB", "1s"),
      createRow("1MB", "1m"),
      createRow("1GB", "1h"),
      createRow("1TB", "24h")
    );

    List<Row> results = directive.execute(rows, null);
    Assert.assertEquals(1, results.size());
    Row result = results.get(0);

    // Calculate expected values
    // 1B + 1KB + 1MB + 1GB + 1TB in MB
    double expectedSizeMB = (1.0/1024.0/1024.0) + (1.0/1024.0) + 1.0 + 1024.0 + (1024.0 * 1024.0);
    // 1ms + 1s + 1m + 1h + 24h in seconds
    double expectedTimeSec = 0.001 + 1 + 60 + 3600 + (24 * 3600);

    Assert.assertEquals(expectedSizeMB, result.getValue("total_size_mb"));
    Assert.assertEquals(expectedTimeSec, result.getValue("total_time_sec"));
  }

  private Row createRow(String size, String time) {
    Row row = new Row();
    row.add("size_col", new ByteSize(size));
    row.add("time_col", new TimeDuration(time));
    return row;
  }
} 