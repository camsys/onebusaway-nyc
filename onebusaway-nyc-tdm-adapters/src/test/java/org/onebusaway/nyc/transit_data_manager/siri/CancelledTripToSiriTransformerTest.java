package org.onebusaway.nyc.transit_data_manager.siri;

import org.junit.Test;

import static org.junit.Assert.*;

public class CancelledTripToSiriTransformerTest {

  @Test
  public void leftPad() {
    CancelledTripToSiriTransformer t = new CancelledTripToSiriTransformer(null, null, false);
    assertEquals("00", t.leftPad(0));
    assertEquals("01", t.leftPad(1));
    assertEquals("09", t.leftPad(9));
    assertEquals("10", t.leftPad(10));
    assertEquals("99", t.leftPad(99));
    // bad data pass through
    assertEquals("100", t.leftPad(100));
  }

  @Test
  public void meridiemTest(){
    CancelledTripToSiriTransformer t = new CancelledTripToSiriTransformer(null, null, false);
    assertEquals("12:00am", t.formatTime("00:00:00"));
    assertEquals("12:00pm", t.formatTime("12:00:00"));
    assertEquals("1:00am", t.formatTime("01:00:00"));
    assertEquals("3:25pm", t.formatTime("15:25:00"));
    assertEquals("11:59pm", t.formatTime("23:59:59"));
    assertEquals("12:59am", t.formatTime("24:59:59"));
    assertEquals("2:00pm", t.formatTime("38:00:00"));
  }
}