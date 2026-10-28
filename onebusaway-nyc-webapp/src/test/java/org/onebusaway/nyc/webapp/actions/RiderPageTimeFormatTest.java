/**
 * Copyright (C) 2026 Metropolitan Transportation Authority
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
package org.onebusaway.nyc.webapp.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.webapp.actions.m.SearchResultFactoryImpl;

/**
 * Pins the exact characters of every clock time the rider web pages print.
 *
 * Why: from Java 20 the default locale data (CLDR 42) puts a narrow no-break
 * space (U+202F) before "AM"/"PM" instead of a normal space, so "5:13 PM"
 * looks the same but is a different string (delta D-16). These tests record
 * the Java 11 output and fail if the character changes.
 */
public class RiderPageTimeFormatTest {

  /** 2026-10-27 17:13:00 in New York, the zone the servers run in. */
  private Date _fixedTime;

  private TimeZone _savedTimeZone;

  private Locale _savedLocale;

  @Before
  public void setup() {
    _savedTimeZone = TimeZone.getDefault();
    _savedLocale = Locale.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
    Locale.setDefault(Locale.US);

    Calendar c = Calendar.getInstance();
    c.clear();
    c.set(2026, Calendar.OCTOBER, 27, 17, 13, 0);
    _fixedTime = c.getTime();
  }

  @After
  public void restoreDefaults() {
    TimeZone.setDefault(_savedTimeZone);
    Locale.setDefault(_savedLocale);
  }

  @Test
  public void testWikiPageLastModified() throws Exception {
    org.onebusaway.nyc.webapp.actions.wiki.IndexAction action =
        new org.onebusaway.nyc.webapp.actions.wiki.IndexAction();
    setField(action, "lastModifiedTimestamp", _fixedTime);

    assertExactText("Oct 27, 2026 at 5:13:00 PM", action.getLastModifiedTimestamp());
  }

  @Test
  public void testMobileWikiPageLastModified() throws Exception {
    org.onebusaway.nyc.webapp.actions.m.wiki.IndexAction action =
        new org.onebusaway.nyc.webapp.actions.m.wiki.IndexAction();
    setField(action, "lastModifiedTimestamp", _fixedTime);

    assertExactText("Oct 27, 2026 at 5:13:00 PM", action.getLastModifiedTimestamp());
  }

  /**
   * The mobile page's "last updated" time always uses the current time, so
   * only its shape can be checked: h:mm:ss, then a normal space, then AM/PM.
   */
  @Test
  public void testMobilePageLastUpdateTime() {
    String time = new org.onebusaway.nyc.webapp.actions.m.IndexAction().getLastUpdateTime();

    assertTrue("unexpected characters in " + describe(time),
        time.matches("\\d{1,2}:\\d{2}:\\d{2} [AP]M"));
  }

  /**
   * The scheduled departure shown for a bus waiting at the terminal
   * ("sched. dep. 5:13 PM"). The formatter is private, so it is called by
   * reflection rather than building a full stop result around it.
   */
  @Test
  public void testMobileScheduledDepartureTime() throws Exception {
    SearchResultFactoryImpl factory = new SearchResultFactoryImpl(
        mock(NycTransitDataService.class), mock(RealtimeService.class),
        mock(ConfigurationService.class));
    Method format = SearchResultFactoryImpl.class.getDeclaredMethod(
        "getFormattedOriginAimedDepartureTime", Date.class);
    format.setAccessible(true);

    assertExactText("5:13 PM", (String) format.invoke(factory, _fixedTime));
  }

  /**
   * assertEquals, with a message that shows each character's code point,
   * because the difference this guards against is invisible when printed.
   */
  private static void assertExactText(String expected, String actual) {
    assertEquals("expected " + describe(expected) + " but was " + describe(actual),
        expected, actual);
  }

  private static String describe(String text) {
    StringBuilder sb = new StringBuilder("\"" + text + "\" [");
    for (int i = 0; i < text.length(); i++) {
      if (i > 0)
        sb.append(' ');
      sb.append(String.format("U+%04X", (int) text.charAt(i)));
    }
    return sb.append(']').toString();
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }
}
