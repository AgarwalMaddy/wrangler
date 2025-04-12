package io.cdap.wrangler.api.parser;

import org.junit.Assert;
import org.junit.Test;

public class ByteSizeTest {
  @Test
  public void testParseBytes() {
    ByteSize size = new ByteSize("1024B");
    Assert.assertEquals(1024L, size.getBytes());
  }

  @Test
  public void testParseKB() {
    ByteSize size = new ByteSize("1KB");
    Assert.assertEquals(1024L, size.getBytes());
  }

  @Test
  public void testParseMB() {
    ByteSize size = new ByteSize("1MB");
    Assert.assertEquals(1024L * 1024L, size.getBytes());
  }

  @Test
  public void testParseGB() {
    ByteSize size = new ByteSize("1GB");
    Assert.assertEquals(1024L * 1024L * 1024L, size.getBytes());
  }

  @Test
  public void testParseTB() {
    ByteSize size = new ByteSize("1TB");
    Assert.assertEquals(1024L * 1024L * 1024L * 1024L, size.getBytes());
  }

  @Test
  public void testParseDecimal() {
    ByteSize size = new ByteSize("1.5MB");
    Assert.assertEquals((long)(1.5 * 1024 * 1024), size.getBytes());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidUnit() {
    new ByteSize("1XB");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidFormat() {
    new ByteSize("invalid");
  }
} 