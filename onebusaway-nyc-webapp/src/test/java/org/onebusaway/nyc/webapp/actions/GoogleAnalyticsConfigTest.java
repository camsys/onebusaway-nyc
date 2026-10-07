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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onebusaway.nyc.util.configuration.ConfigurationService;

/**
 * GA4 settings on OneBusAwayNYCActionSupport, exercised through ErrorAction
 * (the simplest concrete subclass).
 */
@RunWith(MockitoJUnitRunner.class)
public class GoogleAnalyticsConfigTest {

  private static final String ID_KEY = "display.googleAnalyticsMeasurementId";

  private static final String EVENTS_KEY = "display.googleAnalyticsEventsEnabled";

  @Mock
  private ConfigurationService configurationService;

  @InjectMocks
  private ErrorAction action = new ErrorAction();

  private void setMeasurementId(String value) {
    when(configurationService.getConfigurationValueAsString(ID_KEY, null)).thenReturn(value);
  }

  @Test
  public void testValidMeasurementIdIsReturned() {
    setMeasurementId("G-96C2YDLGBP");
    assertEquals("G-96C2YDLGBP", action.getGoogleAnalyticsMeasurementId());
  }

  @Test
  public void testMeasurementIdIsTrimmed() {
    setMeasurementId("  G-HQEF54YFW2 \n");
    assertEquals("G-HQEF54YFW2", action.getGoogleAnalyticsMeasurementId());
  }

  @Test
  public void testUnsetMeasurementIdRendersNoTag() {
    setMeasurementId(null);
    assertNull(action.getGoogleAnalyticsMeasurementId());
  }

  @Test
  public void testBlankMeasurementIdRendersNoTag() {
    setMeasurementId("   ");
    assertNull(action.getGoogleAnalyticsMeasurementId());
  }

  @Test
  public void testUniversalAnalyticsIdIsRejected() {
    // the old display.googleAnalyticsSiteId format must not leak into the gtag snippet
    setMeasurementId("UA-12345678-1");
    assertNull(action.getGoogleAnalyticsMeasurementId());
  }

  @Test
  public void testMarkupInMeasurementIdIsRejected() {
    // the id is written unescaped into a script src and an inline script
    setMeasurementId("G-1'</script><script>alert(1)</script>");
    assertNull(action.getGoogleAnalyticsMeasurementId());
  }

  @Test
  public void testMeasurementIdWithEmbeddedSpaceIsRejected() {
    setMeasurementId("G-ABC DEF");
    assertNull(action.getGoogleAnalyticsMeasurementId());
  }

  @Test
  public void testEventsFlagReadsConfig() {
    when(configurationService.getConfigurationValueAsBoolean(EVENTS_KEY, false)).thenReturn(true);
    assertTrue(action.getGoogleAnalyticsEventsEnabled());
  }

  @Test
  public void testEventsFlagDefaultsToOff() {
    // the custom events were not part of the ticket, so an unset key must mean off
    when(configurationService.getConfigurationValueAsBoolean(EVENTS_KEY, false)).thenReturn(false);
    assertFalse(action.getGoogleAnalyticsEventsEnabled());
    verify(configurationService).getConfigurationValueAsBoolean(EVENTS_KEY, false);
  }

}
