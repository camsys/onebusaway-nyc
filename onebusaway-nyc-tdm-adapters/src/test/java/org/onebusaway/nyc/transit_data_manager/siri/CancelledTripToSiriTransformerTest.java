/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

  @Test
  public void formatTime() {
    CancelledTripToSiriTransformer t = new CancelledTripToSiriTransformer(null, null, false);
    assertEquals("12:00am", t.formatTime("0:00:00"));
    assertEquals("12:01am", t.formatTime("0:01:00"));
    assertEquals("1:00am", t.formatTime("1:00:00"));
    assertEquals("1:01am", t.formatTime("1:01:00"));
    assertEquals("10:00am", t.formatTime("10:00:00"));
    assertEquals("10:01am", t.formatTime("10:01:00"));
    assertEquals("11:59am", t.formatTime("11:59:00"));
    assertEquals("12:00pm", t.formatTime("12:00:00"));
    assertEquals("12:01pm", t.formatTime("12:01:00"));
    assertEquals("1:00pm", t.formatTime("13:00:00"));
    assertEquals("1:01pm", t.formatTime("13:01:00"));
    assertEquals("11:59pm", t.formatTime("23:59:00"));
  }
}