package org.onebusaway.nyc.transit_data_manager.siri;

import org.junit.Test;

import static org.junit.Assert.*;

public class CancelledTripToSiriTransformerTest {

  @Test
  public void leftPad() {
    CancelledTripToSiriTransformer t = new CancelledTripToSiriTransformer(null, null);
    assertEquals("00", t.leftPad(0));
    assertEquals("01", t.leftPad(1));
    assertEquals("09", t.leftPad(9));
    assertEquals("10", t.leftPad(10));
    assertEquals("99", t.leftPad(99));
    // bad data pass through
    assertEquals("100", t.leftPad(100));
  }
}