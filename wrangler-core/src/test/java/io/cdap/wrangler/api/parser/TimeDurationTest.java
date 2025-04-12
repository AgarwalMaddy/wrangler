package io.cdap.wrangler.api.parser;

import org.junit.Assert;
import org.junit.Test;

public class TimeDurationTest {
  @Test
  public void testParseMilliseconds() {
    TimeDuration duration = new TimeDuration("100ms");
    Assert.assertEquals(100L, duration.getMilliseconds());
  }

  @Test
  public void testParseSeconds() {
    TimeDuration duration = new TimeDuration("1s");
    Assert.assertEquals(1000L, duration.getMilliseconds());
  }

  @Test
  public void testParseMinutes() {
    TimeDuration duration = new TimeDuration("1m");
    Assert.assertEquals(60 * 1000L, duration.getMilliseconds());
  }

  @Test
  public void testParseHours() {
    TimeDuration duration = new TimeDuration("1h");
    Assert.assertEquals(60 * 60 * 1000L, duration.getMilliseconds());
  }

  @Test
  public void testParseDecimal() {
    TimeDuration duration = new TimeDuration("1.5s");
    Assert.assertEquals(1500L, duration.getMilliseconds());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidUnit() {
    new TimeDuration("1xs");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidFormat() {
    new TimeDuration("invalid");
  }
} 