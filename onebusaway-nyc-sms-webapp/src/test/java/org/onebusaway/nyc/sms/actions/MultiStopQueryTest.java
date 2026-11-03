package org.onebusaway.nyc.sms.actions;

import org.junit.Test;
import static org.junit.Assert.*;

public class MultiStopQueryTest {
  @Test public void parsesThreeStopsWithThreeRoutes() {
    MultiStopQuery.Request result = MultiStopQuery.parse("300037,300038,300039 QM2 QM20 Q32");
    assertNotNull(result);
    assertNull(result.getError());
    assertEquals(3, result.getStopIds().size());
    assertTrue(result.getRoutes().contains("QM20"));
  }

  @Test public void acceptsSpacesAfterCommas() {
    assertEquals(2, MultiStopQuery.parse("300037, 300038 QM2").getStopIds().size());
  }

  @Test public void permitsSingleStopWithMultipleRoutes() {
    MultiStopQuery.Request result = MultiStopQuery.parse("300037 qm2 qm20");
    assertNotNull(result);
    assertEquals(2, result.getRoutes().size());
  }

  @Test public void leavesLegacyRequestsAlone() {
    assertNull(MultiStopQuery.parse("300037"));
    assertNull(MultiStopQuery.parse("300037 QM2"));
    assertNull(MultiStopQuery.parse("R"));
    assertNull(MultiStopQuery.parse("N"));
  }

  @Test public void rejectsTooManyStops() {
    assertNotNull(MultiStopQuery.parse("1,2,3,4").getError());
  }

  @Test public void rejectsTooManyRoutes() {
    assertNotNull(MultiStopQuery.parse("123,456 M1 M2 M3 M4").getError());
  }
}
